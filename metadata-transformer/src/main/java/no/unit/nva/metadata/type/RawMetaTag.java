package no.unit.nva.metadata.type;

import java.util.Arrays;
import java.util.Optional;

public enum RawMetaTag implements MetaTagSet {
    DOI("doi", Bibo.DOI);

    private final String tagName;
    private final OntologyProperty mapping;

    RawMetaTag(String tagName, OntologyProperty mapping) {
        this.tagName = tagName;
        this.mapping = mapping;
    }

    @Override
    public String getMetaTagName() {
        return tagName;
    }

    @Override
    public OntologyProperty getMapping() {
        return mapping;
    }

    public static Optional<RawMetaTag> getTagByString(String candidate) {
        return Optional.of(Arrays.stream(values())
                .filter(property -> property.getMetaTagName().equalsIgnoreCase(candidate))
                .findAny()).orElse(Optional.empty());
    }
}
