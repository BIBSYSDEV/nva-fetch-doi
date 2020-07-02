package no.unit.nva.doi.transformer.utils;

import static java.util.Objects.isNull;

import java.util.Arrays;

public enum LicensingIndicator {
    ANY_CREATIVE_COMMONS("creativecommons", true),
    EUROPEAN_REPOSITORY_SYSTEMS_CLOSED_ACCESS("info:eu-repo/semantics/closedAccess", false),
    EUROPEAN_REPOSITORY_SYSTEMS_EMBARGOED_ACCESS("info:eu-repo/semantics/embargoedAccess", false),
    EUROPEAN_REPOSITORY_SYSTEMS_RESTRICTED_ACCESS("info:eu-repo/semantics/restrictedAccess", false),
    EUROPEAN_REPOSITORY_SYSTEMS_OPEN_ACCESS("info:eu-repo/semantics/openAccess", true);

    private final String matcher;
    private final boolean openLicense;

    LicensingIndicator(String matcher, boolean openLicense) {
        this.matcher = matcher;
        this.openLicense = openLicense;
    }

    /**
     * Takes a license URI or URN as a String and matches against known license types.
     * If the input is unknown, false is returned.
     *
     * <p>The methodology here is that ALL creative commons licenses are considered open, while ERS/COAR URNs
     * reflect open/closed status directly. An unknown license is assumed to be not open.
     *
     * @param input a string representing a URI or a URN
     * @return boolean whether the corresponding license is considered open
     */
    public static boolean isOpen(String input) {
        if (isNull(input)) {
            return false;
        }

        return Arrays.stream(values())
            .filter(licensingIndicator -> input.contains(licensingIndicator.getMatcher()))
            .anyMatch(LicensingIndicator::isOpenLicense);
    }

    /**
     * Get the matcher.
     * @return returns a string to be used for matching associated licenses.
     */
    public String getMatcher() {
        return matcher;
    }

    /**
     * Gets the openness of the license as far as NVA is concerned.
     * @return boolean True is open, false is closed.
     */
    public boolean isOpenLicense() {
        return openLicense;
    }
}
