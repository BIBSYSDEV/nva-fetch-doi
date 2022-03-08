package no.sikt.nva.scopus;

import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;

public class ScopusConstants {
    //identifier fieldsnames:
    public static final String ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME = "scopusIdentifier";
    public static final String SCOPUS_ITEM_IDENTIFIER_SCP_FIELD_NAME = "scp";

    //URI constants:
    public static final String DOI_OPEN_URL_FORMAT = "https://doi.org";
    public static final String ORCID_DOMAIN_URL = "https://orcid.org/";

    //Journal constants:
    public static final String ISSN_TYPE_ELECTRONIC = "electronic";
    public static final String ISSN_TYPE_PRINT = "print";
    public static final URI DUMMY_URI = UriWrapper.fromUri("https://loremipsum.io/").getUri();

    //affiliation constants:
    public static final String AFFILIATION_DELIMITER = ", ";

    //xml field names:
    public static final String SUP_START = "<sup>";
    public static final String SUP_END = "</sup>";
    public static final String INF_START = "<inf>";
    public static final String INF_END = "</inf>";

    @JacocoGenerated
    public ScopusConstants() {
    }
}
