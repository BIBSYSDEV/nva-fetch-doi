package no.unit.nva.doi.transformer.utils;

import static java.util.Objects.isNull;

import java.util.Arrays;
import no.unit.nva.model.PublicationType;
import nva.commons.utils.SingletonCollector;

public enum SchemaOrgType {
    ARCHIVE_COMPONENT("ArchiveComponent", null),
    ARTICLE("Article", null),
    ADVERTISER_CONTENT_ARTICLE("AdvertiserContentArticle", null),
    NEWS_ARTICLE("NewsArticle", null),
    ANALYSIS_NEWS_ARTICLE("AnalysisNewsArticle", null),
    ASK_PUBLIC_NEWS_ARTICLE("AskPublicNewsArticle", null),
    BACKGROUND_NEWS_ARTICLE("BackgroundNewsArticle", null),
    OPINION_NEWS_ARTICLE("OpinionNewsArticle", null),
    REPORTAGE_NEWS_ARTICLE("ReportageNewsArticle", null),
    REVIEW_NEWS_ARTICLE("ReviewNewsArticle", null),
    REPORT("Report", null),
    SCHOLARLY_ARTICLE("ScholarlyArticle", PublicationType.JOURNAL_CONTENT),
    MEDICAL_SCHOLARLY_ARTICLE("MedicalScholarlyArticle", PublicationType.JOURNAL_CONTENT),
    SOCIAL_MEDIA_POSTING("SocialMediaPosting", null),
    BLOG_POSTING("BlogPosting", null),
    LIVE_BLOG_POSTING("LiveBlogPosting", null),
    DISCUSSION_FORUM_POSTING("DiscussionForumPosting", null),
    TECH_ARTICLE("TechArticle", null),
    API_REFERENCE("APIReference", null),
    ATLAS("Atlas", null),
    BLOG("Blog", null),
    BOOK("Book", null),
    AUDIOBOOK("Audiobook", null),
    CHAPTER("Chapter", null),
    CLAIM("Claim", null),
    CLIP("Clip", null),
    MOVIE_CLIP("MovieClip", null),
    RADIO_CLIP("RadioClip", null),
    TV_CLIP("TVClip", null),
    VIDEO_GAME_CLIP("VideoGameClip", null),
    COLLECTION("Collection", null),
    COMIC_STORY("ComicStory", null),
    COMIC_COVER_ART("ComicCoverArt", null),
    COMMENT("Comment", null),
    ANSWER("Answer", null),
    CORRECTION_COMMENT("CorrectionComment", null),
    CONVERSATION("Conversation", null),
    COURSE("Course", null),
    CREATIVE_WORK_SEASON("CreativeWorkSeason", null),
    PODCAST_SEASON("PodcastSeason", null),
    RADIO_SEASON("RadioSeason", null),
    TV_SEASON("TVSeason", null),
    CREATIVE_WORK_SERIES("CreativeWorkSeries", null),
    BOOK_SERIES("BookSeries", null),
    MOVIE_SERIES("MovieSeries", null),
    PERIODICAL("Periodical", null),
    COMIC_SERIES("ComicSeries", null),
    NEWSPAPER("Newspaper", null),
    PODCAST_SERIES("PodcastSeries", null),
    RADIO_SERIES("RadioSeries", null),
    TV_SERIES("TVSeries", null),
    VIDEO_GAME_SERIES("VideoGameSeries", null),
    DATA_CATALOG("DataCatalog", null),
    DATASET("Dataset", null),
    DATA_FEED("DataFeed", null),
    COMPLETE_DATA_FEED("CompleteDataFeed", null),
    DEFINED_TERM_SET("DefinedTermSet", null),
    CATEGORY_CODE_SET("CategoryCodeSet", null),
    DIET("Diet", null),
    DIGITAL_DOCUMENT("DigitalDocument", null),
    NOTE_DIGITAL_DOCUMENT("NoteDigitalDocument", null),
    PRESENTATION_DIGITAL_DOCUMENT("PresentationDigitalDocument", null),
    SPREADSHEET_DIGITAL_DOCUMENT("SpreadsheetDigitalDocument", null),
    TEXT_DIGITAL_DOCUMENT("TextDigitalDocument", null),
    DRAWING("Drawing", null),
    EDUCATIONAL_OCCUPATIONAL_CREDENTIAL("EducationalOccupationalCredential", null),
    EPISODE("Episode", null),
    PODCAST_EPISODE("PodcastEpisode", null),
    RADIO_EPISODE("RadioEpisode", null),
    TV_EPISODE("TVEpisode", null),
    EXERCISE_PLAN("ExercisePlan", null),
    GAME("Game", null),
    VIDEO_GAME("VideoGame", null),
    GUIDE("Guide", null),
    HOW_TO("HowTo", null),
    RECIPE("Recipe", null),
    HOW_TO_DIRECTION("HowToDirection", null),
    HOW_TO_SECTION("HowToSection", null),
    HOW_TO_STEP("HowToStep", null),
    HOW_TO_TIP("HowToTip", null),
    LEGISLATION("Legislation", null),
    LEGISLATION_OBJECT("LegislationObject", null),
    MANUSCRIPT("Manuscript", null),
    MAP("Map", null),
    MEDIA_OBJECT("MediaObject", null),
    THREE_DIMENSIONAL_MODEL("3DModel", null),
    AUDIO_OBJECT("AudioObject", null),
    DATA_DOWNLOAD("DataDownload", null),
    IMAGE_OBJECT("ImageObject", null),
    BARCODE("Barcode", null),
    MUSIC_VIDEO_OBJECT("MusicVideoObject", null),
    VIDEO_OBJECT("VideoObject", null),
    MENU("Menu", null),
    MENU_SECTION("MenuSection", null),
    MESSAGE("Message", null),
    EMAIL_MESSAGE("EmailMessage", null),
    MOVIE("Movie", null),
    MUSIC_COMPOSITION("MusicComposition", null),
    MUSIC_PLAYLIST("MusicPlaylist", null),
    MUSIC_ALBUM("MusicAlbum", null),
    MUSIC_RELEASE("MusicRelease", null),
    MUSIC_RECORDING("MusicRecording", null),
    PAINTING("Painting", null),
    PHOTOGRAPH("Photograph", null),
    PLAY("Play", null),
    POSTER("Poster", null),
    PUBLICATION_ISSUE("PublicationIssue", null),
    COMIC_ISSUE("ComicIssue", null),
    PUBLICATION_VOLUME("PublicationVolume", null),
    QUESTION("Question", null),
    QUOTATION("Quotation", null),
    REVIEW("Review", null),
    CLAIMREVIEW("ClaimReview", null),
    CRITIC_REVIEW("CriticReview", null),
    EMPLOYER_REVIEW("EmployerReview", null),
    MEDIA_REVIEW("MediaReview", null),
    RECOMMENDATION("Recommendation", null),
    USER_REVIEW("UserReview", null),
    SCULPTURE("Sculpture", null),
    SHEET_MUSIC("SheetMusic", null),
    SHORT_STORY("ShortStory", null),
    SOFTWARE_APPLICATION("SoftwareApplication", null),
    MOBILE_APPLICATION("MobileApplication", null),
    WEB_APPLICATION("WebApplication",null),
    SOFTWARE_SOURCE_CODE("SoftwareSourceCode",null),
    SPECIAL_ANNOUNCEMENT("SpecialAnnouncement",null),
    THESIS("Thesis",null),
    VISUAL_ARTWORK("VisualArtwork",null),
    COVER_ART("CoverArt",null),
    WEB_CONTENT("WebContent",null),
    HEALTH_TOPIC_CONTENT("HealthTopicContent",null),
    WEB_PAGE("WebPage",null),
    ABOUT_PAGE("AboutPage",null),
    CHECKOUT_PAGE("CheckoutPage",null),
    COLLECTION_PAGE("CollectionPage",null),
    MEDIA_ALLERY("MediaGallery",null),
    IMAGE_GALLERY("ImageGallery",null),
    VIDEO_GALLERY("VideoGallery",null),
    CONTACT_PAGE("ContactPage",null),
    FAQ_PAGE("FAQPage",null),
    ITEM_PAGE("ItemPage",null),
    MEDICAL_WEBPAGE("MedicalWebPage",null),
    PROFILE_PAGE("ProfilePage",null),
    QA_PAGE("QAPage",null),
    REAL_ESTATE_LISTING("RealEstateListing",null),
    SEARCH_RESULTS_PAGE("SearchResultsPage",null),
    WEB_PAGE_ELEMENT("WebPageElement",null),
    SITE_NAVIGATION_ELEMENT("SiteNavigationElement",null),
    TABLE("Table",null),
    WP_AD_BLOCK("WPAdBlock",null),
    WP_FOOTER("WPFooter",null),
    WP_HEADER("WPHeader",null),
    WP_SIDEBAR("WPSideBar",null),
    WEBSITE("WebSite",null),
    NON_EXISTING_TYPE(null, null);

    private final String type;
    private final PublicationType publicationType;

    public String getType() {
        return this.type;
    }

    SchemaOrgType(String type, PublicationType publicationType) {
        this.type = type;
        this.publicationType = publicationType;
    }

    /**
     * Retrieve the PublicationType based on a Schema.org type string.
     *
     * @param type the Schema.org type string.
     * @return a PublicationType.
     */
    public static SchemaOrgType getByType(String type) {
        if (isNull(type)) {
            return NON_EXISTING_TYPE;
        }

        return Arrays.stream(values())
                .filter(schemaOrgType -> !schemaOrgType.equals(SchemaOrgType.NON_EXISTING_TYPE))
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .collect(SingletonCollector.collectOrElse(NON_EXISTING_TYPE));
    }

    public PublicationType getPublicationType() {
        return this.publicationType;
    }
}
