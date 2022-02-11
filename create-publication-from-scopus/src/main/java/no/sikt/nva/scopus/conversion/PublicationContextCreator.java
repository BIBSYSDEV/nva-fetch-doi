package no.sikt.nva.scopus.conversion;

import static no.sikt.nva.scopus.ScopusSourceType.BOOK;
import static no.sikt.nva.scopus.ScopusSourceType.JOURNAL;
import static nva.commons.core.attempt.Try.attempt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import no.scopus.generated.DocTp;
import no.scopus.generated.IssnTp;
import no.scopus.generated.MetaTp;
import no.scopus.generated.PublisherTp;
import no.scopus.generated.SourceTp;
import no.sikt.nva.scopus.ScopusConstants;
import no.sikt.nva.scopus.ScopusSourceType;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Periodical;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;

public class PublicationContextCreator {

    public static final String DASH = "-";
    public static final int START_YEAR_FOR_LEVEL_INFO = 2004;
    public static final String EMPTY_STRING = "";

    private final MetadataService metadataService;
    private final DocTp docTp;

    public PublicationContextCreator(MetadataService metadataService, DocTp docTp) {
        this.metadataService = metadataService;
        this.docTp = docTp;
    }

    public PublicationContext getPublicationContext() {
        if (isJournal()) {
            return createJournal();
        }
        if (isBook()) {
            return createBook();
        }
        return ScopusConstants.EMPTY_PUBLICATION_CONTEXT;
    }

    private boolean isJournal() {
        return Optional.ofNullable(docTp)
                .map(DocTp::getMeta)
                .map(MetaTp::getSrctype)
                .map(srcTyp -> JOURNAL.equals(ScopusSourceType.valueOfCode(srcTyp)))
                .orElse(false);
    }

    private boolean isBook() {
        return Optional.ofNullable(docTp)
                .map(DocTp::getMeta)
                .map(MetaTp::getSrctype)
                .map(srcTyp -> BOOK.equals(ScopusSourceType.valueOfCode(srcTyp)))
                .orElse(false);
    }

    public Periodical createJournal() {
        return createConfirmedJournal().orElseGet(this::createUnconfirmedJournal);
    }

    public Book createBook() {
        BookSeries bookSeries = null;
        String seriesNumber = null;
        PublishingHouse publishingHouse = createPublisher();
        List<String> isbnList = new ArrayList<>();
        return attempt(() -> new Book(bookSeries, seriesNumber, publishingHouse, isbnList)).orElseThrow();
    }

    private PublishingHouse createPublisher() {
        return fetchConfirmedPublisherFromPublicationChannels().orElseGet(this::createUnconfirmedPublisher);
    }


    private Optional<PublishingHouse> fetchConfirmedPublisherFromPublicationChannels() {
        var publisherName = findPublisherName();
        Optional<String> publisherID = Optional.ofNullable(metadataService
                .fetchPublisherIdFromPublicationChannel(publisherName));
        return publisherID.map(id -> new Publisher(UriWrapper.fromUri(id).getUri()));
    }

    private UnconfirmedPublisher createUnconfirmedPublisher() {
        var publisherName = findPublisherName();
        return new UnconfirmedPublisher(publisherName);
    }

    private String findPublisherName() {
        Optional<PublisherTp> publisherTp = docTp.getItem().getItem().getBibrecord().getHead().getSource()
                .getPublisher().stream().findFirst();
        return publisherTp.map(PublisherTp::getPublishername).orElse(EMPTY_STRING);
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
