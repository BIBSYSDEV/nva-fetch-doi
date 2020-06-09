package no.unit.nva.doi.transformer.model.internal.external;

public class DataciteRelatedIdentifier {

    private String relationType;
    private String relatedIdentifier;
    private String resourceTypeGeneral;
    private String relatedIdentifierType;

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public String getRelatedIdentifier() {
        return relatedIdentifier;
    }

    public void setRelatedIdentifier(String relatedIdentifier) {
        this.relatedIdentifier = relatedIdentifier;
    }

    public String getResourceTypeGeneral() {
        return resourceTypeGeneral;
    }

    public void setResourceTypeGeneral(String resourceTypeGeneral) {
        this.resourceTypeGeneral = resourceTypeGeneral;
    }

    public String getRelatedIdentifierType() {
        return relatedIdentifierType;
    }

    public void setRelatedIdentifierType(String relatedIdentifierType) {
        this.relatedIdentifierType = relatedIdentifierType;
    }
}
