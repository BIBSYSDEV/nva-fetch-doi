package no.unit.nva.doi.transformer.utils;

import java.net.URI;

public class OrcidLookupResponse {
    private final String orcid;
    private final URI uri;

    public OrcidLookupResponse(String orcid, URI uri) {
        this.orcid = orcid;
        this.uri = uri;
    }

    public String getOrcid() {
        return orcid;
    }

    public URI getUri() {
        return uri;
    }
}
