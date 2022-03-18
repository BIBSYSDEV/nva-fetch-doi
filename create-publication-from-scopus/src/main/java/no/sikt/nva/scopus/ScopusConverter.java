package no.sikt.nva.scopus;

import static com.google.common.net.HttpHeaders.CONTENT_LENGTH;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME;
import static no.sikt.nva.scopus.ScopusConstants.DOI_OPEN_URL_FORMAT;
import static no.sikt.nva.scopus.ScopusConstants.INF_END;
import static no.sikt.nva.scopus.ScopusConstants.INF_START;
import static no.sikt.nva.scopus.ScopusConstants.SUP_END;
import static no.sikt.nva.scopus.ScopusConstants.SUP_START;
import static nva.commons.core.StringUtils.isEmpty;

import jakarta.xml.bind.JAXBElement;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.scopus.generated.AbstractTp;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorKeywordTp;
import no.scopus.generated.AuthorKeywordsTp;
import no.scopus.generated.CitationInfoTp;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.DateSortTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.InfTp;
import no.scopus.generated.OpenAccessType;
import no.scopus.generated.SupTp;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.UpwOaLocationType;
import no.scopus.generated.UpwOpenAccessType;
import no.scopus.generated.YesnoAtt;
import no.sikt.nva.scopus.conversion.ContributorExtractor;
import no.sikt.nva.scopus.conversion.PublicationContextCreator;
import no.sikt.nva.scopus.conversion.PublicationInstanceCreator;
import no.unit.nva.file.model.File;
import no.unit.nva.file.model.FileSet;
import no.unit.nva.file.model.License;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings({"PMD.GodClass"})
public class ScopusConverter {

    private final DocTp docTp;
    private final MetadataService metadataService;

    protected ScopusConverter(DocTp docTp, MetadataService metadataService) {
        this.docTp = docTp;
        this.metadataService = metadataService;
    }

    public CreatePublicationRequest generateCreatePublicationRequest() {
        CreatePublicationRequest createPublicationRequest = new CreatePublicationRequest();
        createPublicationRequest.setAdditionalIdentifiers(generateAdditionalIdentifiers());
        createPublicationRequest.setEntityDescription(generateEntityDescription());
        generateFileSet().ifPresent(createPublicationRequest::setFileSet);
        return createPublicationRequest;
    }

