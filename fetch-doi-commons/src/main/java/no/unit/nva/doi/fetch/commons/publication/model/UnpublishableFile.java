package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.time.Instant;
import java.util.UUID;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonTypeInfo(use = Id.NAME, property = "type")
public class UnpublishableFile extends File {

    public UnpublishableFile(@JsonProperty("identifier") UUID identifier,
                             @JsonProperty("mimeType") String mimeType,
                             @JsonProperty("embargoDate") Instant embargoDate,
                             @JsonProperty("administrativeAgreement") boolean administrativeAgreement) {
        super(identifier, mimeType, embargoDate, administrativeAgreement);
    }

    @Override
    @JsonIgnore
    public boolean isVisibleForNonOwner() {
        return false;
    }
}
