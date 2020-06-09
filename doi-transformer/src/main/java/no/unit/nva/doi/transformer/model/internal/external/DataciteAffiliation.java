package no.unit.nva.doi.transformer.model.internal.external;

import java.net.URI;

public class DataciteAffiliation {

    private String affiliation;
    private String affiliationIdentifier;
    private String affiliationIdentifierScheme;
    private URI schemeURI;

    public DataciteAffiliation() {

    }

    public String getAffiliationIdentifier() {
        return affiliationIdentifier;
    }

    public void setAffiliationIdentifier(String affiliationIdentifier) {
        this.affiliationIdentifier = affiliationIdentifier;
    }

    public String getAffiliationIdentifierScheme() {
        return affiliationIdentifierScheme;
    }

    public void setAffiliationIdentifierScheme(String affiliationIdentifierScheme) {
        this.affiliationIdentifierScheme = affiliationIdentifierScheme;
    }

    public URI getSchemeURI() {
        return schemeURI;
    }

    public void setSchemeURI(URI schemeURI) {
        this.schemeURI = schemeURI;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }
}
