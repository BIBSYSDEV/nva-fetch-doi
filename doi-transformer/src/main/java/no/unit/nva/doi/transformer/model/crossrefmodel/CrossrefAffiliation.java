package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.utils.JacocoGenerated;

public class CrossrefAffiliation {

    @JsonProperty("name")
    public String name;

    @JacocoGenerated
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
