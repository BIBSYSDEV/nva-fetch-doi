package no.sikt.nva.scopus.conversion.model.cristin;

import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.FormalParameterNamingConventions", "PMD.MethodNamingConventions", "PMD.ShortClassName"})
public class Unit {

    private final String cristin_unit_id;

    private final String url;

    @JacocoGenerated
    public Unit(String cristin_unit_id, String url) {
        this.cristin_unit_id = cristin_unit_id;
        this.url = url;
    }

    @JacocoGenerated
    public String getCristin_unit_id() {
        return cristin_unit_id;
    }

    @JacocoGenerated
    public String getUrl() {
        return url;
    }
}
