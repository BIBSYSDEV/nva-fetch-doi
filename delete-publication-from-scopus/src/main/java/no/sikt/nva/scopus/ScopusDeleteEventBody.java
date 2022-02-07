package no.sikt.nva.scopus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import no.unit.nva.events.models.EventBody;
import nva.commons.core.JacocoGenerated;

public class ScopusDeleteEventBody implements EventBody {

    public static final String TOPIC = "delete-scopus-identifier";
    public static final String SCOPUS_IDENTIFIER = "scopusIdentifier";

    @JsonProperty(SCOPUS_IDENTIFIER)
    private final String scopusIdentifier;

    @JsonCreator
    public ScopusDeleteEventBody(@JsonProperty(SCOPUS_IDENTIFIER) String scopusIdentifier) {
        this.scopusIdentifier = scopusIdentifier;
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
