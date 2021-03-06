package no.unit.nva.doi.transformer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum MetadataLocation {
    CROSSREF("https://api.crossref.org"), DATACITE("https://data.datacite.org");

    public static final String CROSSREF_STRING = "crossref";
    public static final String DATACITE_STRING = "datacite";
    private static final Map<String, MetadataLocation> valuesMap;
    private final String value;

    static {
        valuesMap = new HashMap<>();
        valuesMap.put(CROSSREF.getValue(), CROSSREF);
        valuesMap.put(DATACITE.getValue(), DATACITE);
    }

    MetadataLocation(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Create a MetadataLocation enum instance from a string.
     *
     * @param location a valid location string {@see } .
     * @return a {@link MetadataLocation} object
     */
    public static MetadataLocation lookup(String location) {
        if (locationContainsCrossref(location)) {
            return valuesMap.get(CROSSREF.getValue());
        } else {
            return valuesMap.get(DATACITE.getValue());
        }
    }

    private static boolean locationContainsCrossref(String location) {
        return location.toLowerCase(Locale.getDefault()).contains(CROSSREF_STRING.toLowerCase(Locale.getDefault()));
    }
}
