package no.unit.nva.metadata.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.utils.JacocoGenerated;

import java.net.URI;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata {

    private URI id;
    private String type;
    @JsonProperty("creator")
    private List<Map<String,String>> creators;
    private Map<String,String> title;
    @JsonProperty("subject")
    private List<Map<String,String>> subjects;
    private Map<String,String> description;
    private Map<String,String> date;
    @JsonProperty("https://unit.no/ontology#language")
    private Map<String,String> language;

    public URI getId() {
        return id;
    }

    @JacocoGenerated
    public void setId(URI id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    @JacocoGenerated
    public void setType(String type) {
        this.type = type;
    }

    public List<Map<String, String>> getCreators() {
        return creators;
    }

    @JacocoGenerated
    public void setCreators(List<Map<String, String>> creators) {
        this.creators = creators;
    }

    public Map<String, String> getTitle() {
        return title;
    }

    @JacocoGenerated
    public void setTitle(Map<String, String> title) {
        this.title = title;
    }

    public List<Map<String, String>> getSubjects() {
        return subjects;
    }

    @JacocoGenerated
    public void setSubjects(List<Map<String, String>> subjects) {
        this.subjects = subjects;
    }

    public Map<String, String> getDescription() {
        return description;
    }

    @JacocoGenerated
    public void setDescription(Map<String, String> description) {
        this.description = description;
    }

    public Map<String, String> getDate() {
        return date;
    }

    @JacocoGenerated
    public void setDate(Map<String, String> date) {
        this.date = date;
    }

    public Map<String, String> getLanguage() {
        return language;
    }

    @JacocoGenerated
    public void setLanguage(Map<String, String> language) {
        this.language = language;
    }
}
