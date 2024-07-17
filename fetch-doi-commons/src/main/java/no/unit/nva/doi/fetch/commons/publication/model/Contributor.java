package no.unit.nva.doi.fetch.commons.publication.model;

import java.util.List;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public record Contributor(Role role, Identity identity, List<Agent> affiliations, Integer sequence) {

    @JacocoGenerated
    public static class Builder {
        private Role role;
        private Identity identity;
        private List<Agent> affiliations;
        private Integer sequence;


        public Builder withRole(Role role) {
            this.role = role;
            return this;
        }

        public Builder withIdentity(Identity identity) {
            this.identity = identity;
            return this;
        }

        public Builder withAffiliations(List<Agent> affiliations) {
            this.affiliations = affiliations;
            return this;
        }

        public Builder withSequence(Integer sequence) {
            this.sequence = sequence;
            return this;
        }

        public Contributor build() {
            return new Contributor(role, identity, affiliations, sequence);
        }
    }
}
