package no.unit.nva.doi.transformer.utils;

import static java.util.function.Predicate.not;
import java.util.Arrays;
import java.util.Optional;
import nva.commons.core.SingletonCollector;

/**
 * Enumeration, one of the type ids from <a href="https://api.crossref.org/v1/types">Crossref types</a>
 */
public enum CrossrefType {
    JOURNAL_ARTICLE("journal-article", PublicationType.JOURNAL_CONTENT),
    BOOK("book", PublicationType.BOOK),
    BOOK_CHAPTER("book-chapter", PublicationType.BOOK_CHAPTER),
    EDITED_BOOK("edited-book", PublicationType.EDITED_BOOK),
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
    public static Optional<CrossrefType> getByType(String type) {
        return collectSingleNonEmptyCrossrefType(type);
    }

    private static Optional<CrossrefType> collectSingleNonEmptyCrossrefType(String type) {
        return Arrays.stream(values())
                   .filter(not(NON_EXISTING_TYPE::equals))
                   .filter(crossrefType -> crossrefType.getType().equalsIgnoreCase(type))
                   .map(Optional::of)
                   .collect(SingletonCollector.collectOrElse(Optional.empty()));
    }

    public String getType() {
        return type;
    }

    public PublicationType getPublicationType() {
        return publicationType;
    }
}
