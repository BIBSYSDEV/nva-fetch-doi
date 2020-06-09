package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JournalIssue {

    @JsonProperty("published-print")
    private CrossrefDate publishedPrint;
    @JsonProperty("issue")
    private String issue;

    public CrossrefDate getPublishedPrint() {
        return publishedPrint;
    }

    public void setPublishedPrint(CrossrefDate publishedPrint) {
        this.publishedPrint = publishedPrint;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }
}
