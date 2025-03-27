package no.unit.nva.metadata.extractors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationContext;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationInstance;
import no.unit.nva.doi.fetch.commons.publication.model.Reference;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.Book;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicArticle;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicMonograph;
import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.commons.validator.routines.ISSNValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DocumentTypeExtractor {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTypeExtractor.class);
    private static final String SPACES_AND_HYPHENS_REGEX = "[ -]";
    private static final ISBNValidator ISBN_VALIDATOR = new ISBNValidator();

    public static final Function<ExtractionPair, EntityDescription> apply = extractOrConsumeError();

    private DocumentTypeExtractor() {

    }

    private static Function<ExtractionPair, EntityDescription> extractOrConsumeError() {
        return (extractionPair) -> attempt(() -> extract(extractionPair))
                                       .orElse(fail -> defaultValue(extractionPair));
    }

    private static EntityDescription defaultValue(ExtractionPair extractionPair) {
        logger.warn("Could not extract type metadata from statement {}", extractionPair.getStatement());
        extractionPair.getEntityDescription().setReference(null);
        return extractionPair.getEntityDescription();
    }

    private static EntityDescription extract(ExtractionPair extractionPair) throws InvalidIssnException {
        if (extractionPair.isDocumentTypeIndicator()) {
            addDocumentTypeInformation(extractionPair);
        }
        return extractionPair.getEntityDescription();
    }

    private static void addDocumentTypeInformation(ExtractionPair extractionPair)
        throws InvalidIssnException {
        Reference reference = ExtractorUtil.getReference(extractionPair.getEntityDescription());
        String isxn = extractionPair.getStatementLiteral();
        if (extractionPair.isBook()) {
            generateInstanceAndContextForBook(isxn, reference);
        }
        generateInstanceAndContextForJournal(isxn, reference);
    }

    private static String validateIsbn(String isbn) throws InvalidIsbnException {
        var sanitizedIsbn = isbn.replaceAll(SPACES_AND_HYPHENS_REGEX, "");
        var validatedIsbn = ISBN_VALIDATOR.validate(sanitizedIsbn);

        return Optional.ofNullable(validatedIsbn).orElseThrow(() -> new InvalidIsbnException(isbn));
    }

    private static void generateInstanceAndContextForJournal(String issn, Reference reference)
        throws InvalidIssnException {
        if (isNull(reference.getPublicationInstance()) && isNull(reference.getPublicationContext())) {
            var validatedIssn = validateIssn(issn);
            var instanceType = new AcademicArticle(null, null, null);
            UnconfirmedJournal contextType = new UnconfirmedJournal(null, null, validatedIssn);
            reference.setPublicationInstance(instanceType);
            reference.setPublicationContext(contextType);
        }
    }

    private static String validateIssn(String issn) throws InvalidIssnException {
        if (isNull(issn) || issn.isEmpty()) {
            return null;
        }
        if (new ISSNValidator().isValid(issn)) {
            return issn;
        } else {
            throw new InvalidIssnException(issn);
        }
    }

    private static void generateInstanceAndContextForBook(String isbn, Reference reference) {
        var validatedIsbn = attempt(() -> validateIsbn(isbn)).toOptional();
        PublicationContext context = reference.getPublicationContext();
        if (hasExistingInstanceAndContext(reference.getPublicationInstance(), context)) {
            List<String> existingIsbnList = ((Book) context).isbnList();
            List<String> isbnList = nonNull(existingIsbnList) ? new ArrayList<>(existingIsbnList) : new ArrayList<>();

            validatedIsbn.ifPresent(
                presentIsbn -> reference.setPublicationContext(
                    addNonPreexistingIsbn(presentIsbn, (Book) context, isbnList)));
        } else {
            var academicMonograph = new AcademicMonograph(null);
            var isbnList = validatedIsbn.stream().toList();
            Book contextType = new Book(null, null, null, isbnList);
            reference.setPublicationInstance(academicMonograph);
            reference.setPublicationContext(contextType);
        }
    }

    private static Book addNonPreexistingIsbn(String isbn, Book context, List<String> isbnList) {
        if (!isbnList.contains(isbn)) {
            isbnList.add(isbn);
            return new Book(context.series(),
                            context.seriesNumber(),
                            context.publisher(),
                            isbnList);
        } else {
            return context;
        }
    }

    private static boolean hasExistingInstanceAndContext(PublicationInstance instance,
                                                         PublicationContext context) {
        return nonNull(instance) && instance instanceof AcademicMonograph
               && nonNull(context) && context instanceof Book;
    }
}
