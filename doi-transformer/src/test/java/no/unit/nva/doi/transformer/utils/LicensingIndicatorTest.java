package no.unit.nva.doi.transformer.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LicensingIndicatorTest {

    public static final String EMPTY_STRING = "";

    @DisplayName("Test LicensingIndicator::isOpen returns true for open licenses")
    @ParameterizedTest
    @ValueSource(strings = {"info:eu-repo/semantics/openAccess", "http://creativecommons.org/licenses/by-nd/3.0/de",
            "http://creativecommons.org/licenses/by/4.0/legalcode"})
    void isOpenReturnsTrueWhenInputLicenseIsOpen(String licenseUriString) {
        assertTrue(LicensingIndicator.isOpen(licenseUriString));
    }

    @DisplayName("Test LicensingIndicator::isOpen returns false for closed licenses")
    @ParameterizedTest
    @ValueSource(strings = {"info:eu-repo/semantics/closedAccess", "info:eu-repo/semantics/embargoedAccess",
            "info:eu-repo/semantics/restrictedAccess"})
    void isOpenReturnsFalseWhenInputLicenseIsClosed(String licenseUriString) {
        assertFalse(LicensingIndicator.isOpen(licenseUriString));
    }

    @DisplayName("Test LicensingIndicator::isOpen returns false for unknown licenses")
    @ParameterizedTest
    @ValueSource(strings = {"http://www.opendatacommons.org/licenses/pddl/1.0/", "http://example.org/made.up.license",
            "info:eurepo/semantics/misspelt"})
    void isOpenReturnsFalseWhenInputLicenseIsUnknown(String licenseUriString) {
        assertFalse(LicensingIndicator.isOpen(licenseUriString));
    }

    @DisplayName("Test LicensingIndicator::isOpen returns false for null")
    @Test
    void isOpenReturnsFalseWhenInputIsNull() {
        assertFalse(LicensingIndicator.isOpen(null));
    }

    @DisplayName("Test LicensingIndicator::isOpen returns false for null")
    @Test
    void isOpenReturnsFalseWhenInputIsEmpty() {
        assertFalse(LicensingIndicator.isOpen(EMPTY_STRING));
    }
}