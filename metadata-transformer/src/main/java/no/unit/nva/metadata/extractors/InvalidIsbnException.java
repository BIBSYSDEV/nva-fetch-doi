package no.unit.nva.metadata.extractors;

public class InvalidIsbnException extends Exception {
    public static final String ERROR_TEMPLATE = "The provided ISBN %s is invalid";

    public InvalidIsbnException(String isbn) {
        super(String.format(ERROR_TEMPLATE, isbn));
    }

}
