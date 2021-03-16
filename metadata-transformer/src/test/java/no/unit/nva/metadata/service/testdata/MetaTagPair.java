package no.unit.nva.metadata.service.testdata;

public class MetaTagPair {
    private final String name;
    private final String content;

    public MetaTagPair(String name, String value) {
        this.name = name;
        this.content = value;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }
}
