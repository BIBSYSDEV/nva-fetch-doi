package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum VerificationStatus {

    VERIFIED("Verified"), NOT_VERIFIED("NotVerified");

    @JsonValue
    private final String value;

    VerificationStatus(String value) {
        this.value = value;
    }

    public static VerificationStatus fromBoolean(boolean value) {
        return value ? VERIFIED : NOT_VERIFIED;
    }

    public String getValue() {
        return value;
    }
}
