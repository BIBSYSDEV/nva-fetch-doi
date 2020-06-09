package no.unit.nva.doi.transformer.model.internal.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URL;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings({"PMD.TooManyFields", "PMD.ExcessivePublicCount"})
public class DataciteResponse {
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

    public URL getId() {
        return id;
    }

    public void setId(URL id) {
        this.id = id;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public DataciteTypes getTypes() {
        return types;
    }

    public void setTypes(DataciteTypes types) {
        this.types = types;
    }

    public List<DataciteCreator> getCreators() {
        return creators;
    }

    public void setCreators(List<DataciteCreator> creators) {
        this.creators = creators;
    }

    public List<DataciteTitle> getTitles() {
        return titles;
    }

    public void setTitles(List<DataciteTitle> titles) {
        this.titles = titles;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public DataciteContainer getContainer() {
        return container;
    }

    public void setContainer(DataciteContainer container) {
        this.container = container;
    }

    public List<DataciteContributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<DataciteContributor> contributors) {
        this.contributors = contributors;
    }

    public List<DataciteDate> getDates() {
        return dates;
    }

    public void setDates(List<DataciteDate> dates) {
        this.dates = dates;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }

    public List<DataciteIdentifier> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<DataciteIdentifier> identifiers) {
        this.identifiers = identifiers;
    }

    public List<DataciteRelatedIdentifier> getRelatedIdentifiers() {
        return relatedIdentifiers;
    }

    public void setRelatedIdentifiers(List<DataciteRelatedIdentifier> relatedIdentifiers) {
        this.relatedIdentifiers = relatedIdentifiers;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<DataciteRights> getRightsList() {
        return rightsList;
    }

    public void setRightsList(List<DataciteRights> rightsList) {
        this.rightsList = rightsList;
    }

    public static final class Builder {
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
        public List<DataciteRights> rightsList;

        public Builder() {
        }

        public Builder withId(URL id) {
            this.id = id;
            return this;
        }

        public Builder withDoi(String doi) {
            this.doi = doi;
            return this;
        }

        public Builder withUrl(URL url) {
            this.url = url;
            return this;
        }

        public Builder withTypes(DataciteTypes types) {
            this.types = types;
            return this;
        }

        public Builder withCreators(List<DataciteCreator> creators) {
            this.creators = creators;
            return this;
        }

        public Builder withTitles(List<DataciteTitle> titles) {
            this.titles = titles;
            return this;
        }

        public Builder withPublisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder withContainer(DataciteContainer container) {
            this.container = container;
            return this;
        }

        public Builder withContributors(List<DataciteContributor> contributors) {
            this.contributors = contributors;
            return this;
        }

        public Builder withDates(List<DataciteDate> dates) {
            this.dates = dates;
            return this;
        }

        public Builder withPublicationYear(Integer publicationYear) {
            this.publicationYear = publicationYear;
            return this;
        }

        public Builder withIdentifiers(List<DataciteIdentifier> identifiers) {
            this.identifiers = identifiers;
            return this;
        }

        public Builder withRelatedIdentifiers(List<DataciteRelatedIdentifier> relatedIdentifiers) {
            this.relatedIdentifiers = relatedIdentifiers;
            return this;
        }

        public Builder withSchemaVersion(String schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder withProviderId(String providerId) {
            this.providerId = providerId;
            return this;
        }

        public Builder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder withAgency(String agency) {
            this.agency = agency;
            return this;
        }

        public Builder withState(String state) {
            this.state = state;
            return this;
        }

        public Builder withRightsList(List<DataciteRights> rights) {
            this.rightsList = rights;
            return this;
        }

        public DataciteResponse build() {
            return new DataciteResponse(this);
        }
    }
}
