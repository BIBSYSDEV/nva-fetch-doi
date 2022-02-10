package no.sikt.nva.scopus.conversion;

import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import java.util.Optional;
import no.scopus.generated.DocTp;
import no.scopus.generated.IssnTp;
import no.scopus.generated.MetaTp;
import no.scopus.generated.SourceTp;
import no.sikt.nva.scopus.ScopusConstants;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Periodical;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import nva.commons.core.SingletonCollector;

public class JournalCreator {

    public static final String DASH = "-";
    public static final int START_YEAR_FOR_LEVEL_INFO = 2004;

    private final MetadataService metadataService;
    private final DocTp docTp;

    public JournalCreator(MetadataService metadataService, DocTp docTp) {
        this.metadataService = metadataService;
        this.docTp = docTp;
    }

    public Periodical createJournal() {
        return createConfirmedJournal().orElseGet(this::createUnconfirmedJournal);
    }

    private Optional<Periodical> createConfirmedJournal() {
        return thereIsLevelInformationForPublication()
                   ? fetchPeriodicalInfoFromPublicationChannels()
                   : Optional.empty();
    }

    private Optional<Periodical> fetchPeriodicalInfoFromPublicationChannels() {
        var sourceTitle = findSourceTitle();
        var printIssn = findPrintIssn().orElse(null);
        var electronicIssn = findElectronicIssn().orElse(null);
        var publicationYear = findPublicationYear().orElseThrow();

        return metadataService
            .lookUpJournalIdAtPublicationChannel(sourceTitle, electronicIssn, printIssn, publicationYear)
            .map(Journal::new);
    }

    private boolean thereIsLevelInformationForPublication() {
        return findPublicationYear().map(year -> year >= START_YEAR_FOR_LEVEL_INFO).orElse(false);
    }

    private UnconfirmedJournal createUnconfirmedJournal() {
        var sourceTitle = findSourceTitle();
        var printIssn = findPrintIssn().orElse(null);
        var electronicIssn = findElectronicIssn().orElse(null);
        return attempt(() -> new UnconfirmedJournal(sourceTitle, printIssn, electronicIssn)).orElseThrow();
    }

    private Optional<Integer> findPublicationYear() {
        return Optional.ofNullable(docTp)
            .map(DocTp::getMeta)
            .map(MetaTp::getPubYear)
            .map(Integer::parseInt);
    }

    private String findSourceTitle() {
        StringBuilder sourceTitle = new StringBuilder();
        getSource().getSourcetitle().getContent().forEach(sourceTitle::append);
        return sourceTitle.toString();
    }

    private Optional<String> findElectronicIssn() {
        return findIssn(getSource().getIssn(), ScopusConstants.ISSN_TYPE_ELECTRONIC);
    }

    private Optional<String> findPrintIssn() {
        return findIssn(getSource().getIssn(), ScopusConstants.ISSN_TYPE_PRINT);
    }

    private SourceTp getSource() {
        return docTp.getItem().getItem().getBibrecord().getHead().getSource();
    }

    private Optional<String> findIssn(List<IssnTp> issnTpList, String issnType) {
        return Optional.ofNullable(issnTpList.stream()
                                       .filter(issn -> issnType.equals(issn.getType()))
                                       .map(IssnTp::getContent)
                                       .map(this::addDashToIssn)
                                       .collect(SingletonCollector.collectOrElse(null)));
    }

    private String addDashToIssn(String issn) {
        return issn.contains(DASH) ? issn : issn.substring(0, 4) + DASH + issn.substring(4);
    }
}
