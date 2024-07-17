package no.unit.nva.doi.transformer.utils;

import static java.util.Objects.isNull;
import org.apache.commons.validator.routines.ISSNValidator;

public final class IssnUtil {

    private IssnUtil() {
    }

    /**
     * Returns a valid ISSN or null.
     *
     * @param issn a valid ISSN
     * @return String, validated representation of the ISSN
     * @throws InvalidIssnException Thrown if the ISSN is invalid
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static String checkIssn(String issn) throws InvalidIssnException {
        if (isNull(issn) || issn.isEmpty()) {
            return null;
        }
        if (new ISSNValidator().isValid(issn)) {
            return issn;
        } else {
            throw new InvalidIssnException(issn);
        }
    }
}
