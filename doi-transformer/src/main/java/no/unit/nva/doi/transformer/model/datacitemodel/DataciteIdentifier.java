package no.unit.nva.doi.transformer.model.datacitemodel;

import nva.commons.core.JacocoGenerated;

public class DataciteIdentifier {

    private String identifier;
    private String identifierType;

    @JacocoGenerated
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @JacocoGenerated
    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }
}
