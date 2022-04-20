package no.sikt.nva.scopus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.events.models.EventBody;
import nva.commons.core.JacocoGenerated;


import static nva.commons.core.attempt.Try.attempt;

public class ScopusDeleteEventBody implements EventBody {

    public static final String TOPIC = "FetchDoi.Delete.Scopus";
    public static final String SCOPUS_IDENTIFIER = "scopusIdentifier";

    @JsonProperty(SCOPUS_IDENTIFIER)
    private final String scopusIdentifier;

    @JsonCreator
    public ScopusDeleteEventBody(@JsonProperty(SCOPUS_IDENTIFIER) String scopusIdentifier) {
        this.scopusIdentifier = scopusIdentifier;
    }

    public static ScopusDeleteEventBody fromJson(String json) {
        return attempt(() -> Json.readValue(json, ScopusDeleteEventBody.class)).orElseThrow();
    }

    public String toJson() {
        return attempt(() -> Json.writeValueAsString(this)).orElseThrow();
    }

    @JacocoGenerated
    public String getScopusIdentifier() {
        return scopusIdentifier;
    }

    @Override
    public String getTopic() {
        return TOPIC;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTopic(), getScopusIdentifier());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScopusDeleteEventBody)) {
            return false;
        }
        ScopusDeleteEventBody that = (ScopusDeleteEventBody) o;
        return Objects.equals(getTopic(), that.getTopic()) && Objects.equals(getScopusIdentifier(),
                that.getScopusIdentifier());
    }
}
