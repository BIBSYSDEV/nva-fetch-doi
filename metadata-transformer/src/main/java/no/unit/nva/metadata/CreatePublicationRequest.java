package no.unit.nva.metadata;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.WithContext;
import no.unit.nva.WithFile;
import no.unit.nva.WithMetadata;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.file.model.FileSet;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.ResearchProject;
import nva.commons.core.JacocoGenerated;

public class CreatePublicationRequest implements WithMetadata, WithFile, WithContext, JsonSerializable {

    private EntityDescription entityDescription;
    private FileSet fileSet;
    @JsonProperty("@context")
    private JsonNode context;
    private List<ResearchProject> projects;
    private List<URI> subjects;
    private Set<AdditionalIdentifier> additionalIdentifiers;
    private String authorKeywordsXmlFormat;

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
    @JacocoGenerated
    public FileSet getFileSet() {
        return fileSet;
    }

    @Override
    @JacocoGenerated
    public void setFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
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
    public String getAuthorKeywordsXmlFormat() {
        return authorKeywordsXmlFormat;
    }

    @JacocoGenerated
    public void setAuthorKeywordsXmlFormat(String authorKeywordsXmlFormat) {
        this.authorKeywordsXmlFormat = authorKeywordsXmlFormat;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getEntityDescription(), getFileSet(), getContext(), getProjects(),
                            getSubjects(), getAdditionalIdentifiers(), getAuthorKeywordsXmlFormat());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreatePublicationRequest)) {
            return false;
        }
        CreatePublicationRequest that = (CreatePublicationRequest) o;
        return Objects.equals(getEntityDescription(), that.getEntityDescription())
               && Objects.equals(getFileSet(), that.getFileSet())
               && Objects.equals(getContext(), that.getContext())
               && Objects.equals(getProjects(), that.getProjects())
               && Objects.equals(getSubjects(), that.getSubjects())
               && Objects.equals(getAdditionalIdentifiers(), that.getAdditionalIdentifiers())
               && Objects.equals(getAuthorKeywordsXmlFormat(), that.getAuthorKeywordsXmlFormat());
    }
}
