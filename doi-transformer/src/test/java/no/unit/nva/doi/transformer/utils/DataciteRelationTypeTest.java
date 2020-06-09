package no.unit.nva.doi.transformer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DataciteRelationTypeTest {

    @DisplayName("DataciteRelationType can be got by relation type string")
    @ParameterizedTest
    @ValueSource(strings = {"IsCitedBy", "Cites", "IsSupplementTo", "IsSupplementedBy", "IsContinuedBy", "Continues",
            "IsDescribedBy", "Describes", "HasMetadata", "IsMetadataFor", "HasVersion", "IsVersionOf", "IsNewVersionOf",
            "IsPreviousVersionOf", "IsPartOf", "HasPart", "IsReferencedBy", "References", "IsDocumentedBy", "Documents",
            "IsCompiledBy", "Compiles", "IsVariantFormOf", "IsOriginalFormOf", "IsIdenticalTo", "IsReviewedBy",
            "Reviews", "IsDerivedFrom", "IsSourceOf", "IsRequiredBy", "Requires", "IsObsoletedBy", "Obsoletes"})
    void getByRelationReturnsTypeWhenInputStringMatchesType(String input) {
        assertEquals(input, DataciteRelationType.getByRelation(input).getRelation());
    }

    @DisplayName("getByRelation returns NON_EXISTING_RELATION when input relation does not exist")
    @ParameterizedTest
    @ValueSource(strings = {"", "sister", "mother", "daughter", "father", "grandfather clock"})
    void getByRelationReturnsNonExistingRelationWhenInputDoesNotMatchExistingRelation(String input) {
        assertEquals(DataciteRelationType.NON_EXISTING_RELATION, DataciteRelationType.getByRelation(input));
    }

    @DisplayName("getByRelation returns NON_EXISTING_RELATION when input relation is null")
    @Test
    void getByRelationReturnsNonExistingRelationWhenInputIsNull() {
        assertEquals(DataciteRelationType.NON_EXISTING_RELATION, DataciteRelationType.getByRelation(null));
    }
}
