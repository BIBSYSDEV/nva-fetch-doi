package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import nva.commons.utils.JacocoGenerated;

public class CrossrefAuthor {

    @JsonProperty("given")
    private String givenName;
    @JsonProperty("family")
    private String familyName;
    @JsonProperty("sequence")
    private String sequence;
    @JsonProperty("affiliation")
    private List<CrossrefAffiliation> affiliation;

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    @JacocoGenerated
    public List<CrossrefAffiliation> getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(List<CrossrefAffiliation> affiliation) {
        this.affiliation = affiliation;
    }

    public static final class Builder {

        private String givenName;
        private String familyName;
        private String sequence;
        private List<CrossrefAffiliation> affiliation;

        public Builder() {
        }

        public Builder withGivenName(String givenName) {
            this.givenName = givenName;
            return this;
        }

        public Builder withFamilyName(String familyName) {
            this.familyName = familyName;
            return this;
        }

        public Builder withSequence(String sequence) {
            this.sequence = sequence;
            return this;
        }

        @JacocoGenerated
        public Builder withAffiliation(List<CrossrefAffiliation> affiliation) {
            this.affiliation = affiliation;
            return this;
        }

        /**
         * Creates an CrossrefAuthor object.
         *
         * @return an CrossrefAuthor object.
         */
        public CrossrefAuthor build() {
            CrossrefAuthor author = new CrossrefAuthor();
            author.setGivenName(givenName);
            author.setFamilyName(familyName);
            author.setSequence(sequence);
            author.setAffiliation(affiliation);
            return author;
        }
    }
}
