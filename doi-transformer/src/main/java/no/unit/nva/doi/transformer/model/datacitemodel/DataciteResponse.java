package no.unit.nva.doi.transformer.model.datacitemodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URL;
import java.util.List;
import nva.commons.core.JacocoGenerated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings({"PMD.TooManyFields", "PMD.ExcessivePublicCount"})
public class DataciteResponse {

    private URL id;
    private String doi;
    private URL url;
    private DataciteTypes types;
    private List<DataciteCreator> creators;
    private List<DataciteTitle> titles;
    private Publisher publisher;
    private DataciteContainer container;
    private List<DataciteContributor> contributors;
    private List<DataciteDate> dates;
    private Integer publicationYear;
    private List<DataciteIdentifier> identifiers;
    private List<DataciteRelatedIdentifier> relatedIdentifiers;
    private String schemaVersion;
    private String providerId;
    private String clientId;
    private String agency;
    private String state;
    private List<DataciteRights> rightsList;

    public DataciteResponse() {
    }

    private DataciteResponse(Builder builder) {
        setId(builder.id);
        setDoi(builder.doi);
        setUrl(builder.url);
        setTypes(builder.types);
        setCreators(builder.creators);
        setTitles(builder.titles);
        setPublisher(builder.publisher);
        setContainer(builder.container);
        setContributors(builder.contributors);
        setDates(builder.dates);
        setPublicationYear(builder.publicationYear);
        setIdentifiers(builder.identifiers);
        setRelatedIdentifiers(builder.relatedIdentifiers);
        setSchemaVersion(builder.schemaVersion);
        setProviderId(builder.providerId);
        setClientId(builder.clientId);
        setAgency(builder.agency);
        setState(builder.state);
        setRightsList(builder.rightsList);
    }

    @JacocoGenerated
    public URL getId() {
        return id;
    }

    @JacocoGenerated
    public void setId(URL id) {
        this.id = id;
    }

    @JacocoGenerated
    public String getDoi() {
        return doi;
    }

    @JacocoGenerated
    public void setDoi(String doi) {
        this.doi = doi;
    }

    @JacocoGenerated
    public URL getUrl() {
        return url;
    }

    @JacocoGenerated
    public void setUrl(URL url) {
        this.url = url;
    }

    @JacocoGenerated
    public DataciteTypes getTypes() {
        return types;
    }

    @JacocoGenerated
    public void setTypes(DataciteTypes types) {
        this.types = types;
    }

    @JacocoGenerated
    public List<DataciteCreator> getCreators() {
        return creators;
    }

    @JacocoGenerated
    public void setCreators(List<DataciteCreator> creators) {
        this.creators = creators;
    }

    @JacocoGenerated
    public List<DataciteTitle> getTitles() {
        return titles;
    }

    @JacocoGenerated
    public void setTitles(List<DataciteTitle> titles) {
        this.titles = titles;
    }

    @JacocoGenerated
    public String getPublisher() {
        return publisher.value();
    }

    @JacocoGenerated
    public void setPublisher(Object publisher) {
        this.publisher = Publisher.fromJson(publisher);
    }

    @JacocoGenerated
    public DataciteContainer getContainer() {
        return container;
    }

    @JacocoGenerated
    public void setContainer(DataciteContainer container) {
        this.container = container;
    }

    @JacocoGenerated
    public List<DataciteContributor> getContributors() {
        return contributors;
    }

    @JacocoGenerated
    public void setContributors(List<DataciteContributor> contributors) {
        this.contributors = contributors;
    }

    @JacocoGenerated
    public List<DataciteDate> getDates() {
        return dates;
    }

    @JacocoGenerated
    public void setDates(List<DataciteDate> dates) {
        this.dates = dates;
    }

    @JacocoGenerated
    public Integer getPublicationYear() {
        return publicationYear;
    }

