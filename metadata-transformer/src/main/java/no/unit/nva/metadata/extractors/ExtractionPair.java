package no.unit.nva.metadata.extractors;

import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;
import no.unit.nva.metadata.type.Bibo;
import no.unit.nva.metadata.type.DcTerms;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;

import java.util.Set;
import java.util.regex.Pattern;

public class ExtractionPair {
    private static final Set<IRI> CONTRIBUTOR_PROPERTIES = Set.of(DcTerms.CREATOR.getIri(),
            DcTerms.CONTRIBUTOR.getIri());
    public static final Set<IRI> DATE_PREDICATES = Set.of(DcTerms.DATE.getIri(), DcTerms.DATE_ACCEPTED.getIri(),
            DcTerms.DATE_COPYRIGHTED.getIri(), DcTerms.DATE_SUBMITTED.getIri());
    public static final String DATE_REGEX = "^[0-9]{4}(-[0-9]{2}(-[0-9]{2})?)?$";
    public static final Set<IRI> TAG_IRIS = Set.of(DcTerms.COVERAGE.getIri(), DcTerms.TEMPORAL.getIri(),
            DcTerms.SPATIAL.getIri(), DcTerms.SUBJECT.getIri());
    private final Statement statement;
    private final EntityDescription entityDescription;
    private final boolean abstractPropertyExists;

    public ExtractionPair(Statement statement,
                          EntityDescription entityDescription,
                          boolean abstractPropertyExists) {
        this.statement = statement;
        this.entityDescription = entityDescription;
        this.abstractPropertyExists = abstractPropertyExists;
    }

    public Statement getStatement() {
        return statement;
    }

    public String getStatementLiteral() {
        return statement.getObject().stringValue();
    }

    public EntityDescription getEntityDescription() {
        return entityDescription;
    }

    public boolean hasLiteralObject() {
        return statement.getObject() instanceof Literal;
    }

    public boolean isAbstract() {
        return hasLiteralObject() && (docHasAbstract() || docHasDescriptionServingAsAbstract());
    }

    private boolean docHasAbstract() {
        return DcTerms.ABSTRACT.getIri().equals(statement.getPredicate());
    }

    private boolean docHasDescriptionServingAsAbstract() {
        return DcTerms.DESCRIPTION.getIri().equals(statement.getPredicate()) && !abstractPropertyExists;
    }

    public boolean isDescription() {
        return DcTerms.DESCRIPTION.getIri().equals(statement.getPredicate())
                && hasLiteralObject()
                && abstractPropertyExists;
    }

    public boolean isContributor() {
        return CONTRIBUTOR_PROPERTIES.contains(statement.getPredicate()) && hasLiteralObject();
    }

    public boolean isDate() {
        return DATE_PREDICATES.contains(statement.getPredicate())
                && Pattern.matches(DATE_REGEX, statement.getObject().stringValue());
    }

    public boolean isDocumentTypeIndicator() {
        return isBook() || isJournal();
    }

    public boolean isBook() {
        return Bibo.ISBN.getIri().equals(statement.getPredicate()) && hasLiteralObject();
    }

    public boolean isJournal() {
        return Bibo.ISSN.getIri().equals(statement.getPredicate()) && hasLiteralObject();
    }

    public boolean isDoi() {
        return Bibo.DOI.getIri().equals(statement.getPredicate()) && statement.getObject() instanceof IRI;
    }

    public boolean isLanguage() {
        return DcTerms.LANGUAGE.getIri().equals(statement.getPredicate()) && hasLiteralObject();
    }

    public boolean isTag() {
        return TAG_IRIS.contains(statement.getPredicate()) && hasLiteralObject();
    }

    public boolean isTitle() {
        return DcTerms.TITLE.getIri().equals(statement.getPredicate()) && hasLiteralObject();
    }
}
