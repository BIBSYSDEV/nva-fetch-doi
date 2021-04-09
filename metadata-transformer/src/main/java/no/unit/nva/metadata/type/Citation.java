package no.unit.nva.metadata.type;

import java.util.Arrays;
import java.util.Optional;

public enum Citation implements MetaTagSet {
    AUTHOR("author", DcTerms.CONTRIBUTOR),
    COVER_DATE("cover_date", DcTerms.DATE),
    DATE("date", DcTerms.DATE),
    DOI("doi", Bibo.DOI),
    LANGUAGE("language", DcTerms.LANGUAGE),
    PUBLICATION_DATE("publication_date", DcTerms.DATE),
    TITLE("title", DcTerms.TITLE);

    private static final String CITATION = "citation_";
    private final String tagName;
    private final OntologyProperty mapping;

    Citation(String tagName, OntologyProperty mapping) {
        this.tagName = tagName;
        this.mapping = mapping;
    }

    @Override
    public String getMetaTagName() {
        return CITATION + tagName;
    }

    @Override
    public OntologyProperty getMapping() {
        return mapping;
    }

    public static Optional<Citation> getTagByString(String candidate) {
        return Arrays.stream(values())
                .filter(property -> property.getMetaTagName().equalsIgnoreCase(candidate))
                .findAny();
    }
}
