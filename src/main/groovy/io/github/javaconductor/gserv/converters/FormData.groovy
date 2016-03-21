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

package io.github.javaconductor.gserv.converters

/**
 * Created by lcollins on 7/24/2015.
 */
class FormData {
	List<ValueElement> values = []
	List<FileElement> files = []

	/**
	 *
	 * @param key
	 * @return the first matching value
	 */
	FormElement getElement(key) {
		values.find { v ->
			v.name == key
		}
	}

	String getValue(String key) {
		getElement(key)?.value
	};

	boolean hasFiles() {
		!files.empty
	}

	boolean hasFields() {
		!values.empty
	}

	boolean hasField(key) {
		!!getElement(key)
	}
	def fieldValue = new _values(this);
	class _values {
		FormData formData

		def _values(FormData formData) {
			List<ValueElement> elements
			this.formData = formData
		}

		/**
		 *
		 * @param key
		 * @return all values for that key
		 */
		List<FormElement> getElements(key) {
			formData.values.findAll { v ->
				v.name == key
			}
		}

		/**
		 *
		 * @param key
		 * @return all values for that key
		 */
		List<String> getValues(key) {
			getElements(key).collect { v ->
				v.value
			}
		}

		/**
		 *
		 *
		 * @param key
		 * @return first value for the key
		 */
		String getValue(key) {
			List l = getElements(key)
			(l.empty) ? null : l[0].value
		}

		/**
		 * Added to use the [] operator

		 * @param key
		 * @return String or null
		 */
		String getAt(String key) {
			getValue(key)
		}

	}
	/**
	 *
	 * @param key
	 * @return all values for that key
	 */
	List<FormElement> getElements(key) {
		values.findAll { v ->
			v.name == key
		}
	}

	/**
	 *
	 * @param key
	 * @return all values for that key
	 */
	List<String> getValues(key) {
		getElements(key).collect { v ->
			v.value
		}
	}

}

public enum ElementType {
	Value,
	File
}

class FormElement {
	ElementType type;
	String name;
}

class ValueElement extends FormElement {
	String value;

	ValueElement() {
		type = ElementType.Value
	}
}

class FileElement extends FormElement {
	String contentType
	byte[] content
	int size

	FileElement() {
		type = ElementType.File
	}
}
