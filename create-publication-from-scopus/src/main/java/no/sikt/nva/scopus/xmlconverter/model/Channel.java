package no.sikt.nva.scopus.xmlconverter.model;

import nva.commons.core.JacocoGenerated;

import java.util.ArrayList;
import java.util.List;

public class Channel {
    private Integer cristinTidsskriftNr;
    private String externalId;
    private String externalName;
    private String issn;
    private String eissn;
    private List<String> isbns;
    private String volume;
    private String issue;
    private Integer numberOfPages;
    private String supplement;
    private String pageFrom;
    private String pageTo;
    private String country;
    private String articleNr;
    private String publisherName;


    public Channel(String issn, String volume, String issue) {
        this.issn = issn;
        this.volume = volume;
        this.issue = issue;
    }

    @JacocoGenerated
    public void setCristinTidsskriftNr(Integer cristinTidsskriftNr) {
        this.cristinTidsskriftNr = cristinTidsskriftNr;
    }


    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }


    public void setExternalName(String externalName) {
        this.externalName = externalName;
    }


    public void setIssn(String issn) {
        this.issn = issn;
    }


    public void setEissn(String eissn) {
        this.eissn = eissn;
    }

    @JacocoGenerated
    public void setIsbns(List<String> isbns) {
        this.isbns = isbns;
    }

    @JacocoGenerated
    public void setVolume(String volume) {
        this.volume = volume;
    }

    @JacocoGenerated
    public void setIssue(String issue) {
        this.issue = issue;
    }

    @JacocoGenerated
    public void setNumberOfPages(Integer numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    @JacocoGenerated
    public void setSupplement(String supplement) {
        this.supplement = supplement;
    }


    public void setPageFrom(String pageFrom) {
        this.pageFrom = pageFrom;
    }


    public void setPageTo(String pageTo) {
        this.pageTo = pageTo;
    }


    public void setCountry(String country) {
        this.country = country;
    }


    public void setArticleNr(String articleNr) {
        this.articleNr = articleNr;
    }


    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    @JacocoGenerated
    public Integer getCristinTidsskriftNr() {
        return cristinTidsskriftNr;
    }

    @JacocoGenerated
    public String getExternalId() {
        return externalId;
    }

    @JacocoGenerated
    public String getExternalName() {
        return externalName;
    }

    @JacocoGenerated
    public String getIssn() {
        return issn;
    }

    @JacocoGenerated
    public String getEissn() {
        return eissn;
    }


    public List<String> getIsbns() {
        if (isbns == null) isbns = new ArrayList<>();
        return isbns;
    }


    public String getVolume() {
        return volume;
    }


    public String getIssue() {
        return issue;
    }


    public Integer getNumberOfPages() {
        return numberOfPages;
    }


    public String getSupplement() {
        return supplement;
    }

    @JacocoGenerated
    public String getPageFrom() {
        return pageFrom;
    }

    @JacocoGenerated
    public String getPageTo() {
        return pageTo;
    }

    @JacocoGenerated
    public String getCountry() {
        return country;
    }

    @JacocoGenerated
    public String getArticleNr() {
        return articleNr;
    }

    @JacocoGenerated
    public String getPublisherName() {
        return publisherName;
    }

    @JacocoGenerated
    @Override public String toString() {
        return "Channel{" +
                "cristinTidsskriftNr=" + cristinTidsskriftNr +
                ", externalId='" + externalId + '\'' +
                ", externalName='" + externalName + '\'' +
                ", issn='" + issn + '\'' +
                ", eissn='" + eissn + '\'' +
                ", isbns=" + isbns +
                ", volume='" + volume + '\'' +
                ", issue='" + issue + '\'' +
                ", numberOfPages=" + numberOfPages +
                ", supplement='" + supplement + '\'' +
                ", pageFrom='" + pageFrom + '\'' +
                ", pageTo='" + pageTo + '\'' +
                ", country='" + country + '\'' +
                ", articleNr='" + articleNr + '\'' +
                ", publisherName='" + publisherName + '\'' +
                '}';
    }
}
