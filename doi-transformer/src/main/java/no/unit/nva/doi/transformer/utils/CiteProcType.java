package no.unit.nva.doi.transformer.utils;

import static java.util.Objects.isNull;

import java.util.Arrays;
import no.unit.nva.model.PublicationType;
import nva.commons.utils.SingletonCollector;

public enum CiteProcType {
    ARTICLE("article", null),
    ARTICLE_JOURNAL("article-journal", PublicationType.JOURNAL_CONTENT),
    ARTICLE_MAGAZINE("article-magazine", null),
    ARTICLE_NEWSPAPER("article-newspaper", null),
    BILL("bill", null),
    BOOK("book", null),
    BROADCAST("broadcast", null),
    CHAPTER("chapter", null),
    DATASET("dataset", null),
    ENTRY("entry", null),
    ENTRY_DICTIONARY("entry-dictionary", null),
    ENTRY_ENCYCLOPEDIA("entry-encyclopedia", null),
    FIGURE("figure", null),
    GRAPHIC("graphic", null),
    INTERVIEW("interview", null),
    LEGAL_CASE("legal_case", null),
    LEGISLATION("legislation", null),
    MANUSCRIPT("manuscript", null),
    MAP("map", null),
    MOTION_PICTURE("motion_picture", null),
    MUSICAL_SCORE("musical_score", null),
    PAMPHLET("pamphlet", null),
    PAPER_CONFERENCE("paper-conference", null),
    PATENT("patent", null),
    PERSONAL_COMMUNICATION("personal_communication", null),
    POST("post", null),
    POST_WEBLOG("post-weblog", null),
    REPORT("report", null),
    REVIEW("review", null),
    REVIEW_BOOK("review-book", null),
    SONG("song", null),
    SPEECH("speech", null),
    THESIS("thesis", null),
    TREATY("treaty", null),
    WEBPAGE("webpage", null),
    NON_EXISTING_TYPE(null, null);

    private final String type;
    private final PublicationType publicationType;

    CiteProcType(String type, PublicationType publicationType) {
        this.type = type;
        this.publicationType = publicationType;
    }

    public String getType() {
        return this.type;
    }

    /**
     * Retrieve the PublicationType based on a CiteProc type string.
     *
     * @param type the CiteProc type string.
     * @return a PublicationType.
     */
    public static CiteProcType getByType(String type) {
        if (isNull(type)) {
            return NON_EXISTING_TYPE;
        }

        return Arrays.stream(values())
                .filter(citeProcType -> !citeProcType.equals(CiteProcType.NON_EXISTING_TYPE))
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .collect(SingletonCollector.collectOrElse(NON_EXISTING_TYPE));
    }

    public PublicationType getPublicationType() {
        return this.publicationType;
    }
}
