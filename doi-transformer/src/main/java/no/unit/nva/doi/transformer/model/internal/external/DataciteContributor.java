package no.unit.nva.doi.transformer.model.internal.external;

import java.util.List;

public class DataciteContributor {
    private String nameType;
    private String name;
    private String givenName;
    private String familyName;
    private List<DataciteAffiliation> affiliation;

    public DataciteContributor() {
    }

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

        public Builder() {
        }

        public Builder withNameType(String nameType) {
            this.nameType = nameType;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withGivenName(String givenName) {
            this.givenName = givenName;
            return this;
        }

        public Builder withFamilyName(String familyName) {
            this.familyName = familyName;
            return this;
        }

        public Builder withAffiliation(List<DataciteAffiliation> affiliation) {
            this.affiliation = affiliation;
            return this;
        }

        public DataciteContributor build() {
            return new DataciteContributor(this);
        }
    }
}
