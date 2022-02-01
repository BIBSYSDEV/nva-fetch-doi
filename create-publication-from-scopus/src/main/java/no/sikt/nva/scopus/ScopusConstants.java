package no.sikt.nva.scopus;

import no.unit.nva.model.contexttypes.PublicationContext;
import nva.commons.core.JacocoGenerated;

public class ScopusConstants {
    //identifier fieldsnames:
    public static final String ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME = "scopusIdentifier";
    public static final String SCOPUS_ITEM_IDENTIFIER_SCP_FIELD_NAME = "scp";

    //URI constants:
    public static final String DOI_OPEN_URL_FORMAT = "https://doi.org";

    //Hournal constants:
    public static final String ISSN_TYPE_ELECTRONIC = "electronic";
    public static final String ISSN_TYPE_PRINT = "print";
    public static final PublicationContext EMPTY_PUBLICATION_CONTEXT = null;
    public static final String ERROR_MSG_ISSN_NOT_FOUND = "Could not find issn";

    @JacocoGenerated
    public ScopusConstants() {
    }
}
