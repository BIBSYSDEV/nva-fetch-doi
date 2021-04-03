package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import nva.commons.core.JacocoGenerated;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Set;
import java.util.regex.Pattern;

public final class DateExtractor {
    public static final String DATE_SEPARATOR = "-";
    public static final int FULL_DATE = 3;
    public static final int YEAR_ONLY = 1;
    public static final Set<IRI> DATE_PREDICATES = Set.of(DcTerms.DATE.getIri(), DcTerms.DATE_ACCEPTED.getIri(),
            DcTerms.DATE_COPYRIGHTED.getIri(), DcTerms.DATE_SUBMITTED.getIri());
    public static final String DATE_REGEX = "^[0-9]{4}(-[0-9]{2}(-[0-9]{2})?)?$";

    @JacocoGenerated
    private DateExtractor() {

    }

    public static void extract(EntityDescription entityDescription, Statement statement) {
        if (isDate(statement)) {
            addDate(entityDescription, statement);
        }
    }

    private static boolean isDate(Statement candidate) {
        return DATE_PREDICATES.contains(candidate.getPredicate())
                && Pattern.matches(DATE_REGEX, candidate.getObject().stringValue());
    }

    private static void addDate(EntityDescription entityDescription, Statement statement) {
        if (!DcTerms.DATE.getIri().equals(statement.getPredicate())) {
            return; // We don't yet know what should be mapped
        }
        String date = statement.getObject().stringValue();
        String[] dateParts = date.split(DATE_SEPARATOR);
        int length = dateParts.length;

        PublicationDate.Builder publicationDateBuilder = new PublicationDate.Builder()
                .withYear(dateParts[0]);

        if (length > YEAR_ONLY) {
            publicationDateBuilder.withMonth(dateParts[1]);
        }
        if (length == FULL_DATE) {
            publicationDateBuilder.withDay(dateParts[2]);
        }
        entityDescription.setDate(publicationDateBuilder.build());
    }
}
