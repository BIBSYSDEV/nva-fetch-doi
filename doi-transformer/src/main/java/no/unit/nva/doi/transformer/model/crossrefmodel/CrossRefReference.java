package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.TooManyFields")
public class CrossRefReference {

    @JsonProperty("key")
    private String key;
    @JsonProperty("DOI")
    private String doi;
    @JsonProperty("doi-asserted-by")
    private String doiAssertedBy;
    @JsonProperty("issue")
    private String issue;
    @JsonProperty("first-page")
    private String firstPage;
    @JsonProperty("volume")
    private String volume;
    @JsonProperty("edition")
    private String edition;
    @JsonProperty("component")
    private String component;
    @JsonProperty("standard-designator")
    private String standardDesignator;
    @JsonProperty("standard-body")
    private String standardBody;
    @JsonProperty("author")
    private String author;
    @JsonProperty("year")
    private String year;
    @JsonProperty("unstructured")
    private String unstructuredReference;
    @JsonProperty("journal-title")
    private String journalTitle;
    @JsonProperty("article-title")
    private String articleTitle;
    @JsonProperty("series-title")
    private String seriesTitle;
    @JsonProperty("volume-title")
    private String volumeTitle;
    @JsonProperty("ISSN")
    private String issn;
    @JsonProperty("issn-type")
    private String issnType;
    @JsonProperty("ISBN")
    private String isbn;
    @JsonProperty("isbn-type")
    private String isbnType;


    @JacocoGenerated
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @JacocoGenerated
    public String getDoiAssertedBy() {
        return doiAssertedBy;
    }

    public void setDoiAssertedBy(String doiAssertedBy) {
        this.doiAssertedBy = doiAssertedBy;
    }

    @JacocoGenerated
    public String getFirstPage() {
        return firstPage;
    }

    public void setFirstPage(String firstPage) {
        this.firstPage = firstPage;
    }

    @JacocoGenerated
    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    @JacocoGenerated
    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    @JacocoGenerated
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @JacocoGenerated
    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    @JacocoGenerated
    public String getUnstructuredReference() {
        return unstructuredReference;
    }

    @JacocoGenerated
    public void setUnstructuredReference(String unstructuredReference) {
        this.unstructuredReference = unstructuredReference;
    }

    @JacocoGenerated
    public String getJournalTitle() {
        return journalTitle;
    }

    public void setJournalTitle(String journalTitle) {
        this.journalTitle = journalTitle;
    }

    public String getIssue() {
        return issue;
    }

    @JacocoGenerated
    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getEdition() {
        return edition;
    }

    @JacocoGenerated
    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getComponent() {
        return component;
    }

    @JacocoGenerated
    public void setComponent(String component) {
        this.component = component;
    }

    public String getStandardDesignator() {
        return standardDesignator;
    }

    @JacocoGenerated
    public void setStandardDesignator(String standardDesignator) {
        this.standardDesignator = standardDesignator;
    }

    public String getStandardBody() {
        return standardBody;
    }

    @JacocoGenerated
    public void setStandardBody(String standardBody) {
        this.standardBody = standardBody;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    @JacocoGenerated
    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
    }

    public String getSeriesTitle() {
        return seriesTitle;
    }

    @JacocoGenerated
    public void setSeriesTitle(String seriesTitle) {
        this.seriesTitle = seriesTitle;
    }

    public String getVolumeTitle() {
        return volumeTitle;
    }

    @JacocoGenerated
    public void setVolumeTitle(String volumeTitle) {
        this.volumeTitle = volumeTitle;
    }

    public String getIssn() {
        return issn;
    }

    @JacocoGenerated
    public void setIssn(String issn) {
        this.issn = issn;
    }

    public String getIssnType() {
        return issnType;
    }

    @JacocoGenerated
    public void setIssnType(String issnType) {
        this.issnType = issnType;
    }

    public String getIsbn() {
        return isbn;
    }

    @JacocoGenerated
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getIsbnType() {
        return isbnType;
    }

    @JacocoGenerated
    public void setIsbnType(String isbnType) {
        this.isbnType = isbnType;
    }
}
