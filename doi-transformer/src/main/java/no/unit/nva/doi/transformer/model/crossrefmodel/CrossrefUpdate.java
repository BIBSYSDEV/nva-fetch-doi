package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class CrossrefUpdate {

    @JsonProperty("updated")
    private CrossrefDate updated;
    @JsonProperty("DOI")
    private String doi;
    @JsonProperty("type")
    private String type;
    @JsonProperty("label")
    private String label;

    public CrossrefDate getUpdated() {
        return updated;
    }

    public void setUpdated(CrossrefDate updated) {
        this.updated = updated;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
