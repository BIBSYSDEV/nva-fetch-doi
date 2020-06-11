package no.unit.nva.doi.transformer.model.datacitemodel;

import java.util.List;
import nva.commons.utils.JacocoGenerated;

public class DataciteContributor {
    private String nameType;
    private String name;
    private String givenName;
    private String familyName;
    private List<DataciteAffiliation> affiliation;

    @JacocoGenerated
    public DataciteContributor() {
    }

    @JacocoGenerated
    private DataciteContributor(Builder builder) {
        nameType = builder.nameType;
        name = builder.name;
        givenName = builder.givenName;
        familyName = builder.familyName;
        affiliation = builder.affiliation;
    }


    public static final class Builder {
        private String nameType;
        private String name;
        private String givenName;
        private String familyName;
        private List<DataciteAffiliation> affiliation;

        @JacocoGenerated
        public Builder() {
        }

        @JacocoGenerated
        public Builder withNameType(String nameType) {
            this.nameType = nameType;
            return this;
        }

        @JacocoGenerated
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        @JacocoGenerated
        public Builder withGivenName(String givenName) {
            this.givenName = givenName;
            return this;
        }

        @JacocoGenerated
        public Builder withFamilyName(String familyName) {
            this.familyName = familyName;
            return this;
        }

        @JacocoGenerated
        public Builder withAffiliation(List<DataciteAffiliation> affiliation) {
            this.affiliation = affiliation;
            return this;
        }

        @JacocoGenerated
        public DataciteContributor build() {
            return new DataciteContributor(this);
        }
    }
}
