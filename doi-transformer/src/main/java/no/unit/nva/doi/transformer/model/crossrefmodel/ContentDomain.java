package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ContentDomain {

    @JsonProperty("domain")
    private List<String> domain;
    @JsonProperty("crossmark-restriction")
    private boolean crossmarkRestriction;

    public List<String> getDomain() {
        return domain;
    }

    public void setDomain(List<String> domain) {
        this.domain = domain;
    }

    public boolean isCrossmarkRestriction() {
        return crossmarkRestriction;
    }

    public void setCrossmarkRestriction(boolean crossmarkRestriction) {
        this.crossmarkRestriction = crossmarkRestriction;
    }
}
