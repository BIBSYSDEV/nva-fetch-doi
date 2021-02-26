package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.List;

@JacocoGenerated
public class CrossrefFunder {

    @JsonProperty("name")
    private String name;
    @JsonProperty("DOI")
    private String doi;
    @JsonProperty("award")
    private List<String> award;
    @JsonProperty("doi-asserted-by")
    private String doiAssertedBy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public List<String> getAward() {
        return award;
    }

    public void setAward(List<String> award) {
        this.award = award;
    }

    public String getDoiAssertedBy() {
        return doiAssertedBy;
    }

    public void setDoiAssertedBy(String doiAssertedBy) {
        this.doiAssertedBy = doiAssertedBy;
    }

    @JacocoGenerated
    public static final class Builder {

        private String name;
        private String doi;
        private List<String> award;
        private String doiAssertedBy;

        public Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDoi(String doi) {
            this.doi = doi;
            return this;
        }

        public Builder withAward(List<String> award) {
            this.award = award;
            return this;
        }

        public Builder withDoiAssertedBy(String doiAssertedBy) {
            this.doiAssertedBy = doiAssertedBy;
            return this;
        }

        /**
         * Creates an CrossrefAuthor object.
         *
         * @return an CrossrefAuthor object.
         */
        public CrossrefFunder build() {
            CrossrefFunder funder = new CrossrefFunder();
            funder.setName(name);
            funder.setDoi(doi);
            funder.setAward(award);
            funder.setDoiAssertedBy(doiAssertedBy);
            return funder;
        }
    }
}
