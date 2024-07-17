package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Publication")
public class CreatePublicationRequest {

    private EntityDescription entityDescription;
    private List<AssociatedArtifact> associatedArtifacts;

    public CreatePublicationRequest() {
    }

    public CreatePublicationRequest(EntityDescription entityDescription, List<AssociatedArtifact> associatedArtifacts) {
        this.entityDescription = entityDescription;
        this.associatedArtifacts = associatedArtifacts;
    }

    public EntityDescription getEntityDescription() {
        return entityDescription;
    }

    public void setEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
    }

    public List<AssociatedArtifact> getAssociatedArtifacts() {
        return associatedArtifacts;
    }

    public void setAssociatedArtifacts(List<AssociatedArtifact> associatedArtifacts) {
        this.associatedArtifacts = associatedArtifacts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CreatePublicationRequest that = (CreatePublicationRequest) o;
        return Objects.equals(getEntityDescription(), that.getEntityDescription()) && Objects.equals(
            getAssociatedArtifacts(), that.getAssociatedArtifacts());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEntityDescription(), getAssociatedArtifacts());
    }

    @Override
    public String toString() {
        return "CreatePublicationRequest{" +
               "entityDescription=" + entityDescription +
               ", associatedArtifacts=" + associatedArtifacts +
               '}';
    }

    @JacocoGenerated
    public static class Builder {
        private EntityDescription entityDescription;
        private List<AssociatedArtifact> associatedArtifacts = Collections.emptyList();

        public Builder withEntityDescription(EntityDescription entityDescription) {
            this.entityDescription = entityDescription;
            return this;
        }

        public Builder withAssociatedArtifacts(List<AssociatedArtifact> associatedArtifacts) {
            this.associatedArtifacts = associatedArtifacts;
            return this;
        }

        public CreatePublicationRequest build() {
            return new CreatePublicationRequest(entityDescription, associatedArtifacts);
        }

    }
}
