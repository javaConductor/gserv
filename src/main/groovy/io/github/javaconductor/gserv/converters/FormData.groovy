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
    FormElement getValue(key) {
        values.find { v ->
            v.name == key
        }
    }
    /**
     *
     * @param key
     * @return all values for that key
     */
    List<FormElement> getValues(key) {
        values.findAll { v ->
            v.name == key
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
