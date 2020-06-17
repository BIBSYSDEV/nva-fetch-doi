package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import nva.commons.utils.JacocoGenerated;

public class ContentDomain {

    @JsonProperty("domain")
    private List<String> domain;
    @JsonProperty("crossmark-restriction")
    private boolean crossmarkRestriction;

    @JacocoGenerated
    public List<String> getDomain() {
        return domain;
    }

    public void setDomain(List<String> domain) {
        this.domain = domain;
    }

    @JacocoGenerated
    public boolean isCrossmarkRestriction() {
        return crossmarkRestriction;
    }

    public void setCrossmarkRestriction(boolean crossmarkRestriction) {
        this.crossmarkRestriction = crossmarkRestriction;
    }
}
