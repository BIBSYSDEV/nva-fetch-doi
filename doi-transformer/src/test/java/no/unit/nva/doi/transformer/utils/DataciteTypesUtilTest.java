package no.unit.nva.doi.transformer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.unit.nva.doi.transformer.model.datacitemodel.DataciteResponse;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataciteTypesUtilTest {

    public static final String DATASET = "Dataset";

    @DisplayName("The mapper util checks for resourceTypeGeneral and returns null for non-text values")
    @Test
    void testMapperReturnsNullWhenInputIsNotTypeText() {
        DataciteTypesUtil.mapToType(getNonTextDataciteResource());
    }

    @DisplayName("The mapping utility returns type JOURNAL_CONTENT when all input types map to JOURNAL_CONTENT")
    @Test
    void mapToTypeReturnsJournalContentWhenAllTypesAreJournalContent() {
        DataciteTypes dataciteTypes = new DataciteTypes.Builder()
                .withBibtex(BibTexType.ARTICLE.getType())
                .withCiteproc(CiteProcType.ARTICLE_JOURNAL.getType())
                .withResourceType("JournalArticle")
                .withRis(RisType.JOUR.name())
                .withSchemaOrg(SchemaOrgType.SCHOLARLY_ARTICLE.getType())
                .withResourceTypeGeneral("Text")
                .build();
        DataciteResponse dataciteResponse = new DataciteResponse.Builder()
                .withTypes(dataciteTypes)
                .build();
        assertEquals(PublicationType.JOURNAL_CONTENT, DataciteTypesUtil.mapToType(dataciteResponse).orElseThrow());

    }

    @DisplayName("The mapping utility returns type JOURNAL_CONTENT when all but one types map to JOURNAL_CONTENT")
    @Test
    void mapToTypeReturnsJournalContentWhenAllButOneTypesAreJournalContent() {
        String dissentingType = BibTexType.CONFERENCE.getType();

        DataciteTypes dataciteTypes = new DataciteTypes.Builder()
                .withBibtex(dissentingType)
                .withCiteproc(CiteProcType.ARTICLE_JOURNAL.getType())
                .withResourceType("JournalArticle")
                .withRis(RisType.JOUR.name())
                .withSchemaOrg(SchemaOrgType.SCHOLARLY_ARTICLE.getType())
                .withResourceTypeGeneral("Text")
                .build();
        DataciteResponse dataciteResponse = new DataciteResponse.Builder()
                .withTypes(dataciteTypes)
                .build();
        assertEquals(PublicationType.JOURNAL_CONTENT, DataciteTypesUtil.mapToType(dataciteResponse).orElseThrow());
    }

    @DisplayName("The mapping utility returns type JOURNAL_CONTENT when all but two types map to JOURNAL_CONTENT")
    @Test
    void mapToTypeReturnsJournalContentWhenAllButTwoTypesAreJournalContent() {
        String firstDissentingType = BibTexType.BOOK.getType();
        String secondDissentingType = CiteProcType.CHAPTER.getType();

        DataciteTypes dataciteTypes = new DataciteTypes.Builder()
                .withBibtex(firstDissentingType)
                .withCiteproc(secondDissentingType)
                .withResourceType("JournalArticle")
                .withRis(RisType.JOUR.name())
                .withSchemaOrg(SchemaOrgType.SCHOLARLY_ARTICLE.getType())
                .withResourceTypeGeneral("Text")
                .build();
        DataciteResponse dataciteResponse = new DataciteResponse.Builder()
                .withTypes(dataciteTypes)
                .build();
        assertEquals(PublicationType.JOURNAL_CONTENT, DataciteTypesUtil.mapToType(dataciteResponse).orElseThrow());
    }

    @DisplayName("The mapping utility returns type JOURNAL_CONTENT when two types map to JOURNAL_CONTENT")
    @Test
    void mapToTypeReturnsJournalContentWhenTwoTypesAreJournalContent() {
        var dataciteTypes = new DataciteTypes.Builder()
                .withBibtex(BibTexType.CONFERENCE.getType())
                .withCiteproc(CiteProcType.PAPER_CONFERENCE.getType())
                .withResourceType("JournalArticle")
                .withSchemaOrg(SchemaOrgType.SCHOLARLY_ARTICLE.getType())
                .withResourceTypeGeneral("Text")
                .build();
        var dataciteResponse = new DataciteResponse.Builder()
                .withTypes(dataciteTypes)
                .build();
        assertEquals(PublicationType.JOURNAL_CONTENT, DataciteTypesUtil.mapToType(dataciteResponse).orElseThrow());
    }


    private DataciteResponse getNonTextDataciteResource() {
        DataciteTypes dataciteType = new DataciteTypes.Builder()
                .withResourceType(DATASET)
                .build();
        return new DataciteResponse.Builder()
                .withTypes(dataciteType)
                .build();
    }

}