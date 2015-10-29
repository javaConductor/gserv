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
        !getElement(key)
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

        String getValue(key) {
            List l = getElements(key)
            (l.empty) ? null : l[0].value
        }

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

    FileElement() {
        type = ElementType.File
    }
}