    private Optional<FileSet> generateFileSet() {
        // TODO: Iterate other UpwOaLocationTypes if UpwBestOaLocationType is not present?
        List<File> files = extractUpwBestOaLocationType().stream()
                .map(this::generateFile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        return Optional.of(new FileSet(files));
    }

    private Optional<UpwOaLocationType> extractUpwBestOaLocationType() {
        return Optional.ofNullable(docTp.getMeta().getOpenAccess())
                .map(OpenAccessType::getUpwOpenAccess)
                .map(UpwOpenAccessType::getUpwBestOaLocation);
    }

    private Optional<File> generateFile(UpwOaLocationType upwOaLocationType) {
        return nonNull(upwOaLocationType.getUpwUrlForPdf())
                ? Optional.of(createFile(upwOaLocationType))
                : Optional.empty();
    }

    private File createFile(UpwOaLocationType upwOaLocationType) {
        File.Builder builder = new File.Builder();
        builder.withName(upwOaLocationType.getUpwUrlForPdf());
        extractLicense(upwOaLocationType).ifPresent(builder::withLicense);
        Optional<HttpHeaders> httpHeaders = fetchHeadersFromPdfUrl(upwOaLocationType);
        if (httpHeaders.isPresent()) {
            HttpHeaders headers = httpHeaders.get();
            headers.firstValueAsLong(CONTENT_LENGTH).ifPresent(builder::withSize);
            headers.firstValue(CONTENT_TYPE).ifPresent(builder::withMimeType);
        }
        return builder.build();
    }

    private Optional<HttpHeaders> fetchHeadersFromPdfUrl(UpwOaLocationType upwOaLocationType) {
        return Optional.ofNullable(upwOaLocationType.getUpwUrlForPdf()).stream()
                .map(metadataService::fetchHeadResponseHeadersFromUrl)
                .flatMap(Optional::stream)
                .findAny();
    }

    private Optional<License> extractLicense(UpwOaLocationType upwOaLocationType) {
        return Optional.ofNullable(upwOaLocationType.getUpwLicense()).stream()
                .map(this::mapUpwLicenseToLicense)
                .findFirst();
    }

    private License mapUpwLicenseToLicense(String scopusLicense) {
        // TODO: Mapping from scopus-xml license to License
        License.Builder builder = new License.Builder();
        builder.withIdentifier(scopusLicense);
        return builder.build();
    }

    private Optional<AuthorKeywordsTp> extractAuthorKeyWords() {
        return Optional.ofNullable(extractHead())
            .map(HeadTp::getCitationInfo)
            .map(CitationInfoTp::getAuthorKeywords);
    }

    private HeadTp extractHead() {
        return docTp.getItem().getItem().getBibrecord().getHead();
    }

    private EntityDescription generateEntityDescription() {
        EntityDescription entityDescription = new EntityDescription();
        entityDescription.setReference(generateReference());
        entityDescription.setMainTitle(extractMainTitle());
        entityDescription.setAbstract(extractMainAbstract());
        entityDescription.setContributors(new ContributorExtractor(
            extractCorrespondence(), extractAuthorGroup()).generateContributors());
        entityDescription.setTags(generateTags());
        entityDescription.setDate(extractPublicationDate());
        return entityDescription;
    }

    private PublicationDate extractPublicationDate() {
        var publicationDate = getDateSortTp();
        return new PublicationDate.Builder().withDay(publicationDate.getDay())
            .withMonth(publicationDate.getMonth())
            .withYear(publicationDate.getYear())
            .build();
    }

    /*
    According to the "SciVerse SCOPUS CUSTOM DATA DOCUMENTATION" dateSort contains the publication date if it exists,
     if not there are several rules to determine what's the second-best date is. See "SciVerse SCOPUS CUSTOM DATA
     DOCUMENTATION" for details.
     */
    private DateSortTp getDateSortTp() {
        return docTp.getItem().getItem().getProcessInfo().getDateSort();
    }

    private String extractMainAbstract() {
        return getMainAbstract().flatMap(this::extractAbstractStringOrReturnNull).orElse(null);
    }

    private Optional<String> returnNullInsteadOfEmptyString(String input) {
        return isEmpty(input.trim()) ? Optional.empty() : Optional.of(input);
    }

    private Optional<String> extractAbstractStringOrReturnNull(AbstractTp abstractTp) {
        return returnNullInsteadOfEmptyString(extractAbstractString(abstractTp));
    }

    private String extractAbstractString(AbstractTp abstractTp) {
        return
            abstractTp
                .getPara()
                .stream()
                .map(para -> extractContentAndPreserveXmlSupAndInfTags(para.getContent()))
                .collect(Collectors.joining());
    }

    private Optional<AbstractTp> getMainAbstract() {
        return nonNull(getAbstracts())
                   ? getAbstracts().stream().filter(this::isOriginalAbstract).findFirst()
                   : Optional.empty();
    }

    private List<AbstractTp> getAbstracts() {
        return nonNull(extractHead().getAbstracts())
                   ? extractHead().getAbstracts().getAbstract()
                   : null;
    }

    private boolean isOriginalAbstract(AbstractTp abstractTp) {
        return YesnoAtt.Y.equals(abstractTp.getOriginal());
    }

    private List<String> generateTags() {
        return extractAuthorKeyWords()
            .map(this::extractKeywordsAsStrings)
            .orElse(emptyList());
    }

    private List<String> extractKeywordsAsStrings(AuthorKeywordsTp authorKeywordsTp) {
        return authorKeywordsTp
            .getAuthorKeyword()
            .stream()
            .map(this::extractConcatenatedKeywordString)
            .collect(Collectors.toList());
    }

    private String extractConcatenatedKeywordString(AuthorKeywordTp keyword) {
        return keyword
            .getContent()
            .stream()
            .map(ScopusConverter::extractContentAndPreserveXmlSupAndInfTags)
            .collect(Collectors.joining());

    }

    public static String extractContentString(Object content) {
        if (content instanceof String) {
            return ((String) content).trim();
        } else if (content instanceof JAXBElement) {
            return extractContentString(((JAXBElement<?>) content).getValue());
        } else if (content instanceof SupTp) {
            return extractContentString(((SupTp) content).getContent());
        } else if (content instanceof InfTp) {
            return extractContentString(((InfTp) content).getContent());
        } else {
            return ((ArrayList<?>) content).stream()
                .map(ScopusConverter::extractContentString)
                .collect(Collectors.joining());
        }
    }

    public static String extractContentAndPreserveXmlSupAndInfTags(Object content) {
        if (content instanceof String) {
            return ((String) content).trim();
        } else if (content instanceof JAXBElement) {
            return extractContentAndPreserveXmlSupAndInfTags(((JAXBElement<?>) content).getValue());
        } else if (content instanceof SupTp) {
            return SUP_START + extractContentAndPreserveXmlSupAndInfTags(((SupTp) content).getContent()) + SUP_END;
        } else if (content instanceof InfTp) {
            return INF_START + extractContentAndPreserveXmlSupAndInfTags(((InfTp) content).getContent()) + INF_END;
        } else {
            return ((ArrayList<?>) content).stream()
                .map(ScopusConverter::extractContentAndPreserveXmlSupAndInfTags)
                .collect(Collectors.joining());
        }
    }

    private String extractMainTitle() {
        return getMainTitleTextTp()
            .map(this::extractMainTitleContent)
            .orElse(null);
    }

    private String extractMainTitleContent(TitletextTp titletextTp) {
        return extractContentAndPreserveXmlSupAndInfTags(titletextTp.getContent());
    }

    private Reference generateReference() {
        Reference reference = new Reference();
        reference.setPublicationContext(new PublicationContextCreator(metadataService, docTp).getPublicationContext());
        reference.setPublicationInstance(new PublicationInstanceCreator(docTp, reference.getPublicationContext())
                .getPublicationInstance());
        reference.setDoi(extractDOI());
        return reference;
    }

    private URI extractDOI() {
        return nonNull(docTp.getMeta().getDoi())
                   ? UriWrapper.fromUri(DOI_OPEN_URL_FORMAT).addChild(docTp.getMeta().getDoi()).getUri()
                   : null;
    }

    private Optional<TitletextTp> getMainTitleTextTp() {
        return getTitleText()
            .stream()
            .filter(this::isTitleOriginal)
            .findFirst();
    }

    private boolean isTitleOriginal(TitletextTp titletextTp) {
        return titletextTp.getOriginal().equals(YesnoAtt.Y);
    }

    private List<TitletextTp> getTitleText() {
        return extractHead().getCitationTitle().getTitletext();
    }

    private Set<AdditionalIdentifier> generateAdditionalIdentifiers() {
        return Set.of(extractScopusIdentifier());
    }

    private AdditionalIdentifier extractScopusIdentifier() {
        return new AdditionalIdentifier(ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME, docTp.getMeta().getEid());

    }

    private List<AuthorGroupTp> extractAuthorGroup() {
        return extractHead().getAuthorGroup();
    }

    private List<CorrespondenceTp> extractCorrespondence() {
        return extractHead().getCorrespondence();
    }
}
