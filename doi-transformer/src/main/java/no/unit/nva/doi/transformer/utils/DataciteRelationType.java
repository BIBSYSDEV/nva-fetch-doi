package no.unit.nva.doi.transformer.utils;

import static java.util.Objects.isNull;

import java.util.Arrays;
import nva.commons.core.SingletonCollector;

public enum DataciteRelationType {
    IS_CITED_BY("IsCitedBy"),
    CITES("Cites"),
    IS_SUPPLEMENT_TO("IsSupplementTo"),
    IS_SUPPLEMENTED_BY("IsSupplementedBy"),
    IS_CONTINUED_BY("IsContinuedBy"),
    CONTINUES("Continues"),
    IS_DESCRIBED_BY("IsDescribedBy"),
    DESCRIBES("Describes"),
    HAS_METADATA("HasMetadata"),
    IS_METADATA_FOR("IsMetadataFor"),
    HAS_VERSION("HasVersion"),
    IS_VERSION_OF("IsVersionOf"),
    IS_NEW_VERSION_OF("IsNewVersionOf"),
    IS_PREVIOUS_VERSION_OF("IsPreviousVersionOf"),
    IS_PART_OF("IsPartOf"),
    HAS_PART("HasPart"),
    IS_REFERENCED_BY("IsReferencedBy"),
    REFERENCES("References"),
    IS_DOCUMENTED_BY("IsDocumentedBy"),
    DOCUMENTS("Documents"),
    IS_COMPILED_BY("IsCompiledBy"),
    COMPILES("Compiles"),
    IS_VARIANT_FORM_OF("IsVariantFormOf"),
    IS_ORIGINAL_FORM_OF("IsOriginalFormOf"),
    IS_IDENTICAL_TO("IsIdenticalTo"),
    IS_REVIEWED_BY("IsReviewedBy"),
    REVIEWS("Reviews"),
    IS_DERIVED_FROM("IsDerivedFrom"),
    IS_SOURCE_OF("IsSourceOf"),
    IS_REQUIRED_BY("IsRequiredBy"),
    REQUIRES("Requires"),
    IS_OBSOLETED_BY("IsObsoletedBy"),
    OBSOLETES("Obsoletes"),
    NON_EXISTING_RELATION(null);

    private final String relation;

    DataciteRelationType(String relation) {
        this.relation = relation;
    }

    /**
     * Returns DataciteRelationType based on input string representation of the type, or null if no type matches.
     * @param relation String representation of the type.
     * @return DataciteRelationType or null.
     */
    public static DataciteRelationType getByRelation(String relation) {
        if (isNull(relation)) {
            return NON_EXISTING_RELATION;
        }

        return Arrays.stream(DataciteRelationType.values())
                .filter(value -> !value.equals(NON_EXISTING_RELATION))
                .filter(value -> value.getRelation().equalsIgnoreCase(relation))
                .collect(SingletonCollector.collectOrElse(NON_EXISTING_RELATION));
    }

    public String getRelation() {
        return relation;
    }
}
