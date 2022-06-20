package no.sikt.nva.scopus.conversion.model.cristin;

import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.FormalParameterNamingConventions", "PMD.MethodNamingConventions"})
public class Institution {

    private final String cristin_institution_id;

    private final String url;

    @JacocoGenerated
    public Institution(String cristin_institution_id, String url) {
        this.cristin_institution_id = cristin_institution_id;
        this.url = url;
    }

    @JacocoGenerated
    public String getCristin_institution_id() {
        return cristin_institution_id;
    }

    @JacocoGenerated
    public String getUrl() {
        return url;
    }
}
