package no.sikt.nva.scopus;

public enum ScopusSourceType {

    JOURNAL("j");

    public final String code;

    ScopusSourceType(String code) {
        this.code = code;
    }

    public static ScopusSourceType valueOfCode(String code) {
        for (ScopusSourceType c : values()) {
            if (c.code.equals(code)) {
                return c;
            }
        }
        return null;
    }

}
