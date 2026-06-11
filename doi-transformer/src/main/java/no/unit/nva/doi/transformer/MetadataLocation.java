package no.unit.nva.doi.transformer;

import java.util.Locale;

public enum MetadataLocation {
    CROSSREF, DATACITE;

    private static final String CROSSREF_STRING = "crossref";

    public static MetadataLocation lookup(String location) {
        return locationContainsCrossref(location) ? CROSSREF : DATACITE;
    }

    private static boolean locationContainsCrossref(String location) {
        return location.toLowerCase(Locale.ROOT).contains(CROSSREF_STRING);
    }
}
