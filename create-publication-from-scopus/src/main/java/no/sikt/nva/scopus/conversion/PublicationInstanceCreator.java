package no.sikt.nva.scopus.conversion;

import jakarta.xml.bind.JAXBElement;
import no.scopus.generated.BibrecordTp;
import no.scopus.generated.CitationTypeTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.ItemTp;
import no.scopus.generated.OrigItemTp;
import no.scopus.generated.PagerangeTp;
import no.scopus.generated.SourceTp;
import no.scopus.generated.VolissTp;
import no.scopus.generated.VolisspagTp;
import no.sikt.nva.scopus.ScopusConstants;
import no.sikt.nva.scopus.exception.UnsupportedCitationTypeException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticleContentType;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

public class PublicationInstanceCreator {

    public static final String UNSUPPORTED_CITATION_TYPE_MESSAGE = "Unsupported citation type, cannot convert eid %s";

    private final DocTp docTp;

    public PublicationInstanceCreator(DocTp docTp) {
        this.docTp = docTp;
    }

    public PublicationInstance<? extends Pages> getPublicationInstance() {
        return getCitationTypeCode()
                .flatMap(this::convertCitationTypeCodeToPublicationInstance)
                .orElseThrow(this::getUnsupportedCitationTypeException);
    }

    private UnsupportedCitationTypeException getUnsupportedCitationTypeException() {
        return new UnsupportedCitationTypeException(
                String.format(UNSUPPORTED_CITATION_TYPE_MESSAGE, docTp.getMeta().getEid()));
    }

    /*
    See enum explanation in "SCOPUS CUSTOM DATA DOCUMENTATION", copy can be found at
    https://isikt.sharepoint.com/:b:/s/Dovre/EQGVGp2Xn-RDvDi8zg3XFlQB6vo95nGLbINztJcXjStG5w?e=O9wQwB
     */
    private Optional<PublicationInstance<? extends Pages>> convertCitationTypeCodeToPublicationInstance(
            CitationtypeAtt citationtypeAtt) {
        switch (citationtypeAtt) {
            case AR:
                return Optional.of(generateJournalArticle());
            case BK:
                return Optional.of(new BookMonograph());
            case CH:
                return Optional.of(generateChapterArticle());
            case CP:
                if (hasIsbn() && hasNoIssn()) {
                    return Optional.of(generateChapterArticle());
                } else {
                    return Optional.of(generateJournalArticle());
                }
            case ED:
                return Optional.of(generateJournalLeader());
            case ER:
                return Optional.of(generateJournalCorrigendum());
            case LE:
            case NO:
                return Optional.of(generateJournalLetter());
            case RE:
            case SH:
                return Optional.of(generateJournalArticle(JournalArticleContentType.REVIEW_ARTICLE));
            default:
                return Optional.empty();
        }
    }

    private JournalArticle generateJournalArticle(JournalArticleContentType contentType) {
        JournalArticle journalArticle = generateJournalArticle();
        journalArticle.setContentType(contentType);
        return journalArticle;
    }

    private JournalArticle generateJournalArticle() {
        JournalArticle.Builder builder = new JournalArticle.Builder();
        extractPages().ifPresent(builder::withPages);
        extractVolume().ifPresent(builder::withVolume);
        extractIssue().ifPresent(builder::withIssue);
        extractArticleNumber().ifPresent(builder::withArticleNumber);
        return builder.build();
    }

    private JournalLeader generateJournalLeader() {
        JournalLeader.Builder builder = new JournalLeader.Builder();
        extractPages().ifPresent(builder::withPages);
        extractVolume().ifPresent(builder::withVolume);
        extractIssue().ifPresent(builder::withIssue);
        extractArticleNumber().ifPresent(builder::withArticleNumber);
        return builder.build();
    }

    private JournalCorrigendum generateJournalCorrigendum() {
        JournalCorrigendum.Builder builder = new JournalCorrigendum.Builder();
        extractPages().ifPresent(builder::withPages);
        extractVolume().ifPresent(builder::withVolume);
        extractIssue().ifPresent(builder::withIssue);
        extractArticleNumber().ifPresent(builder::withArticleNumber);
        builder.withCorrigendumFor(ScopusConstants.DUMMY_URI);
        return builder.build();
    }

    private JournalLetter generateJournalLetter() {
        JournalLetter.Builder builder = new JournalLetter.Builder();
        extractPages().ifPresent(builder::withPages);
        extractVolume().ifPresent(builder::withVolume);
        extractIssue().ifPresent(builder::withIssue);
        extractArticleNumber().ifPresent(builder::withArticleNumber);
        return builder.build();
    }

    private ChapterArticle generateChapterArticle() {
        ChapterArticle chapterArticle = new ChapterArticle();
        extractPages().ifPresent(chapterArticle::setPages);
        chapterArticle.setContentType(ChapterArticleContentType.ACADEMIC_CHAPTER);
        return chapterArticle;
    }

    private Optional<Range> extractPages() {
        return getVolisspagTpStream()
                .filter(this::isPageRange)
                .map(this::extractPageRange)
                .findAny().orElse(Optional.empty());
    }

    private Stream<JAXBElement<?>> getVolisspagTpStream() {
        return Optional.ofNullable(getSourceTp().getVolisspag())
                .map(VolisspagTp::getContent)
                .orElse(emptyList())
                .stream();
    }

    private Optional<String> extractVolume() {
        return getVolisspagTpStream()
                .filter(this::isVolumeIssue)
                .map(this::extractVolumeValue)
                .findAny().orElse(Optional.empty());
    }

    private Optional<String> extractIssue() {
        return getVolisspagTpStream()
                .filter(this::isVolumeIssue)
                .map(this::extractIssueValue)
                .findAny().orElse(Optional.empty());
    }

    private Optional<String> extractArticleNumber() {
        return Optional.ofNullable(getSourceTp().getArticleNumber());
    }

    private boolean isVolumeIssue(JAXBElement<?> content) {
        return content.getValue() instanceof VolissTp;
    }

    private Optional<String> extractVolumeValue(JAXBElement<?> content) {
        return Optional.ofNullable(((VolissTp) content.getValue()).getVolume());
    }

    private Optional<String> extractIssueValue(JAXBElement<?> content) {
        return Optional.ofNullable(((VolissTp) content.getValue()).getIssue());
    }

    private boolean isPageRange(JAXBElement<?> content) {
        return content.getValue() instanceof PagerangeTp;
    }

    private Optional<Range> extractPageRange(JAXBElement<?> content) {
        return Optional.of(new Range(((PagerangeTp) content.getValue()).getFirst(),
                ((PagerangeTp) content.getValue()).getLast()));
    }

    private SourceTp getSourceTp() {
        return docTp.getItem()
                .getItem()
                .getBibrecord()
                .getHead()
                .getSource();
    }

    private Optional<CitationtypeAtt> getCitationTypeCode() {
        return docTp.getItem()
                .getItem()
                .getBibrecord()
                .getHead()
                .getCitationInfo()
                .getCitationType()
                .stream()
                .findFirst()
                .map(CitationTypeTp::getCode);
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
                .anyMatch(isbnList -> isbnList.size() > 0);
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
}
