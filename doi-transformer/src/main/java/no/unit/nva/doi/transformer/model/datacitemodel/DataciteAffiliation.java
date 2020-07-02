package no.unit.nva.doi.transformer.model.datacitemodel;

import java.net.URI;
import nva.commons.utils.JacocoGenerated;

public class DataciteAffiliation {

    private String affiliation;
    private String affiliationIdentifier;
    private String affiliationIdentifierScheme;
    private URI schemeURI;

    @JacocoGenerated
    public DataciteAffiliation() {

    }

    @JacocoGenerated
    public String getAffiliationIdentifier() {
        return affiliationIdentifier;
    }

    @JacocoGenerated
    public void setAffiliationIdentifier(String affiliationIdentifier) {
        this.affiliationIdentifier = affiliationIdentifier;
    }

    @JacocoGenerated
    public String getAffiliationIdentifierScheme() {
        return affiliationIdentifierScheme;
    }

    @JacocoGenerated
    public void setAffiliationIdentifierScheme(String affiliationIdentifierScheme) {
        this.affiliationIdentifierScheme = affiliationIdentifierScheme;
    }

    @JacocoGenerated
    public URI getSchemeURI() {
        return schemeURI;
    }

    @JacocoGenerated
    public void setSchemeURI(URI schemeURI) {
        this.schemeURI = schemeURI;
    }

    @JacocoGenerated
    public String getAffiliation() {
        return affiliation;
    }

    @JacocoGenerated
    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }
}
