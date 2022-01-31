package no.sikt.nva.scopus;

import static no.sikt.nva.scopus.ScopusConstants.DOI_OPEN_URL_FORMAT;
import jakarta.xml.bind.JAXB;
import java.io.StringWriter;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.ItemidTp;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.YesnoAtt;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Reference;
import nva.commons.core.paths.UriWrapper;

class ScopusConverter {

    private final DocTp docTp;

    protected ScopusConverter(DocTp docTp) {
        this.docTp = docTp;
    }

    public CreatePublicationRequest generateCreatePublicationRequest() {
        CreatePublicationRequest createPublicationRequest = new CreatePublicationRequest();
        createPublicationRequest.setAdditionalIdentifiers(generateAdditionalIdentifiers());
        createPublicationRequest.setEntityDescription(generateEntityDescription());
        return createPublicationRequest;
    }

    private EntityDescription generateEntityDescription() {
        EntityDescription entityDescription = new EntityDescription();
        entityDescription.setReference(generateReference());
        entityDescription.setMainTitle(extractMainTitle());
        entityDescription.setContributors(generateContributors());
        return entityDescription;
    }

    private String extractMainTitle() {
        return getMainTitleTextTp()
            .map(this::marshallMainTitleToXmlPreservingUnderlyingStructure)
            .orElse(null);
    }

    private List<Contributor> generateContributors() {
        return extractAuthorGroup()
            .stream()
            .map(this::generateContributorsFromAuthorGroup)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private Reference generateReference() {
        Reference reference = new Reference();
        reference.setDoi(extractDOI());
        return reference;
    }

    private URI extractDOI() {
        return new UriWrapper(DOI_OPEN_URL_FORMAT).addChild(docTp.getMeta().getDoi()).getUri();
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
        return docTp.getItem().getItem().getBibrecord().getHead().getCitationTitle().getTitletext();
    }

    private List<Contributor> generateContributorsFromAuthorGroup(AuthorGroupTp authorGroupTp) {
        return authorGroupTp.getAuthorOrCollaboration()
            .stream()
            .map(this::generateContributorFromAuthorOrCollaboration)
            .collect(Collectors.toList());
    }

    private Contributor generateContributorFromAuthorOrCollaboration(Object authorOrCollaboration) {
        return authorOrCollaboration instanceof AuthorTp
                   ? generateContributorFromAuthorTp((AuthorTp) authorOrCollaboration)
                   : generateContributorFromCollaborationTp(
                       (CollaborationTp) authorOrCollaboration);
    }

    private Contributor generateContributorFromAuthorTp(AuthorTp author) {
        var identity = new Identity();
        identity.setName(determineContributorName(author));
        return new Contributor(identity, null, null, getSequenceNumber(author), false);
    }

    private Contributor generateContributorFromCollaborationTp(CollaborationTp collaboration) {
        var identity = new Identity();
        identity.setName(determineContributorName(collaboration));
        return new Contributor(identity, null, null, getSequenceNumber(collaboration), false);
    }

    private int getSequenceNumber(AuthorTp authorTp) {
        return Integer.parseInt(authorTp.getSeq());
    }

    private int getSequenceNumber(CollaborationTp collaborationTp) {
        return Integer.parseInt(collaborationTp.getSeq());
    }

    private String determineContributorName(AuthorTp author) {
        return author.getPreferredName().getIndexedName();
    }

    private String determineContributorName(CollaborationTp collaborationTp) {
        return collaborationTp.getIndexedName();
    }

    private List<AuthorGroupTp> extractAuthorGroup() {
        return docTp.getItem().getItem().getBibrecord().getHead().getAuthorGroup();
    }

    private Set<AdditionalIdentifier> generateAdditionalIdentifiers() {
        return extractItemIdentifiers()
            .stream()
            .filter(this::isScopusIdentifier)
            .map(this::toAdditionalIdentifier)
            .collect(Collectors.toSet());
    }

    private String marshallMainTitleToXmlPreservingUnderlyingStructure(TitletextTp contents) {
        StringWriter sw = new StringWriter();
        JAXB.marshal(contents, sw);
        return sw.toString();
    }

    private List<ItemidTp> extractItemIdentifiers() {
        return docTp.getItem()
            .getItem()
            .getBibrecord()
            .getItemInfo()
            .getItemidlist()
            .getItemid();
    }

    private boolean isScopusIdentifier(ItemidTp itemIdTp) {
        return itemIdTp.getIdtype().equalsIgnoreCase(ScopusConstants.SCOPUS_ITEM_IDENTIFIER_SCP_FIELD_NAME);
    }

    private AdditionalIdentifier toAdditionalIdentifier(ItemidTp itemIdTp) {
        return new AdditionalIdentifier(ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME,
                                        itemIdTp.getValue());
    }
}
