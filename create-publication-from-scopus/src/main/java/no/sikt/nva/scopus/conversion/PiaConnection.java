package no.sikt.nva.scopus.conversion;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import no.sikt.nva.scopus.conversion.model.Author;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public class PiaConnection {
    private final HttpClient httpClient;
    private final URI piaEndpoint;
    private static final String USERNAME_PASSWORD_DELIMITER = ":";

    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC_AUTHORIZATION = "Basic %s";
    private static final String PIA_REST_API = new Environment().readEnv("PIA_REST_API");
    private static final String PIA_USERNAME = new Environment().readEnv("PIA_USERNAME");
    private static final String PIA_PASSWORD = new Environment().readEnv("PIA_PASSWORD");
    private final transient String piaAuthorization;

    public PiaConnection (HttpClient httpClient, URI piaEndpoint) {
        this.httpClient = httpClient;
        this.piaEndpoint = piaEndpoint;
        this.piaAuthorization = createAuthorization();
    }

    public PiaConnection() {
        this(HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build(),
             UriWrapper.fromUri(PIA_REST_API).getUri());
    }

    private String createAuthorization() {
        String loginPassword = PIA_USERNAME + USERNAME_PASSWORD_DELIMITER + PIA_PASSWORD;
        return String.format(BASIC_AUTHORIZATION, Base64.getEncoder().encodeToString(loginPassword.getBytes()));
    }

    private List<Author> getPiaAuthorResponse(String scopusID){
        ArrayList<Author> authors = new ArrayList<>();
        Author author = new Author();
        author.setCristinId(1);
        authors.add(author);

        return authors;
    }

    private Author getCristinNumber(List<Author> authors) {
        return authors.get(1);
    }

    public URI getCristinID(String scopusId) {
        List<Author> piaAuthorResponse = getPiaAuthorResponse(scopusId);
        Author author = getCristinNumber(piaAuthorResponse);
        int cristinId = author.getCristinId();

        return UriWrapper.fromUri("https://api.nva.unit.no/cristin/person/" + cristinId).getUri();
    }
}
