package no.unit.nva.doi.fetch.model;

import java.net.URL;

public class RequestBody {

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
