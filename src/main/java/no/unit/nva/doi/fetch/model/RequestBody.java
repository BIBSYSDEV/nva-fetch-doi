package no.unit.nva.doi.fetch.model;

import java.net.URL;

public class RequestBody {

    public static final String DOI_FIELD="doiUrl";

    private URL doiUrl;

    public RequestBody() {
    }

    public URL getDoiUrl() {
        return doiUrl;
    }

    public void setDoiUrl(URL doiUrl) {
        this.doiUrl = doiUrl;
    }
}
