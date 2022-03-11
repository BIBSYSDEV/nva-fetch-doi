package no.sikt.nva.scopus.conversion;

import static nva.commons.core.attempt.Try.attempt;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.scopus.generated.BibrecordTp;
import no.scopus.generated.CitationInfoTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.IsbnTp;
import no.scopus.generated.IssnTp;
import no.scopus.generated.ItemTp;
import no.scopus.generated.MetaTp;
import no.scopus.generated.OrigItemTp;
import no.scopus.generated.PublisherTp;
import no.scopus.generated.SourceTp;
import no.scopus.generated.SourcetypeAtt;
import no.sikt.nva.scopus.ScopusConstants;
import no.sikt.nva.scopus.exception.UnsupportedSrcTypeException;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Chapter;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Periodical;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;

public class PublicationContextCreator {

    public static final String UNSUPPORTED_SOURCE_TYPE = "Unsupported source type, in %s";
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
        if (isChapter()) {
            return createChapter();
        }
        if (isBook()) {
            return createBook();
        }
        if (isBookSeries()) {
            return createBookInSeries();
        }
        if (isReport()) {
            return createReport();
        }
        if (isConferenceProceeding()) {
            if (hasIsbn() && hasNoIssn()) {
                return createChapter();
            } else {
                return createJournal();
            }
        }
        throw new UnsupportedSrcTypeException(String.format(UNSUPPORTED_SOURCE_TYPE, docTp.getMeta().getEid()));
    }

    private boolean isJournal() {
        return Optional.ofNullable(docTp)
                .map(DocTp::getMeta)
                .map(MetaTp::getSrctype)
                .map(srcType -> srcType.equals(SourcetypeAtt.J.value()))
                .orElse(false);
    }

    private boolean isChapter() {
        return Optional.ofNullable(docTp)
                .map(DocTp::getItem)
                .map(ItemTp::getItem)
                .map(OrigItemTp::getBibrecord)
                .map(BibrecordTp::getHead)
                .map(HeadTp::getCitationInfo)
                .map(CitationInfoTp::getCitationType)
                .orElse(Collections.emptyList())
                .stream()
                .anyMatch(citationType -> CitationtypeAtt.CH.equals(citationType.getCode()));
    }

    private boolean isBook() {
        return Optional.ofNullable(docTp)
                .map(DocTp::getMeta)
                .map(MetaTp::getSrctype)
                .map(srcType -> srcType.equals(SourcetypeAtt.B.value()))
                .orElse(false);
    }

    private boolean isBookSeries() {
        return Optional.ofNullable(docTp)
                .map(DocTp::getMeta)
                .map(MetaTp::getSrctype)
                .map(srcType -> srcType.equals(SourcetypeAtt.K.value()))
                .orElse(false);
    }

    private boolean isReport() {
        return Optional.ofNullable(docTp)
                .map(DocTp::getMeta)
                .map(MetaTp::getSrctype)
                .map(srcType -> srcType.equals(SourcetypeAtt.R.value()))
                .orElse(false);
    }

    private boolean isConferenceProceeding() {
        return Optional.ofNullable(docTp)
                .map(DocTp::getMeta)
                .map(MetaTp::getSrctype)
                .map(srcType -> srcType.equals(SourcetypeAtt.P.value()))
                .orElse(false);
    }

    private boolean hasIsbn() {
        return Optional.ofNullable(docTp)
                .map(DocTp::getItem)
                .map(ItemTp::getItem)
                .map(OrigItemTp::getBibrecord)
                .map(BibrecordTp::getHead)
                .map(HeadTp::getSource)
                .map(SourceTp::getIsbn)
                .stream()
                .noneMatch(List::isEmpty);
    }

    private boolean hasNoIssn() {
        return Optional.ofNullable(docTp)
                .map(DocTp::getItem)
                .map(ItemTp::getItem)
                .map(OrigItemTp::getBibrecord)
                .map(BibrecordTp::getHead)
                .map(HeadTp::getSource)
                .map(SourceTp::getIssn)
                .stream()
                .anyMatch(List::isEmpty);
    }

    public Periodical createJournal() {
        return createConfirmedJournal().orElseGet(this::createUnconfirmedJournal);
    }

    public Book createBook() {
        BookSeries bookSeries = null;
        String seriesNumber = null;
        PublishingHouse publishingHouse = createPublisher();
        List<String> isbnList = findIsbn();
        return attempt(() -> new Book(bookSeries, seriesNumber, publishingHouse, isbnList)).orElseThrow();
    }

    public Book createBookInSeries() {
        BookSeries bookSeries = createBookSeries();
        String seriesNumber = null;
        PublishingHouse publishingHouse = createPublisher();
        List<String> isbnList = findIsbn();
        return attempt(() -> new Book(bookSeries, seriesNumber, publishingHouse, isbnList)).orElseThrow();
    }

    public Book createReport() {
        BookSeries bookSeries = null;
        String seriesTitle = null;
        String seriesNumber = null;
        PublishingHouse publishingHouse = createPublisher();
        List<String> isbnList = Collections.emptyList();
        return attempt(()
                -> new Report(bookSeries, seriesTitle, seriesNumber, publishingHouse, isbnList)).orElseThrow();
    }

    public Chapter createChapter() {
        //Todo: we do not have access to partOf URI for chapter yet -> se a dummy-uri
        return attempt(() -> new Chapter.Builder().withPartOf(ScopusConstants.DUMMY_URI).build()).orElseThrow();
    }

    private PublishingHouse createPublisher() {
        return fetchConfirmedPublisherFromPublicationChannels().orElseGet(this::createUnconfirmedPublisher);
    }

    private BookSeries createBookSeries() {
        return fetchConfirmedSeriesFromPublicationChannels().orElseGet(this::createUnconfirmedSeries);
    }

    private Optional<BookSeries> fetchConfirmedSeriesFromPublicationChannels() {
        var sourceTitle = findSourceTitle();
        var printIssn = findPrintIssn().orElse(null);
        var electronicIssn = findElectronicIssn().orElse(null);
        var publicationYear = findPublicationYear().orElseThrow();
        return metadataService
                .fetchJournalIdFromPublicationChannel(sourceTitle, electronicIssn, printIssn, publicationYear)
                .map(id -> new Series(UriWrapper.fromUri(id).getUri()));
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

    private UnconfirmedSeries createUnconfirmedSeries() {
        var title = findSourceTitle();
        var issn = findPrintIssn().orElse(null);
        var onlineIssn = findElectronicIssn().orElse(null);
        return attempt(() -> new UnconfirmedSeries(title, issn, onlineIssn)).orElseThrow();
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
            .fetchJournalIdFromPublicationChannel(sourceTitle, electronicIssn, printIssn, publicationYear)
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

    private List<String> findIsbn() {
        return findIsbn(getSource().getIsbn());
    }

    private List<String> findIsbn(List<IsbnTp> isbnTpList) {
        return isbnTpList
                .stream()
                .map(IsbnTp::getContent)
                .collect(Collectors.toList());
    }

}
