package no.unit.nva.doi.transformer.utils;

import static java.util.Objects.isNull;

import java.util.Arrays;
import nva.commons.core.SingletonCollector;

/**
 * Enumeration, one of the type ids from https://api.crossref.org/v1/types
 */
public enum CrossrefType {
    JOURNAL_ARTICLE("journal-article", PublicationType.JOURNAL_CONTENT),
    BOOK("book", PublicationType.BOOK),
    NON_EXISTING_TYPE(null, null);

    private final String type;
    private final PublicationType publicationType;

    CrossrefType(String type, PublicationType publicationType) {
        this.type = type;
        this.publicationType = publicationType;
    }

    /**
     * Retrieve the PublicationType based on a Crossref type string.
     *
     * @param type the Crossref type string.
     * @return a PublicationType.
     */
    public static CrossrefType getByType(String type) {
        if (isNull(type)) {
            return NON_EXISTING_TYPE;
        }

        return collectSingleNonEmptyCrossrefType(type);
    }

    private static CrossrefType collectSingleNonEmptyCrossrefType(String type) {
        return Arrays.stream(values())
                .filter(crossrefType -> !crossrefType.equals(NON_EXISTING_TYPE))
                .filter(crossrefType -> crossrefType.getType().equalsIgnoreCase(type))
                .collect(SingletonCollector.collectOrElse(NON_EXISTING_TYPE));
    }

    public String getType() {
        return type;
    }

    public PublicationType getPublicationType() {
        return publicationType;
    }
}
