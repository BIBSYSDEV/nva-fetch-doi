package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class CrossrefRelation {

    @JsonProperty("id-type")
    private String idType;
    @JsonProperty("id")
    private String id;
    @JsonProperty("asserted-by")
    private String assertedBy;

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAssertedBy() {
        return assertedBy;
    }

    public void setAssertedBy(String assertedBy) {
        this.assertedBy = assertedBy;
    }
}
