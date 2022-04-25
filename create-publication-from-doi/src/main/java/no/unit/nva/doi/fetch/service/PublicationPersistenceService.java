package no.unit.nva.doi.fetch.service;

public class PublicationPersistenceService extends RestClient {

//    public static final String PATH = "publication";
//    public static final String WARNING_MESSAGE = "Inserting publication failed.";
//    public static final String INSERTING_PUBLICATION_FAILED = WARNING_MESSAGE + "\nAPI-URL:%s\nRequestBody:%s\n";
//    private final HttpClient client;
//
//    public PublicationPersistenceService(HttpClient client) {
//        super();
//        this.client = client;
//    }
//
//    @JacocoGenerated
//    public PublicationPersistenceService() {
//        this(HttpClient.newBuilder().build());
//    }
//
//    /**
//     * Create Publication in Database.
//     *
//     * @param request       CreatePublicationRequest
//     * @param apiUrl        apiUrl
//     * @param authorization authorization
//     * @throws IOException                when json parsing fails
//     * @throws InterruptedException       When HttpClient throws it.
//     * @throws CreatePublicationException When publication api service responds with failure.
//     */
//    public PublicationResponse createPublication(CreatePublicationRequest request, URI apiUrl, String authorization)
//        throws IOException, InterruptedException, CreatePublicationException {
//        String requestBodyString = Json.writeValueAsString(request);
//
//        HttpRequest httpRequest = HttpRequest.newBuilder()
//            .uri(UriWrapper.fromUri(apiUrl).addChild(PATH).getUri())
//            .header(AUTHORIZATION, authorization)
//            .header(CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
//            .POST(BodyPublishers.ofString(requestBodyString))
//            .build();
//
//        HttpResponse<String> response = client.send(httpRequest, BodyHandlers.ofString());
//
//        if (!responseIsSuccessful(response)) {
//            throw new CreatePublicationException(insertionErrorMessage(apiUrl, requestBodyString));
//        }
//
//        return Json.readValue(response.body(), PublicationResponse.class);
//    }
//
//    private String insertionErrorMessage(URI apiUrl, String requestBodyString) {
//        return String.format(INSERTING_PUBLICATION_FAILED, apiUrl, requestBodyString);
//    }
}


