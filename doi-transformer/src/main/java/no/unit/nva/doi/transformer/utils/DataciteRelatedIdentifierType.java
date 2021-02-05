package no.unit.nva.doi.transformer.utils;

import static java.util.Objects.isNull;

import java.util.Arrays;
import nva.commons.core.SingletonCollector;

public enum DataciteRelatedIdentifierType {
    ARK("ARK", "Archival Resource Key"),
    ARXIV("arXiv", "arXiv identifier"),
    BIBCODE("bibcode", "Astrophysics Data System bibliographic code"),
    DOI("DOI", "Digital Object Identifier"),
    EAN13("EAN13", "European Article Number"),
    EISSN("EISSN", "Electronic International Standard Serial Number"),
    HANDLE("Handle", "HNDL, Handle"),
    IGSN("IGSN", "International Geo Sample Number"),
    ISBN("ISBN", "International Standard Book Number"),
    ISSN("ISSN", "International Standard Serial Number"),
    ISTC("ISTC", "International Standard Text Code"),
    LISSN("LISSN", "Linking ISSN, or ISSN-L"),
    LSID("LSID", "Life Science Identifier"),
    PMID("PMID", "PubMed identifier"),
    PURL("PURL", "Persistent URL"),
    UPC("UPC", "Universal Product Code"),
    URL("URL", "Uniform Resource Locator"),
    URN("URN", "Uniform Resource Name"),
    W3ID("w3id", "Permanent Web-Application identifier"),
    UNKNOWN_IDENTIFIER(null, null);

    private final String code;
    private final String description;

    DataciteRelatedIdentifierType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Method returns a type based on input code, returns UNKNOWN_IDENTIFIER if type is unknown or null.
     * @param code the string for the code
     * @return a DataciteRelatedIdentifierType
     */
    public static DataciteRelatedIdentifierType getByCode(String code) {
        if (isNull(code)) {
            return UNKNOWN_IDENTIFIER;
        }

        return Arrays.stream(values())
                .filter(DataciteRelatedIdentifierType::isKnown)
                .filter(type -> matchesDataciteIdentifierType(code, type))
                .collect(SingletonCollector.collectOrElse(UNKNOWN_IDENTIFIER));
    }

    private static boolean matchesDataciteIdentifierType(String code, DataciteRelatedIdentifierType type) {
        return type.getCode().equalsIgnoreCase(code);
    }                                                                                                        


    private static boolean isKnown(DataciteRelatedIdentifierType dataciteRelatedIdentifierType) {
        return !dataciteRelatedIdentifierType.equals(DataciteRelatedIdentifierType.UNKNOWN_IDENTIFIER);
    }

    /**
     * Allows access to the string for the DataciteRelatedIdentifierType.
     * @return the string of the DataciteRelatedIdentifierType.
     */
    public String getCode() {
        return code;
    }

    /**
     * Allows access to the textual description of the DataciteRelatedIdentifierType.
     * @return the string description of the DataciteRelatedIdentifierType.
     */
    public String getDescription() {
        return description;
    }
}
