package no.unit.nva.doi.transformer.model.internal.external;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.doi.transformer.utils.BibTexType;
import no.unit.nva.doi.transformer.utils.CiteProcType;
import no.unit.nva.doi.transformer.utils.RisType;
import no.unit.nva.doi.transformer.utils.SchemaOrgType;

public class DataciteTypes {

    private RisType ris;
    private BibTexType bibtex;
    private CiteProcType citeproc;
    private SchemaOrgType schemaOrg;
    private String resourceType;
    private String resourceTypeGeneral;

    /**
     * Constructor for Datacite types structures.
     *
     * @param ris the RIS type.
     * @param bibtex the BibTeX type.
     * @param citeproc the CiteProc type.
     * @param schemaOrg the Schema.org type.
     * @param resourceType the Datacite resource type.
     * @param resourceTypeGeneral the Datacite resource type general.
     */
    @JsonCreator
    public DataciteTypes(
                         @JsonProperty("ris") String ris,
                         @JsonProperty("bibtex") String bibtex,
                         @JsonProperty("citeprox") String citeproc,
                         @JsonProperty("schemaOrg") String schemaOrg,
                         @JsonProperty("resourceType") String resourceType,
                         @JsonProperty("resourceTypeGeneral") String resourceTypeGeneral) {
        setRis(ris);
        setBibtex(bibtex);
        setCiteproc(citeproc);
        setSchemaOrg(schemaOrg);
        this.resourceType = resourceType;
        this.resourceTypeGeneral = resourceTypeGeneral;
    }

    private DataciteTypes(Builder builder) {
        this(builder.ris,
                builder.bibtex,
                builder.citeproc,
                builder.schemaOrg,
                builder.schemaOrg,
                builder.resourceTypeGeneral
        );
    }

    public RisType getRis() {
        return ris;
    }

    private void setRis(String ris) {
        this.ris = RisType.getByType(ris);
    }

    public BibTexType getBibtex() {
        return bibtex;
    }

    private void setBibtex(String bibtex) {
        this.bibtex = BibTexType.getByType(bibtex);
    }

    public CiteProcType getCiteproc() {
        return citeproc;
    }

    private void setCiteproc(String citeproc) {
        this.citeproc = CiteProcType.getByType(citeproc);
    }

    public SchemaOrgType getSchemaOrg() {
        return schemaOrg;
    }

    private void setSchemaOrg(String schemaOrg) {
        this.schemaOrg = SchemaOrgType.getByType(schemaOrg);
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceTypeGeneral() {
        return resourceTypeGeneral;
    }

    public void setResourceTypeGeneral(String resourceTypeGeneral) {
        this.resourceTypeGeneral = resourceTypeGeneral;
    }

    public static final class Builder {
        private String ris;
        private String bibtex;
        private String citeproc;
        private String schemaOrg;
        private String resourceType;
        private String resourceTypeGeneral;

        public Builder() {
        }

        public Builder withRis(String ris) {
            this.ris = ris;
            return this;
        }

        public Builder withBibtex(String bibtex) {
            this.bibtex = bibtex;
            return this;
        }

        public Builder withCiteproc(String citeproc) {
            this.citeproc = citeproc;
            return this;
        }

        public Builder withSchemaOrg(String schemaOrg) {
            this.schemaOrg = schemaOrg;
            return this;
        }

        public Builder withResourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder withResourceTypeGeneral(String resourceTypeGeneral) {
            this.resourceTypeGeneral = resourceTypeGeneral;
            return this;
        }

        public DataciteTypes build() {
            return new DataciteTypes(this);
        }
    }
}
