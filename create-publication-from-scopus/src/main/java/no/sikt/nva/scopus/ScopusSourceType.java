package no.sikt.nva.scopus;

public enum ScopusSourceType {

    JOURNAL("j");

    public final String tag;

    ScopusSourceType(String tag) {
        this.tag = tag;
    }

    public static ScopusSourceType valueOfTag(String tag) {
        for (ScopusSourceType t : values()) {
            if (t.tag.equals(tag)) {
                return t;
            }
        }
        return null;
    }

}
