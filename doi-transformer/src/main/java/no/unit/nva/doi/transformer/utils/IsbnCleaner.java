package no.unit.nva.doi.transformer.utils;

import no.unit.nva.model.exceptions.InvalidIsbnException;
import org.apache.commons.validator.routines.ISBNValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.doi.transformer.utils.StringUtils.removeMultipleWhiteSpaces;


public final class IsbnCleaner {

    public static final String INVALID_CHARS = "[^[0-9]Xx]+";
    public static final String EMPTY_STRING = "";

    private static final Logger logger = LoggerFactory.getLogger(IsbnCleaner.class);
    public static final String ERROR_WHEN_TRYING_TO_CLEAN_ISSN = "Error when trying to clean ISBN: ";

    private IsbnCleaner() {
    }

    /**
     * Takes an input string that is an ISBN candidate, tests it and formats it if possible or returns null.
     * @param value a string ISBN candidate.
     * @return A string of a valid ISBN, or null.
     */
    public static String clean(String value) {
        String isbnCandidate = stripSeparatorAndWhitespace(value);
        if (isNull(isbnCandidate)) {
            return null;
        }
        try {
            return checkIsbn(isbnCandidate);
        } catch (InvalidIsbnException e) {
            logger.warn(ERROR_WHEN_TRYING_TO_CLEAN_ISSN + e.getMessage());
            return null;
        }
    }

    /**
     * This method removes potential formatting errors such as whitespace, non-hyphen separators,
     * invalid characters and missing separators.
     *
     * @param value an ISSN candidate.
     * @return a formatted ISSN candidate or null.
     */
    private static String stripSeparatorAndWhitespace(String value) {
        return nonNull(value) ? removeMultipleWhiteSpaces(value).replaceAll(INVALID_CHARS, EMPTY_STRING) : null;
    }


    public static String checkIsbn(String isbn) throws InvalidIsbnException {
        if (isNull(isbn) || isbn.isEmpty()) {
            return null;
        }
        if (new ISBNValidator().isValid(isbn)) {
            return isbn;
        } else {
            throw new InvalidIsbnException(List.of(isbn));
        }
    }

}
