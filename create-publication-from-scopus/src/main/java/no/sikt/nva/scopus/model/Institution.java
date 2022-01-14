package no.sikt.nva.scopus.model;

import no.scopus.generated.AffiliationTp;
import no.scopus.generated.OrganizationTp;
import nva.commons.core.JacocoGenerated;

import java.util.List;

public class Institution {
    private String externalId;
    private String unitId;
    private String unitName;
    private String countryCode;
    private String institutionName;
    private String city;
    private String cityGroup;
    private Integer cristinInstitutionNr;


    public Institution(AffiliationTp affiliationTp) {
        if (affiliationTp == null) return;
        externalId = affiliationTp.getAfid();
        unitId = affiliationTp.getDptid();
        countryCode = affiliationTp.getCountry();
        unitName = buildUnitName(affiliationTp.getOrganization());
        institutionName = fetchInstitutionName(affiliationTp.getOrganization());
        city = affiliationTp.getCity();
        cityGroup = affiliationTp.getCityGroup();
    }

    @JacocoGenerated
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @JacocoGenerated
    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    @JacocoGenerated
    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    @JacocoGenerated
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @JacocoGenerated
    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    @JacocoGenerated
    public void setCity(String city) {
        this.city = city;
    }

    @JacocoGenerated
    public void setCityGroup(String cityGroup) {
        this.cityGroup = cityGroup;
    }

    @JacocoGenerated
    public void setCristinInstitutionNr(Integer cristinInstitutionNr) {
        this.cristinInstitutionNr = cristinInstitutionNr;
    }


    public String getExternalId() {
        return externalId;
    }


    public String getUnitId() {
        return unitId;
    }

    @JacocoGenerated
    public String getUnitName() {
        return unitName;
    }

    @JacocoGenerated
    public String getCountryCode() {
        return countryCode;
    }


    public String getInstitutionName() {
        return institutionName;
    }

    @JacocoGenerated
    public String getCity() {
        return city;
    }

    @JacocoGenerated
    public String getCityGroup() {
        return cityGroup;
    }

    @JacocoGenerated
    public Integer getCristinInstitutionNr() {
        return cristinInstitutionNr;
    }


    private String buildUnitName(List<OrganizationTp> organizationTps) {
        if (organizationTps == null || organizationTps.isEmpty()) return null;
        StringBuilder organization = new StringBuilder();
        int orgCounter = 1;
        int orgElements = organizationTps.size();
        for (OrganizationTp oneOrg : organizationTps) {
            oneOrg.getContent().stream().forEach(organization::append);
            if (orgCounter < orgElements) {
                organization.append(";");
            }
            orgCounter++;
        }
        return organization.toString();
    }


    private String fetchInstitutionName(List<OrganizationTp> organizationTps) {
        if (organizationTps == null || organizationTps.isEmpty()) return null;
        StringBuilder name = new StringBuilder();
        organizationTps.get(organizationTps.size()-1).getContent().stream().forEach(name::append);
        return name.toString();
    }

    @JacocoGenerated
    @Override public String toString() {
        return "Institution{" +
                "externalId='" + externalId + '\'' +
                ", unitId='" + unitId + '\'' +
                ", unitName='" + unitName + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", institutionName='" + institutionName + '\'' +
                ", city='" + city + '\'' +
                ", cityGroup='" + cityGroup + '\'' +
                ", cristinInstitutionNr=" + cristinInstitutionNr +
                '}';
    }
}
