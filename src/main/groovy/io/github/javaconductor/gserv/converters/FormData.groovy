package io.github.javaconductor.gserv.converters

/**
 * Created by lcollins on 7/24/2015.
 */
class FormData {
    List<ValueElement> values
    List<FileElement> files

}

enum ElementType {
    Value,
    File
}

class FormElement {
    ElementType type = ElementType.File
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
