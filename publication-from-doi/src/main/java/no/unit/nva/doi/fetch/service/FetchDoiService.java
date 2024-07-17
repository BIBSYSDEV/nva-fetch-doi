package no.unit.nva.doi.fetch.service;

import static no.unit.nva.doi.fetch.RestApiConfig.restServiceObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import no.unit.nva.doi.DataciteContentType;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.MetadataAndContentLocation;
import no.unit.nva.doi.fetch.DoiValidator;
import no.unit.nva.doi.fetch.commons.publication.model.AssociatedArtifact;
import no.unit.nva.doi.fetch.commons.publication.model.AssociatedLink;
import no.unit.nva.doi.fetch.commons.publication.model.CreatePublicationRequest;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.doi.transformer.utils.CristinProxyClient;
import no.unit.nva.doi.transformer.utils.InvalidIssnException;
import no.unit.nva.metadata.service.MetadataService;
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

    public CreatePublicationRequest newCreatePublicationRequest(URL url)
        throws URISyntaxException, IOException, MetadataNotFoundException, UnsupportedDocumentTypeException,
               InvalidIssnException {

        CreatePublicationRequest request;

        if (urlIsValidDoi(url)) {
            logger.info("URL is a DOI");
            request = getPublicationFromDoi(url);
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

    private List<AssociatedArtifact> associatedArtifactsWithLinkToMetadataSource(URL url) {
        return List.of(new AssociatedLink(URI.create(url.toString())));
    }

    private CreatePublicationRequest getPublicationFromDoi(URL doi)
        throws URISyntaxException, IOException, MetadataNotFoundException, InvalidIssnException {

        var publicationMetadata = getPublicationMetadataFromDoi(doi);
        var publication =
            IdentityUpdater.enrichPublicationCreators(cristinProxyClient, publicationMetadata);
        return restServiceObjectMapper.convertValue(publication, CreatePublicationRequest.class);
    }

    private CreatePublicationRequest getPublicationMetadataFromDoi(URL doiUrl)
        throws URISyntaxException, IOException, MetadataNotFoundException, InvalidIssnException {

        MetadataAndContentLocation metadataAndContentLocation = doiProxyService.lookupDoiMetadata(
            doiUrl.toString(), DataciteContentType.DATACITE_JSON);

        return doiTransformService.transformPublication(
            metadataAndContentLocation.getJson(),
            metadataAndContentLocation.getContentHeader());
    }

}
