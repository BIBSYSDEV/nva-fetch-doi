package no.unit.nva.doi.transformer.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Optional;
import java.util.regex.Pattern;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CristinProxyClient {

    private static final String API_HOST = "API_HOST";
    private static final String CRISTIN = "cristin";
    private static final String PERSON = "person";
    private static final Pattern ORCID_PATTERN = Pattern.compile("[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9|Xx]");
    private static final String ERROR_INVALID_ORCID = "Supplied ORCID is not valid";

    private final transient HttpClient httpClient;
    private final URI apiUrl;
    private static final Logger logger = LoggerFactory.getLogger(BareProxyClient.class);

    @JacocoGenerated
    public CristinProxyClient() {
        this(HttpClient.newHttpClient(), new Environment());
    }

    public CristinProxyClient(HttpClient httpClient, Environment environment) {
        this.httpClient = httpClient;
        this.apiUrl = URI.create(environment.readEnv(API_HOST));
    }

    /**
     * Get an (optional) Cristin proxy person identifier from an orcid.
     *
     * @param orcid given orcid from metadata
     * @return a URI with person identifier from Cristin proxy for the given orcid
     */
    public Optional<URI> lookupIdentifierFromOrcid(String orcid) {

        var strippedOrcid = stripAndValidateOrcid(orcid);
        var cristinProxyUri = createUrlToCristinProxy(apiUrl, strippedOrcid);
        return null;
    }

    protected URI createUrlToCristinProxy(URI apiUrl, String strippedOrcid) {
        return UriWrapper.fromUri(apiUrl).addChild(CRISTIN).addChild(PERSON).addChild(strippedOrcid).getUri();
    }

    private String stripAndValidateOrcid(String orcid) {
        String strippedOrcid;
        try {
            strippedOrcid = UriWrapper.fromUri(orcid).getLastPathElement();
        } catch (Exception e) {
            throw new IllegalArgumentException(ERROR_INVALID_ORCID);
        }
        if (!ORCID_PATTERN.matcher(strippedOrcid).matches()) {
            throw new IllegalArgumentException(ERROR_INVALID_ORCID);
        }
        return strippedOrcid;
    }
}
