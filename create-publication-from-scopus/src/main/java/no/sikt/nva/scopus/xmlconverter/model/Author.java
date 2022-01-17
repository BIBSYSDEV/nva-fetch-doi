package no.sikt.nva.scopus.xmlconverter.model;

import nva.commons.core.JacocoGenerated;

import java.util.ArrayList;
import java.util.List;

public class Author {
    private Integer cristinId;
    private String externalId;
    private String roleCode;
    private Integer sequenceNr;
    private String surname;
    private String firstname;
    private String authorName;
    private String authorNamePreferred;
    private String orcid;
    private List<Institution> institutions;


    public Author(Integer cristinId, String externalId, String roleCode, Integer sequenceNr, String surname, String firstname, String authorName, String authorNamePreferred, String orcid, List<Institution> institutions) {
        this.cristinId = cristinId;
        this.externalId = externalId;
        this.roleCode = roleCode;
        this.sequenceNr = sequenceNr;
        this.surname = surname;
        this.firstname = firstname;
        this.authorName = authorName;
        this.authorNamePreferred = authorNamePreferred;
        this.orcid = orcid;
        this.institutions = institutions;
    }


    @JacocoGenerated
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @JacocoGenerated
    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    @JacocoGenerated
    public void setCristinId(Integer cristinId) {
        this.cristinId = cristinId;
    }

    @JacocoGenerated
    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    @JacocoGenerated
    public void setSequenceNr(Integer sequenceNr) {
        this.sequenceNr = sequenceNr;
    }

    @JacocoGenerated
    public void setSurname(String surname) {
        this.surname = surname;
    }

    @JacocoGenerated
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    @JacocoGenerated
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    @JacocoGenerated
    public void setAuthorNamePreferred(String authorNamePreferred) {
        this.authorNamePreferred = authorNamePreferred;
    }

    @JacocoGenerated
    public void setInstitutions(List<Institution> institutions) {
        this.institutions = institutions;
    }

    @JacocoGenerated
    public Integer getCristinId() {
        return cristinId;
    }


    public String getExternalId() {
        return externalId;
    }

    @JacocoGenerated
    public String getRoleCode() {
        return roleCode;
    }

    @JacocoGenerated
    public Integer getSequenceNr() {
        return sequenceNr;
    }


    public String getSurname() {
        return surname;
    }

    @JacocoGenerated
    public String getFirstname() {
        return firstname;
    }


    public String getAuthorName() {
        return authorName;
    }

    @JacocoGenerated
    public String getAuthorNamePreferred() {
        return authorNamePreferred;
    }


    public String getOrcid() {
        return orcid;
    }

    public List<Institution> getInstitutions() {
        if (institutions == null) institutions = new ArrayList<>();
        return institutions;
    }

    @JacocoGenerated
    @Override public String toString() {
        return "Author{" +
                "cristinId=" + cristinId +
                ", externalId='" + externalId + '\'' +
                ", roleCode='" + roleCode + '\'' +
                ", sequenceNr=" + sequenceNr +
                ", surname='" + surname + '\'' +
                ", firstname='" + firstname + '\'' +
                ", authorName='" + authorName + '\'' +
                ", authorNamePreferred='" + authorNamePreferred + '\'' +
                ", orcid='" + orcid + '\'' +
                ", institutions=" + institutions +
                '}';
    }


}
