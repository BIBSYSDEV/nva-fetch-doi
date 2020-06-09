package no.unit.nva.doi.transformer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DataciteRelatedIdentifierTypeTest {

    @DisplayName("DataciteRelatedIdentifiers exist")
    @ParameterizedTest
    @ValueSource(strings = {"ARK", "ARXIV", "BIBCODE", "DOI", "EAN13", "EISSN", "HANDLE", "IGSN", "ISBN", "ISSN",
            "ISTC", "LISSN", "LSID", "PMID", "PURL", "UPC", "URL", "URN", "W3ID", "UNKNOWN_IDENTIFIER"})
    void dataciteRelatedIdentifiersExist(String value) {
        assertNotNull(DataciteRelatedIdentifierType.getByCode(value));
    }

    @DisplayName("DataciteRelatedIdentifiers can be retrieved by code")
    @ParameterizedTest
    @ValueSource(strings = {"ARK", "ARXIV", "BIBCODE", "DOI", "EAN13", "EISSN", "HANDLE", "IGSN", "ISBN", "ISSN",
            "ISTC", "LISSN", "LSID", "PMID", "PURL", "UPC", "URL", "URN", "W3ID"})
    void dataciteRelatedIdentifiersGetByCodeReturnCodeWhenInputCodeIsValid(String value) {
        assertEquals(value, DataciteRelatedIdentifierType.getByCode(value).getCode().toUpperCase());
    }

    @DisplayName("DataciteRelatedIdentifiers have a description")
    @ParameterizedTest
    @ValueSource(strings = {"ARK", "ARXIV", "BIBCODE", "DOI", "EAN13", "EISSN", "HANDLE", "IGSN", "ISBN", "ISSN",
            "ISTC", "LISSN", "LSID", "PMID", "PURL", "UPC", "URL", "URN", "W3ID"})
    void dataciteRelatedIdentifiersGetDescriptionReturnsDescriptionWhenInputCodeIsValid(String value) {
        assertNotNull(DataciteRelatedIdentifierType.getByCode(value).getDescription());
    }

    @DisplayName("DataciteRelatedIdentifiers.UNKNOWN_CODE is returned when code is not known")
    @ParameterizedTest
    @ValueSource(strings = {"FARK", "barXive", "BURL"})
    void dataciteRelatedIdentifiersGetByCodeReturnUnknownCodeWhenInputCodeIsInvalid(String value) {
        assertEquals(DataciteRelatedIdentifierType.UNKNOWN_IDENTIFIER, DataciteRelatedIdentifierType.getByCode(value));
    }

    @DisplayName("DataciteRelatedIdentifiers.UNKNOWN_CODE is returned when code is null")
    @Test
    void dataciteRelatedIdentifiersGetByCodeReturnUnknownCodeWhenInputCodeIsINull() {
        assertEquals(DataciteRelatedIdentifierType.UNKNOWN_IDENTIFIER, DataciteRelatedIdentifierType.getByCode(null));
    }
}