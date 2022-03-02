package no.sikt.nva.scopus;

import java.util.Arrays;

public enum ScopusSourceType {

    BOOK("b"),
    BOOKSERIES("k"),
    JOURNAL("j"),
    REPORT("r");

    private final String code;

    ScopusSourceType(String code) {
        this.code = code;
    }

    public static ScopusSourceType valueOfCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findAny()
                .orElse(null);
    }

    public String getCode() {
        return code;
    }
}
