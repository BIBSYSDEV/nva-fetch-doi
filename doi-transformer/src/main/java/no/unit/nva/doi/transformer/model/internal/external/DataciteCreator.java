package no.unit.nva.doi.transformer.model.internal.external;

import java.util.List;

public class DataciteCreator {

    private String name;
    private String nameType;
    private String givenName;
    private String familyName;
    private List<DataciteAffiliation> affiliation;

    public DataciteCreator() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameType() {
        return nameType;
    }

    public void setNameType(String nameType) {
        this.nameType = nameType;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public List<DataciteAffiliation> getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(List<DataciteAffiliation> affiliation) {
        this.affiliation = affiliation;
    }
}
