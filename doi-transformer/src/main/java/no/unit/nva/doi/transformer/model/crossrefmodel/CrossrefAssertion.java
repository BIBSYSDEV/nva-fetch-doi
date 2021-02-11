package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class CrossrefAssertion {

    @JsonProperty("name")
    private String name;
    @JsonProperty("value")
    private String value;
    @JsonProperty("URL")
    private String url;
    @JsonProperty("explanation")
    private String explanation;
    @JsonProperty("label")
    private String label;
    @JsonProperty("order")
    private int order;
    @JsonProperty("group")
    private CrossrefAssertionGroup group;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public CrossrefAssertionGroup getGroup() {
        return group;
    }

    public void setGroup(CrossrefAssertionGroup group) {
        this.group = group;
    }
}
