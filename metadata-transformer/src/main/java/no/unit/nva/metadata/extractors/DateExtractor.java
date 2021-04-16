package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import org.eclipse.rdf4j.model.Statement;

import java.util.function.Function;

public final class DateExtractor {
    public static final String DATE_SEPARATOR = "-";
    public static final int FULL_DATE = 3;
    public static final int YEAR_ONLY = 1;
    public static final Function<ExtractionPair, EntityDescription> apply = DateExtractor::extract;
    public static final int YEAR_PART = 0;
    public static final int MONTH_PART = 1;
    public static final int DAY_PART = 2;

    private DateExtractor() {

    }

    private static EntityDescription extract(ExtractionPair extractionPair) {
        if (extractionPair.isDate()) {
            addDate(extractionPair);
        }
        return extractionPair.getEntityDescription();
    }

    private static void addDate(ExtractionPair extractionPair) {
        Statement statement = extractionPair.getStatement();
        if (!DcTerms.DATE.getIri().equals(statement.getPredicate())) {
            return; // We don't yet know what should be mapped
        }
        extractionPair.getEntityDescription().setDate(extractPublicationDate(extractionPair.getStatementLiteral()));
    }

    private static PublicationDate extractPublicationDate(String date) {
        String[] dateParts = date.split(DATE_SEPARATOR);
        int dateKind = dateParts.length;

        PublicationDate.Builder publicationDateBuilder = new PublicationDate.Builder()
                .withYear(dateParts[YEAR_PART]);

        if (dateKind > YEAR_ONLY) {
            publicationDateBuilder.withMonth(dateParts[MONTH_PART]);
        }
        if (dateKind == FULL_DATE) {
            publicationDateBuilder.withDay(dateParts[DAY_PART]);
        }
        return publicationDateBuilder.build();
    }
}
