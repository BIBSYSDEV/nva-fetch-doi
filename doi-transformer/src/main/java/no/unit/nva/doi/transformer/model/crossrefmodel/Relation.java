package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import nva.commons.utils.JacocoGenerated;

public class Relation {

    @JsonProperty("cites")
    private List<String> cites;

    @JacocoGenerated
    public List<String> getCites() {
        return cites;
    }

    public void setCites(List<String> cites) {
        this.cites = cites;
    }
}