    @JacocoGenerated
    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }

    @JacocoGenerated
    public List<DataciteIdentifier> getIdentifiers() {
        return identifiers;
    }

    @JacocoGenerated
    public void setIdentifiers(List<DataciteIdentifier> identifiers) {
        this.identifiers = identifiers;
    }

    @JacocoGenerated
    public List<DataciteRelatedIdentifier> getRelatedIdentifiers() {
        return relatedIdentifiers;
    }

    @JacocoGenerated
    public void setRelatedIdentifiers(List<DataciteRelatedIdentifier> relatedIdentifiers) {
        this.relatedIdentifiers = relatedIdentifiers;
    }

    @JacocoGenerated
    public String getSchemaVersion() {
        return schemaVersion;
    }

    @JacocoGenerated
    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    @JacocoGenerated
    public String getProviderId() {
        return providerId;
    }

    @JacocoGenerated
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    @JacocoGenerated
    public String getClientId() {
        return clientId;
    }

    @JacocoGenerated
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JacocoGenerated
    public String getAgency() {
        return agency;
    }

    @JacocoGenerated
    public void setAgency(String agency) {
        this.agency = agency;
    }

    @JacocoGenerated
    public String getState() {
        return state;
    }

    @JacocoGenerated
    public void setState(String state) {
        this.state = state;
    }

    @JacocoGenerated
    public List<DataciteRights> getRightsList() {
        return rightsList;
    }

    @JacocoGenerated
    public void setRightsList(List<DataciteRights> rightsList) {
        this.rightsList = rightsList;
    }

    public static final class Builder {

        public List<DataciteRights> rightsList;
        private URL id;
        private String doi;
        private URL url;
        private DataciteTypes types;
        private List<DataciteCreator> creators;
        private List<DataciteTitle> titles;
        private String publisher;
        private DataciteContainer container;
        private List<DataciteContributor> contributors;
        private List<DataciteDate> dates;
        private Integer publicationYear;
        private List<DataciteIdentifier> identifiers;
        private List<DataciteRelatedIdentifier> relatedIdentifiers;
        private String schemaVersion;
        private String providerId;
        private String clientId;
        private String agency;
        private String state;

        public Builder() {
        }

        @JacocoGenerated
        public Builder withId(URL id) {
            this.id = id;
            return this;
        }

        @JacocoGenerated
        public Builder withDoi(String doi) {
            this.doi = doi;
            return this;
        }

        @JacocoGenerated
        public Builder withUrl(URL url) {
            this.url = url;
            return this;
        }

        @JacocoGenerated
        public Builder withTypes(DataciteTypes types) {
            this.types = types;
            return this;
        }

        @JacocoGenerated
        public Builder withCreators(List<DataciteCreator> creators) {
            this.creators = creators;
            return this;
        }

        @JacocoGenerated
        public Builder withTitles(List<DataciteTitle> titles) {
            this.titles = titles;
            return this;
        }

        @JacocoGenerated
        public Builder withPublisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        @JacocoGenerated
        public Builder withContainer(DataciteContainer container) {
            this.container = container;
            return this;
        }

        @JacocoGenerated
        public Builder withContributors(List<DataciteContributor> contributors) {
            this.contributors = contributors;
            return this;
        }

        @JacocoGenerated
        public Builder withDates(List<DataciteDate> dates) {
            this.dates = dates;
            return this;
        }

        @JacocoGenerated
        public Builder withPublicationYear(Integer publicationYear) {
            this.publicationYear = publicationYear;
            return this;
        }

        @JacocoGenerated
        public Builder withIdentifiers(List<DataciteIdentifier> identifiers) {
            this.identifiers = identifiers;
            return this;
        }

        @JacocoGenerated
        public Builder withRelatedIdentifiers(List<DataciteRelatedIdentifier> relatedIdentifiers) {
            this.relatedIdentifiers = relatedIdentifiers;
            return this;
        }

        @JacocoGenerated
        public Builder withSchemaVersion(String schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        @JacocoGenerated
        public Builder withProviderId(String providerId) {
            this.providerId = providerId;
            return this;
        }

        @JacocoGenerated
        public Builder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        @JacocoGenerated
        public Builder withAgency(String agency) {
            this.agency = agency;
            return this;
        }

        @JacocoGenerated
        public Builder withState(String state) {
            this.state = state;
            return this;
        }

        @JacocoGenerated
        public Builder withRightsList(List<DataciteRights> rights) {
            this.rightsList = rights;
            return this;
        }

        public DataciteResponse build() {
            return new DataciteResponse(this);
        }
    }
}
