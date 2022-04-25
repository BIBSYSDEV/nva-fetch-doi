package no.unit.nva.doi.fetch;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public class RequestBody {

    public static final String DOI_FIELD = "doiUrl";

    @JsonProperty(DOI_FIELD)
    private URI doiUrl;

    public RequestBody() {
    }

    public URI getDoiUrl() {
        return doiUrl;
    }

    public void setDoiUrl(URI doiUrl) {
        this.doiUrl = doiUrl;
    }
}
