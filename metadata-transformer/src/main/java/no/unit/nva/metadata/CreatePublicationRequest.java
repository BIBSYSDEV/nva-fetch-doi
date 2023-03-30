package no.unit.nva.metadata;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.unit.nva.WithAssociatedArtifact;
import no.unit.nva.WithContext;
import no.unit.nva.WithMetadata;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.funding.Funding;
import nva.commons.core.JacocoGenerated;

public class CreatePublicationRequest implements WithMetadata, WithAssociatedArtifact, WithContext, JsonSerializable {

    private EntityDescription entityDescription;
    private AssociatedArtifactList associatedArtifacts;
    @JsonProperty("@context")
    private JsonNode context;
    private List<ResearchProject> projects;
    private List<URI> subjects;
    private Set<AdditionalIdentifier> additionalIdentifiers;
    private List<Funding> fundings;
    private String rightsHolder;

    @JacocoGenerated
    public static CreatePublicationRequest fromJson(String json) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, CreatePublicationRequest.class)).orElseThrow();
    }

    @JacocoGenerated
    @Override
    public EntityDescription getEntityDescription() {
        return entityDescription;
    }

    @JacocoGenerated
    @Override
    public void setEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
    }

    @JacocoGenerated
    @Override
    public List<ResearchProject> getProjects() {
        return projects;
    }

    @Override
    @JacocoGenerated
    public void setProjects(List<ResearchProject> projects) {
        this.projects = projects;
    }

    @Override
    @JacocoGenerated
    public List<URI> getSubjects() {
        return subjects;
    }

    @Override
    @JacocoGenerated
    public void setSubjects(List<URI> subjects) {
        this.subjects = subjects;
    }

    @Override
    public List<Funding> getFundings() {
        return fundings;
    }

    @Override
    public void setFundings(List<Funding> fundings) {
        this.fundings = fundings;
    }

    @JacocoGenerated
    @Override
    public String getRightsHolder() {
        return rightsHolder;
    }

    @JacocoGenerated
    @Override
    public void setRightsHolder(String rightsHolder) {
        this.rightsHolder = rightsHolder;
    }

    @Override
    @JacocoGenerated
    public AssociatedArtifactList getAssociatedArtifacts() {
        return associatedArtifacts;
    }

    @Override
    @JacocoGenerated
    public void setAssociatedArtifacts(AssociatedArtifactList associatedArtifacts) {
        this.associatedArtifacts = associatedArtifacts;
    }

    @Override
    @JacocoGenerated
    public JsonNode getContext() {
        return context;
    }

    @JacocoGenerated
    @Override
    public void setContext(JsonNode context) {
        this.context = context;
    }

    @JacocoGenerated
    public Set<AdditionalIdentifier> getAdditionalIdentifiers() {
        return additionalIdentifiers;
    }

    @JacocoGenerated
    public void setAdditionalIdentifiers(Set<AdditionalIdentifier> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CreatePublicationRequest that = (CreatePublicationRequest) o;
        return Objects.equals(getEntityDescription(), that.getEntityDescription())
               && Objects.equals(getAssociatedArtifacts(), that.getAssociatedArtifacts())
               && Objects.equals(getContext(), that.getContext())
               && Objects.equals(getProjects(), that.getProjects())
               && Objects.equals(getSubjects(), that.getSubjects())
               && Objects.equals(getAdditionalIdentifiers(), that.getAdditionalIdentifiers())
               && Objects.equals(getFundings(), that.getFundings())
               && Objects.equals(getRightsHolder(), that.getRightsHolder());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getEntityDescription(),
                            getAssociatedArtifacts(),
                            getContext(),
                            getProjects(),
                            getSubjects(),
                            getAdditionalIdentifiers(),
                            getFundings(),
                            getRightsHolder());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return "CreatePublicationRequest{" +
               "entityDescription=" + entityDescription +
               ", associatedArtifacts=" + associatedArtifacts +
               ", context=" + context +
               ", projects=" + projects +
               ", subjects=" + subjects +
               ", additionalIdentifiers=" + additionalIdentifiers +
               ", fundings=" + fundings +
               ", rightsHolder='" + rightsHolder + '\'' +
               '}';
    }
}
