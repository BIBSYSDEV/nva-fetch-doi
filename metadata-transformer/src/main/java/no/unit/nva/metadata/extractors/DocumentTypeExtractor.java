package no.unit.nva.metadata.extractors;

import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.pages.Pages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;

public final class DocumentTypeExtractor {
    private static final Logger logger = LoggerFactory.getLogger(DocumentTypeExtractor.class);
    public static final Function<ExtractionPair, EntityDescription> apply = extractOrConsumeError();

    private DocumentTypeExtractor() {

    }

    private static Function<ExtractionPair, EntityDescription> extractOrConsumeError() {
        return (extractionPair) -> attempt(() -> extract(extractionPair))
                .orElse(fail -> defaultValue(extractionPair));
    }

    private static EntityDescription defaultValue(ExtractionPair extractionPair) {
        logger.warn("Could not extract type metadata from statement " + extractionPair.getStatement());
        extractionPair.getEntityDescription().setReference(null);
        return extractionPair.getEntityDescription();
    }

    private static EntityDescription extract(ExtractionPair extractionPair) throws InvalidIssnException,
            InvalidIsbnException  {
        if (extractionPair.isDocumentTypeIndicator()) {
            addDocumentTypeInformation(extractionPair);
        }
        return extractionPair.getEntityDescription();
    }

    private static void addDocumentTypeInformation(ExtractionPair extractionPair)
            throws InvalidIsbnException, InvalidIssnException  {
        Reference reference = ExtractorUtil.getReference(extractionPair.getEntityDescription());
        String isxn = extractionPair.getStatementLiteral();
        if (extractionPair.isBook()) {
            generateInstanceAndContextForBook(isxn, reference);
        }
        generateInstanceAndContextForJournal(isxn, reference);
    }

    private static void generateInstanceAndContextForJournal(String issn, Reference reference)
            throws InvalidIssnException {
        if (isNull(reference.getPublicationInstance()) && isNull(reference.getPublicationContext())) {
            var instanceType = new AcademicArticle(null, null, null, null);
            UnconfirmedJournal contextType = new UnconfirmedJournal(null, null, issn);
            reference.setPublicationInstance(instanceType);
            reference.setPublicationContext(contextType);
        }
    }

    private static void generateInstanceAndContextForBook(String isbn, Reference reference)
            throws InvalidIsbnException {
        PublicationContext context = reference.getPublicationContext();
        if (hasExistingInstanceAndContext(reference.getPublicationInstance(), context)) {
            List<String> existingIsbns = ((Book) context).getIsbnList();
            List<String> isbnList = nonNull(existingIsbns) ? new ArrayList<>(existingIsbns) : new ArrayList<>();
            reference.setPublicationContext(addNonPreexistingIsbn(isbn, (Book) context, isbnList));
        } else {
            BookMonograph instanceType = new AcademicMonograph(null);
            Book contextType = new Book(null, null, null, List.of(isbn), null);
            reference.setPublicationInstance(instanceType);
            reference.setPublicationContext(contextType);
        }
    }

    private static Book addNonPreexistingIsbn(String isbn, Book context, List<String> isbnList) {
        if (!isbnList.contains(isbn)) {
            isbnList.add(isbn);
            return new Book(context.getSeries(),
                            context.getSeriesNumber(),
                            context.getPublisher(),
                            isbnList,
                            null);
        }
        return context;
    }

    private static boolean hasExistingInstanceAndContext(PublicationInstance<? extends Pages> instance,
                                                         PublicationContext context) {
        return nonNull(instance) && instance instanceof BookMonograph
                && nonNull(context) && context instanceof Book;
    }
}
