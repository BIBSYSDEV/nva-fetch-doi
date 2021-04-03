package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.Bibo;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.JacocoGenerated;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public final class DocumentTypeExtractor {
    private static final Set<IRI> DOCUMENT_TYPE_INDICATORS = Set.of(Bibo.ISSN.getIri(), Bibo.ISBN.getIri());

    @JacocoGenerated
    private DocumentTypeExtractor() {

    }

    public static void extract(EntityDescription entityDescription, Statement statement) throws InvalidIssnException,
            InvalidIsbnException {
        if (isDocumentTypeIndicator(statement.getPredicate())) {
            addDocumentTypeInformation(entityDescription, statement);
        }
    }

    private static boolean isDocumentTypeIndicator(IRI candidate) {
        return DOCUMENT_TYPE_INDICATORS.contains(candidate);
    }

    private static void addDocumentTypeInformation(EntityDescription entityDescription, Statement statement)
            throws InvalidIsbnException, InvalidIssnException {
        Value object = statement.getObject();
        if (ExtractorUtil.isNotLiteral(object)) {
            return;
        }
        Reference reference = ExtractorUtil.getReference(entityDescription);
        if (Bibo.ISBN.getIri().equals(statement.getPredicate())) {
            generateInstanceAndContextForBook(object, reference);
        } else if (Bibo.ISSN.getIri().equals(statement.getPredicate())) {
            generateInstanceAndContextForJournal(object, reference);
        }
    }

    private static void generateInstanceAndContextForJournal(Value object, Reference reference)
            throws InvalidIssnException {
        PublicationInstance<? extends Pages> instance = reference.getPublicationInstance();
        if (isNull(instance)) {
            JournalArticle instanceType = new JournalArticle();
            Journal contextType = new Journal.Builder()
                    .withOnlineIssn(object.stringValue())
                    .build();
            reference.setPublicationInstance(instanceType);
            reference.setPublicationContext(contextType);
        }
    }

    private static void generateInstanceAndContextForBook(Value object, Reference reference)
            throws InvalidIsbnException {
        PublicationInstance<? extends Pages> instance = reference.getPublicationInstance();
        PublicationContext context = reference.getPublicationContext();
        if (hasExistingInstanceAndContext(instance, context)) {
            List<String> existingIsbns = ((Book) context).getIsbnList();
            List<String> isbnList = nonNull(existingIsbns) ? new ArrayList<>(existingIsbns) : new ArrayList<>();
            addNonPreexistingIsbn(object, (Book) context, isbnList);
        } else {
            BookMonograph instanceType = new BookMonograph.Builder().build();
            Book contextType = new Book.Builder().withIsbnList(List.of(object.stringValue())).build();
            reference.setPublicationInstance(instanceType);
            reference.setPublicationContext(contextType);
        }
    }

    private static void addNonPreexistingIsbn(Value object, Book context, List<String> isbnList)
            throws InvalidIsbnException {
        if (!isbnList.contains(object.stringValue())) {
            isbnList.add(object.stringValue());
            context.setIsbnList(isbnList);
        }
    }

    private static boolean hasExistingInstanceAndContext(PublicationInstance<? extends Pages> instance,
                                                         PublicationContext context) {
        return nonNull(instance) && instance instanceof BookMonograph
                && nonNull(context) && context instanceof Book;
    }
}
