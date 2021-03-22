package no.unit.nva.doi.fetch.model;

import no.unit.nva.identifiers.SortableIdentifier;

public class Summary {

    private SortableIdentifier identifier;
    private String title;
    private String creatorName;
    private PublicationDate date;

    public Summary() {

    }

    private Summary(Builder builder) {
        setIdentifier(builder.identifier);
        setTitle(builder.title);
        setCreatorName(builder.creatorName);
        setDate(builder.date);
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public PublicationDate getDate() {
        return date;
    }

    public void setDate(PublicationDate date) {
        this.date = date;
    }

    public static final class Builder {

        private SortableIdentifier identifier;
        private String title;
        private String creatorName;
        private PublicationDate date;

        public Builder() {
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withCreatorName(String creatorName) {
            this.creatorName = creatorName;
            return this;
        }

        public Builder withDate(PublicationDate date) {
            this.date = date;
            return this;
        }

        public Summary build() {
            return new Summary(this);
        }
    }
}
