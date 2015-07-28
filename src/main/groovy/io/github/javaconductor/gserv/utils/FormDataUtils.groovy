package io.github.javaconductor.gserv.utils

import io.github.javaconductor.gserv.converters.ElementType
import io.github.javaconductor.gserv.converters.FileElement
import io.github.javaconductor.gserv.converters.FormData
import io.github.javaconductor.gserv.converters.FormElement
import io.github.javaconductor.gserv.converters.ValueElement
import io.github.javaconductor.gserv.exceptions.ConversionException

/**
 * Created by lcollins on 7/27/2015.
 */
class FormDataUtils {


    FormData getFormData(byte[] bytes, String contentType) {

        String boundaryMarker
        try {
            boundaryMarker = contentType.split(';')[1].split('=')[1]
        } catch (Exception e) {
            throw new ConversionException("Boundary not found : [$contentType].")
        }

        List<byte[]> parts = new ByteUtils().splitBytes(bytes, boundaryMarker)

        List<FormElement> elements = parts.collect { partBytes ->
            partBytes.size() < boundaryMarker.bytes.size() ? [] : getFormElements(partBytes)
        }.flatten()

        def groups = elements.groupBy({ element -> element.type })

        new FormData(values: groups[ElementType.Value],
                files: groups[ElementType.File])
    }

    List<FormElement> getFormElements(byte[] bytes) {

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        /// Read the first line of the part
        InputStreamReader reader = new InputStreamReader(bais)

        int byteSize = bytes.length
        String line
        String contentDisp
        String contentType
        boolean isFile, isBinary, isMultiPartMixed
        String valueName
        String filename
        def boundary = reader.readLine()
        line = reader.readLine()
        while (line?.length() > 0) {

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
                contentType = line.substring(13)
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
            line = reader.readLine()
        }

        /// next is the content
        /// get the rest as byte array

        List<Byte> data = []
        while (reader.ready()) {
            data.add((Byte) reader.read())
        }

        if (isMultiPartMixed) {
            // get the boundary
            def pairs = contentType.split(';')
            String innerBoundary = pairs[1].split('=')[1]

            def parts = new ByteUtils().splitBytes(data as byte[], innerBoundary)

            return parts.collect { part ->
                this.getFormElements(part)
            }.flatten()
        }



        println new String(data as byte[])
        byte[] dataArray = data.toArray()
        /////////////
        ///TODO process the byte-array in case of separate encryption
        /////////////

        if (isFile) {
            [new FileElement(name: filename, contentType: contentType.trim(), content: dataArray)]
        } else {
            String s = new String(dataArray)
            if (s.endsWith('\n'))
                s = s.substring(0, s.length() - 1)
            [new ValueElement(name: valueName, value: s)]
        }
    }

}
