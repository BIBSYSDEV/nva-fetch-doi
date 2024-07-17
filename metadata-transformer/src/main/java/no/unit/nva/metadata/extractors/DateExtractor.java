package no.unit.nva.metadata.extractors;

import java.util.function.Function;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationDate;
import no.unit.nva.metadata.type.DcTerms;
import org.eclipse.rdf4j.model.Statement;

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

    // Suppressing "closeResource" is necessary because of a PMD bug
    @SuppressWarnings("PMD.CloseResource")
    private static void addDate(ExtractionPair extractionPair) {
        Statement statement = extractionPair.getStatement();
        if (!DcTerms.DATE.getIri().equals(statement.getPredicate())) {
            return; // We don't yet know what should be mapped
        }
        extractionPair.getEntityDescription().setPublicationDate(extractPublicationDate(extractionPair.getStatementLiteral()));
    }

    private static PublicationDate extractPublicationDate(String date) {
        String[] dateParts = date.split(DATE_SEPARATOR);
        int dateKind = dateParts.length;

        var year = dateParts[YEAR_PART];
        var month = (dateKind > YEAR_ONLY) ? dateParts[MONTH_PART] : null;
        var day = (dateKind == FULL_DATE) ? dateParts[DAY_PART] : null;
        return new PublicationDate(year, month, day);
    }
}
