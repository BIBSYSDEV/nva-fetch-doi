package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

public class CrossRefReference {

    @JsonProperty("key")
    private String key;
    @JsonProperty("doi-asserted-by")
    private String doiAssertedBy;
    @JsonProperty("first-page")
    private String firstPage;
    @JsonProperty("DOI")
    private String doi;
    @JsonProperty("volume")
    private String volume;
    @JsonProperty("author")
    private String author;
    @JsonProperty("year")
    private String year;
    @JsonProperty("unstructured")
    private String unstructuredReference;
    @JsonProperty("journal-title")
    private String journalTitle;

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
}
