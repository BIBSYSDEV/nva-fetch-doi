package no.unit.nva.doi.transformer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SchemaOrgTypeTest {
    @DisplayName("getByType returns NON_EXISTING_TYPE when the type does not exist")
    @ParameterizedTest
    @ValueSource(strings = {"", "X", "SchalarlyArticle", "Årticle"})
    void getByTypeReturnsNullWhenValueDoesNotExist(String candidate) {
        assertEquals(SchemaOrgType.NON_EXISTING_TYPE, SchemaOrgType.getByType(candidate));
    }

    @DisplayName("getByType returns NON_EXISTING_TYPE when the type is null")
    @Test
    void getByTypeReturnsNullWhenValueDoesNotExist() {
        assertEquals(SchemaOrgType.NON_EXISTING_TYPE, SchemaOrgType.getByType(null));
    }

    @DisplayName("getByType returns SchemaOrgType when type exists ")
    @ParameterizedTest
    @ValueSource(strings = {"ArchiveComponent", "Article", "AdvertiserContentArticle", "NewsArticle",
            "AnalysisNewsArticle", "AskPublicNewsArticle", "BackgroundNewsArticle", "OpinionNewsArticle",
            "ReportageNewsArticle", "ReviewNewsArticle", "Report", "ScholarlyArticle", "MedicalScholarlyArticle",
            "SocialMediaPosting", "BlogPosting", "LiveBlogPosting", "DiscussionForumPosting", "TechArticle",
            "APIReference", "Atlas", "Blog", "Book", "Audiobook", "Chapter", "Claim", "Clip", "MovieClip", "RadioClip",
            "TVClip", "VideoGameClip", "Collection", "ComicStory", "ComicCoverArt", "Comment", "Answer",
            "CorrectionComment", "Conversation", "Course", "CreativeWorkSeason", "PodcastSeason", "RadioSeason",
            "TVSeason", "CreativeWorkSeries", "BookSeries", "MovieSeries", "Periodical", "ComicSeries", "Newspaper",
            "PodcastSeries", "RadioSeries", "TVSeries", "VideoGameSeries", "DataCatalog", "Dataset", "DataFeed",
            "CompleteDataFeed", "DefinedTermSet", "CategoryCodeSet", "Diet", "DigitalDocument", "NoteDigitalDocument",
            "PresentationDigitalDocument", "SpreadsheetDigitalDocument", "TextDigitalDocument", "Drawing",
            "EducationalOccupationalCredential", "Episode", "PodcastEpisode", "RadioEpisode", "TVEpisode",
            "ExercisePlan", "Game", "VideoGame", "Guide", "HowTo", "Recipe", "HowToDirection", "HowToSection",
            "HowToStep", "HowToTip", "Legislation", "LegislationObject", "Manuscript", "Map", "MediaObject", "3DModel",
            "AudioObject", "DataDownload", "ImageObject", "Barcode", "MusicVideoObject", "VideoObject", "Menu",
            "MenuSection", "Message", "EmailMessage", "Movie", "MusicComposition", "MusicPlaylist", "MusicAlbum",
            "MusicRelease", "MusicRecording", "Painting", "Photograph", "Play", "Poster", "PublicationIssue",
            "ComicIssue", "PublicationVolume", "Question", "Quotation", "Review", "ClaimReview", "CriticReview",
            "EmployerReview", "MediaReview", "Recommendation", "UserReview", "Sculpture", "SheetMusic", "ShortStory",
            "SoftwareApplication", "MobileApplication", "WebApplication", "SoftwareSourceCode", "SpecialAnnouncement",
            "Thesis", "VisualArtwork", "CoverArt", "WebContent", "HealthTopicContent", "WebPage", "AboutPage",
            "CheckoutPage", "CollectionPage", "MediaGallery", "ImageGallery", "VideoGallery", "ContactPage", "FAQPage",
            "ItemPage", "MedicalWebPage", "ProfilePage", "QAPage", "RealEstateListing", "SearchResultsPage",
            "WebPageElement", "SiteNavigationElement", "Table", "WPAdBlock", "WPFooter", "WPHeader", "WPSideBar",
            "WebSite"})
    void getByTypeReturnsSchemaOrgTypeWhenTypeExists(String input) {
        assertNotEquals(SchemaOrgType.NON_EXISTING_TYPE, SchemaOrgType.getByType(input));
    }
}