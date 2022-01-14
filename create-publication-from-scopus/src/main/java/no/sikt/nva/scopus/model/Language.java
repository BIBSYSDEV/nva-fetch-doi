package no.sikt.nva.scopus.model;

import nva.commons.core.JacocoGenerated;

public class Language {
    private String lang;
    private String title;
    private String summary;
    private boolean original;

    public Language(String lang, String title, String summary, boolean original) {
        this.lang = lang;
        this.title = title;
        this.summary = summary;
        this.original = original;
    }


    @JacocoGenerated
    public void setLang(String lang) {
        this.lang = lang;
    }

    @JacocoGenerated
    public void setTitle(String title) {
        this.title = title;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @JacocoGenerated
    public void setOriginal(boolean original) {
        this.original = original;
    }

    @JacocoGenerated
    public String getLang() {
        return lang;
    }


    public String getTitle() {
        return title;
    }

    @JacocoGenerated
    public String getSummary() {
        return summary;
    }

    public boolean isOriginal() {
        return original;
    }

    @JacocoGenerated
    @Override public String toString() {
        return "Language{" +
                "lang='" + lang + '\'' +
                ", title='" + title + '\'' +
                ", summary='" + summary + '\'' +
                ", original=" + original +
                '}';
    }
}
