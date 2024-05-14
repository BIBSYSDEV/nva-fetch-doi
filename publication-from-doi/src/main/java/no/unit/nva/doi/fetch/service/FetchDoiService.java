package no.unit.nva.doi.fetch.service;

import static java.util.Objects.isNull;
import static no.unit.nva.doi.fetch.RestApiConfig.restServiceObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.doi.DataciteContentType;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.MetadataAndContentLocation;
import no.unit.nva.doi.fetch.DoiValidator;
import no.unit.nva.doi.fetch.exceptions.CreatePublicationException;
import no.unit.nva.doi.fetch.exceptions.MalformedRequestException;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.IdentityUpdater;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.PublicationPersistenceService;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.doi.transformer.utils.CristinProxyClient;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchDoiService {

    public static final String NO_METADATA_FOUND = "No metadata found for imported uri";
    private static final Logger logger = LoggerFactory.getLogger(FetchDoiService.class);
    private final transient DoiTransformService doiTransformService;
    private final transient DoiProxyService doiProxyService;
    private final transient CristinProxyClient cristinProxyClient;
    private final transient MetadataService metadataService;

    public FetchDoiService(DoiTransformService doiTransformService,
                            DoiProxyService doiProxyService,
                            CristinProxyClient cristinProxyClient,
                            MetadataService metadataService) {
        this.doiTransformService = doiTransformService;
        this.doiProxyService = doiProxyService;
        this.cristinProxyClient = cristinProxyClient;
        this.metadataService = metadataService;
    }

    public CreatePublicationRequest newCreatePublicationRequest(String owner, URI customerId, URL url)
        throws URISyntaxException, IOException, InvalidIssnException,
               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CreatePublicationRequest request;

        if (urlIsValidDoi(url)) {
            logger.info("URL is a DOI");
            request = getPublicationFromDoi(owner, customerId, url);
        } else {
            logger.info("URL is NOT a DOI, falling back to web metadata scraping");
            request = getPublicationFromOtherUrl(url);
        }

        request.getEntityDescription().setMetadataSource(URI.create(url.toString()));

        return request;
    }

    private boolean urlIsValidDoi(URL url) {
        return DoiValidator.validate(url);
    }

    private CreatePublicationRequest getPublicationFromOtherUrl(URL url)
        throws URISyntaxException, MetadataNotFoundException {
        return metadataService.generateCreatePublicationRequest(url.toURI())
                   .map(request -> saveSourceAsLinkInAssociatedArtifacts(request, url))
                   .orElseThrow(() -> new MetadataNotFoundException(NO_METADATA_FOUND));
    }

    private CreatePublicationRequest saveSourceAsLinkInAssociatedArtifacts(CreatePublicationRequest request, URL url) {
        request.setAssociatedArtifacts(associatedArtifactsWithLinkToMetadataSource(url));
        return request;
    }

    private AssociatedArtifactList associatedArtifactsWithLinkToMetadataSource(URL url) {
        var associatedArtifact = new AssociatedLink(URI.create(url.toString()), null, null);
        return new AssociatedArtifactList(associatedArtifact);
    }

    private CreatePublicationRequest getPublicationFromDoi(String owner, URI customerId, URL doi)
        throws URISyntaxException, IOException, InvalidIssnException,
               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {
        var publicationMetadata = getPublicationMetadataFromDoi(doi, owner, customerId);
        Publication publication =
            IdentityUpdater.enrichPublicationCreators(cristinProxyClient, publicationMetadata);
        return restServiceObjectMapper.convertValue(publication, CreatePublicationRequest.class);
    }

    private Publication getPublicationMetadataFromDoi(URL doiUrl,
                                                      String owner, URI customerId)
        throws URISyntaxException, IOException, InvalidIssnException,
               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {

        MetadataAndContentLocation metadataAndContentLocation = doiProxyService.lookupDoiMetadata(
            doiUrl.toString(), DataciteContentType.DATACITE_JSON);

        return doiTransformService.transformPublication(
            metadataAndContentLocation.getJson(),
            metadataAndContentLocation.getContentHeader(), owner, customerId);
    }

}
