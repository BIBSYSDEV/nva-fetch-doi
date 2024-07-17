package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonTypeInfo(use = Id.NAME, property = "type")
public class EntityDescription {
    private String mainAbstract;
    private Map<String, String> alternativeAbstracts;
    private String mainTitle;
    private Map<String, String> alternativeTitles;
    private String description;
    private URI language;
    private PublicationDate publicationDate;
    private Reference reference;
    private List<Contributor> contributors;
    private List<String> tags;
    private URI metadataSource;

    public EntityDescription() {
    }

    @JsonProperty("abstract")
    public String getMainAbstract() {
        return mainAbstract;
    }

    public void setMainAbstract(String mainAbstract) {
        this.mainAbstract = mainAbstract;
    }

    public Map<String, String> getAlternativeAbstracts() {
        return alternativeAbstracts;
    }

    public void setAlternativeAbstracts(Map<String, String> alternativeAbstracts) {
        this.alternativeAbstracts = alternativeAbstracts;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    public Map<String, String> getAlternativeTitles() {
        return alternativeTitles;
    }

    public void setAlternativeTitles(Map<String, String> alternativeTitles) {
        this.alternativeTitles = alternativeTitles;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URI getLanguage() {
        return language;
    }

    public void setLanguage(URI language) {
        this.language = language;
    }

    public PublicationDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(PublicationDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public Reference getReference() {
        return reference;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    public List<Contributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public URI getMetadataSource() {
        return metadataSource;
    }

    public void setMetadataSource(URI metadataSource) {
        this.metadataSource = metadataSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntityDescription that = (EntityDescription) o;
        return Objects.equals(getMainAbstract(), that.getMainAbstract())
               && Objects.equals(getAlternativeAbstracts(), that.getAlternativeAbstracts())
               && Objects.equals(getMainTitle(), that.getMainTitle())
               && Objects.equals(getAlternativeTitles(), that.getAlternativeTitles())
               && Objects.equals(getDescription(), that.getDescription())
               && Objects.equals(getLanguage(), that.getLanguage())
               && Objects.equals(getPublicationDate(), that.getPublicationDate())
               && Objects.equals(getReference(), that.getReference())
               && Objects.equals(getContributors(), that.getContributors())
               && Objects.equals(getTags(), that.getTags())
               && Objects.equals(getMetadataSource(), that.getMetadataSource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMainAbstract(), getAlternativeAbstracts(), getMainTitle(), getAlternativeTitles(),
                            getDescription(), getLanguage(), getPublicationDate(), getReference(), getContributors(),
                            getTags(), getMetadataSource());
    }

    @Override
    public String toString() {
        return "EntityDescription{" +
               "mainAbstract='" + mainAbstract + '\'' +
               ", alternativeAbstracts=" + alternativeAbstracts +
               ", mainTitle='" + mainTitle + '\'' +
               ", alternativeTitles=" + alternativeTitles +
               ", description='" + description + '\'' +
               ", language=" + language +
               ", publicationDate=" + publicationDate +
               ", reference=" + reference +
               ", contributors=" + contributors +
               ", tags=" + tags +
               ", metadataSource=" + metadataSource +
               '}';
    }

    @JacocoGenerated
    public static class Builder {

        private String mainAbstract;
        private Map<String, String> alternativeAbstracts = new HashMap<>();
        private String mainTitle;
        private Map<String, String> alternativeTitles = new HashMap<>();
        private String description;
        private URI language;
        private PublicationDate publicationDate;
        private Reference reference;
        private List<Contributor> contributors = new ArrayList<>();
        private List<String> tags = new ArrayList<>();
        private URI metadataSource;

        public Builder withMainAbstract(String mainAbstract) {
            this.mainAbstract = mainAbstract;
            return this;
        }

        public Builder withAlternativeAbstracts(Map<String, String> alternativeAbstracts) {
            this.alternativeAbstracts = alternativeAbstracts;
            return this;
        }

        public Builder withMainTitle(String mainTitle) {
            this.mainTitle = mainTitle;
            return this;
        }

        public Builder withAlternativeTitles(Map<String, String> alternativeTitles) {
            this.alternativeTitles = alternativeTitles;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withLanguage(URI language) {
            this.language = language;
            return this;
        }

        public Builder withPublicationDate(PublicationDate publicationDate) {
            this.publicationDate = publicationDate;
            return this;
        }

        public Builder withReference(Reference reference) {
            this.reference = reference;
            return this;
        }

        public Builder withContributors(List<Contributor> contributors) {
            this.contributors = contributors;
            return this;
        }

        public Builder withTags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder withMetadataSource(URI metadataSource) {
            this.metadataSource = metadataSource;
            return this;
        }

        public EntityDescription build() {
            var entityDescription = new EntityDescription();
            entityDescription.setMainAbstract(mainAbstract);
            entityDescription.setAlternativeAbstracts(alternativeAbstracts);
            entityDescription.setMainTitle(mainTitle);
            entityDescription.setAlternativeTitles(alternativeTitles);
            entityDescription.setDescription(description);
            entityDescription.setLanguage(language);
            entityDescription.setPublicationDate(publicationDate);
            entityDescription.setReference(reference);
            entityDescription.setContributors(contributors);
            entityDescription.setTags(tags);
            entityDescription.setMetadataSource(metadataSource);

            return entityDescription;
        }
    }
}
