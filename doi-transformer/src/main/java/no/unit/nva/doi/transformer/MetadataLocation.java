package no.unit.nva.doi.transformer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public enum MetadataLocation {
    CROSSREF("https://api.crossref.org"), DATACITE("https://data.datacite.org");

    public static final String CROSSREF_STRING = "crossref";
    private static String ILLEGAL_ARGUMENT_ERROR = "Illegal MetadataLocation value. Valid values are:";
    private static Map<String, MetadataLocation> valuesMap;
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

    private static String errorMessage() {
        List<String> valueList = Arrays.stream(MetadataLocation.values()).map(MetadataLocation::getValue)
                                       .collect(Collectors.toList());
        String valuesString = String.join(",", valueList);
        return ILLEGAL_ARGUMENT_ERROR + valuesString;
    }
}
