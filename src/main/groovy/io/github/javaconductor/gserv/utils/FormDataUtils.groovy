/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2014-2016 Lee Collins
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package io.github.javaconductor.gserv.utils

import com.google.common.primitives.Bytes
import io.github.javaconductor.gserv.converters.*
import io.github.javaconductor.gserv.exceptions.ConversionException

/**
 * Created by lcollins on 7/27/2015.
 */
class FormDataUtils {

	FormData getFormData(byte[] bytes, String contentType) {

		boolean isMultipart = contentType.toLowerCase().contains("multipart/form-data")
		boolean urlEncoded = contentType.toLowerCase().contains("application/x-www-form-urlencoded")

		if (urlEncoded) {
			// get the data
			/// do some url-decode
			/// for each kv pair
			//// create ValueElement

			String urlEncodedData = new String(bytes);
			String decoded = URLDecoder.decode(urlEncodedData, "UTF-8")
			String[] pairs = decoded.split('&')

			def values = pairs.collect { kvPair ->
				String[] both = kvPair.split('=')
				String name = both[0]
				String value = both[1] ?: true
				new ValueElement(value: value, name: name)
			}

			new FormData(values: values ?: [], files: [])

		} else {
			String boundaryMarker
			try {
				boundaryMarker = '--' + contentType.split(';')[1].split('=')[1]
			} catch (Exception e) {
				throw new ConversionException("Boundary not found : [$contentType].")
			}

			List<byte[]> parts = new ByteUtils().splitBytes(bytes, boundaryMarker)

			List<FormElement> elements = parts.collect { partBytes ->
				partBytes.size() < boundaryMarker.bytes.size() ? [] : getFormElements(partBytes)
			}.flatten()

			def groups = elements.groupBy({ element -> element.type })

			new FormData(values: groups[ElementType.Value] ?: [],
					files: groups[ElementType.File] ?: [])
		}
	}

	List<FormElement> getFormElements(byte[] bytes) {

		def whitespace = 4
		def idx = Bytes.indexOf(bytes, "\r\n\r\n".bytes)
		if (idx == -1) {
			whitespace = 2;
			idx = Bytes.indexOf(bytes, "\n\n".bytes)
		}

		byte[] headers = Arrays.copyOfRange(bytes, 0, idx)
		byte[] body = Arrays.copyOfRange(bytes, idx + whitespace, bytes.length)

		ByteArrayInputStream baisHeaders = new ByteArrayInputStream(headers);
		/// Read the first line of the part
		InputStreamReader reader = new InputStreamReader(baisHeaders)
		int byteSize = bytes.length
		String line
		String contentDisp
		String contentType
		boolean isFile, isBinary, isMultiPartMixed
		String valueName
		String filename
		//TODO Check to see the \n\r at end of line
		//TODO then look to see what is at the beginning of buffer
		def boundary = reader.readLine()
		//    def oddByte = reader.read()
		//TODO BAD - Maybe I can read each line from start to the double \n\r
//        oddByte = reader.read()
		while (reader.ready()) {
			line = reader.readLine()

			if (line.toUpperCase().startsWith("CONTENT-DISPOSITION:")) {
				///TODO finish this !!!!!!
				String[] headerValues = line.split(";")

				headerValues.tail().each { kv ->
					def kvArr = kv.split("=")
					if (kvArr.head().trim() == "filename") {
						isFile = true
						filename = kvArr[1].replace('"', '')
					} else if (kvArr.head().trim() == "name") {
						valueName = kvArr[1]
						valueName = valueName.replace('"', '')
					}
				}

			} else if (line.toUpperCase().startsWith("CONTENT-TYPE:")) {
				contentType = line.substring(14)
				isBinary = contentType?.toLowerCase()?.contains("application/octet-stream")
				isMultiPartMixed = contentType?.toLowerCase()?.contains("multipart/mixed")

				//if multipart/mixed then the files could all be in this one part
				// each one separated by a boundary

			} else if (line.toUpperCase().startsWith("CONTENT-TRANSFER-ENCODING:")) {
				//Content-Transfer-Encoding: binary
				if (line.contains("binary")) {
					isBinary = true
				}
			}
		}

		/// next is the content
		if (isMultiPartMixed) {
			// get the boundary
			def pairs = contentType.split(';')
			String innerBoundary = pairs[1].split('=')[1]

			def parts = new ByteUtils().splitBytes(body as byte[], '--' + innerBoundary)

			return parts.collect { part ->
				this.getFormElements(part)
			}.flatten()
		}

		println new String(body)
		byte[] dataArray = body
		/////////////
		///TODO process the byte-array in case of separate encryption
		/////////////

		if (isFile) {
			[new FileElement(name: filename, contentType: contentType.trim(), content: dataArray, size: dataArray.length)]
		} else {
			String s = new String(body)
			if (s.endsWith('\r\n')) {
				s = s.substring(0, s.length() - 2)
			} else if (s.endsWith('\n')) {
				s = s.substring(0, s.length() - 1)
			}

			s = URLDecoder.decode(s, "UTF-8")
			[new ValueElement(name: valueName, value: s)]
		}
	}
}
