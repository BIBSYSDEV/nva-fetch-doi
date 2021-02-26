package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class CrossrefClinicalTrialNumber {

    @JsonProperty("clinical-trial-number")
    private String clinicalTrialNumber;
    @JsonProperty("registry")
    private String registry;
    @JsonProperty("type")
    private String type;


    public String getClinicalTrialNumber() {
        return clinicalTrialNumber;
    }

    public void setClinicalTrialNumber(String clinicalTrialNumber) {
        this.clinicalTrialNumber = clinicalTrialNumber;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
