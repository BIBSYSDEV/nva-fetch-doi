package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Reference {
    private PublicationContext publicationContext;
    private PublicationInstance publicationInstance;
    private URI doi;

    public Reference() {
    }

    public Reference(PublicationContext publicationContext, PublicationInstance publicationInstance, URI doi) {
        this.publicationContext = publicationContext;
        this.publicationInstance = publicationInstance;
        this.doi = doi;
    }

    public PublicationContext getPublicationContext() {
        return publicationContext;
    }

    public void setPublicationContext(PublicationContext publicationContext) {
        this.publicationContext = publicationContext;
    }

    public PublicationInstance getPublicationInstance() {
        return publicationInstance;
    }

    public void setPublicationInstance(PublicationInstance publicationInstance) {
        this.publicationInstance = publicationInstance;
    }

    public URI getDoi() {
        return doi;
    }

    public void setDoi(URI doi) {
        this.doi = doi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Reference reference = (Reference) o;
        return Objects.equals(getPublicationContext(), reference.getPublicationContext())
               && Objects.equals(getPublicationInstance(), reference.getPublicationInstance())
               && Objects.equals(getDoi(), reference.getDoi());
    }

    @Override
    public String toString() {
        return "Reference{" +
               "publicationContext=" + publicationContext +
               ", publicationInstance=" + publicationInstance +
               ", doi=" + doi +
               '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPublicationContext(), getPublicationInstance(), getDoi());
    }

    @JacocoGenerated
    public static class Builder {
        private PublicationContext publicationContext;
        private PublicationInstance publicationInstance;
        URI doi;

        public Builder withPublicationContext(PublicationContext publicationContext) {
            this.publicationContext = publicationContext;
            return this;
        }

        public Builder withPublicationInstance(PublicationInstance publicationInstance) {
            this.publicationInstance = publicationInstance;
            return this;
        }

        public Builder withDoi(URI doi) {
            this.doi = doi;
            return this;
        }

        public Reference build() {
            return new Reference(publicationContext, publicationInstance, doi);
        }
    }
}
