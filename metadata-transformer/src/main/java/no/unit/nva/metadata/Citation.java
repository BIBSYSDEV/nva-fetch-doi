package no.unit.nva.metadata;

import java.util.Arrays;
import java.util.Optional;

public enum Citation {
    AUTHOR("author", DcTerms.CONTRIBUTOR),
    COVER_DATE("cover_date", DcTerms.DATE),
    DATE("date", DcTerms.DATE),
    DOI("doi", Bibo.DOI),
    LANGUAGE("language", DcTerms.LANGUAGE),
    PUBLICATION_DATE("publication_date", DcTerms.DATE),
    TITLE("title", DcTerms.TITLE);

    private static final String CITATION = "citation_";
    private final String property;
    private final OntologyProperty mappedTerm;

    Citation(String property, OntologyProperty mappedTerm) {
        this.property = property;
        this.mappedTerm = mappedTerm;
    }

    public String getProperty() {
        return CITATION + property;
    }

    public OntologyProperty getMappedTerm() {
        return mappedTerm;
    }

    public static Optional<Citation> getByProperty(String candidate) {
        return Optional.of(Arrays.stream(values())
                .filter(property -> property.getProperty().equalsIgnoreCase(candidate))
                .findAny()).orElse(Optional.empty());
    }
}
