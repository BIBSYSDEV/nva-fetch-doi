package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;


import java.util.List;

@SuppressWarnings({"PMD.TooManyFields", "PMD.ExcessivePublicCount"})
public class CrossRefDocument {

    @JsonProperty("publisher")
    private String publisher;
    @JsonProperty("title")
    private List<String> title;
    @JsonProperty("original-title")
    private List<String> originalTitle;
    @JsonProperty("short-title")
    private List<String> shortTitle;
    @JsonProperty("abstract")
    private String abstractText;
    @JsonProperty("reference-count")
    private int referenceCount;   // this is deprecated,  Same as references-count
    @JsonProperty("references-count")
    private int referencesCount;
    @SuppressWarnings("PMD.LinguisticNaming")
    @JsonProperty("is-referenced-by-count")
    private int isReferencedByCount;
    @JsonProperty("source")
    private String source;
    @JsonProperty("prefix")
    private String prefix;
    @JsonProperty("DOI")
    private String doi;
    @JsonProperty("URL")
    private String url;
    @JsonProperty("member")
    private String member;
    @JsonProperty("type")
    private String type;
    @JsonProperty("created")
    private CrossrefDate created;
    @JsonProperty("deposited")
    private CrossrefDate deposited;
    @JsonProperty("indexed")
    private CrossrefDate indexed;
    @JsonProperty("issued")
    private CrossrefDate issued;
    @JsonProperty("posted")
    private CrossrefDate posted;
    @JsonProperty("accepted")
    private CrossrefDate accepted;
    @JsonProperty("subtitle")
    private List<String> subtitle;
    @JsonProperty("container-title")
    private List<String> containerTitle;
    @JsonProperty("short-container-title")
    private List<String> shortContainerTitle;
    @JsonProperty("group-title")
    private List<String> groupTitle;
    @JsonProperty("issue")
    private String issue;
    @JsonProperty("volume")
    private String volume;
    @JsonProperty("page")
    private String page;
    @JsonProperty("article-number")
    private String articleNumber;
    @JsonProperty("published-print")
    private CrossrefDate publishedPrint;
    @JsonProperty("published-online")
    private CrossrefDate publishedOnline;
    @JsonProperty("subject")
    private List<String> subject;
    @JsonProperty("ISSN")
    private List<String> issn;
    @JsonProperty("issn-type")
    private List<Isxn> issnType;
    @JsonProperty("ISBN")
    private List<String> isbn;
    @JsonProperty("isbn-type")
    private List<Isxn> isbnType;
    @JsonProperty("archive")
    private List<String> archive;
    @JsonProperty("license")
    private List<License> license;
    @JsonProperty("funder")
    private List<CrossrefFunder> funder;
    @JsonProperty("assertion")
    private List<CrossrefAssertion> assertion;
    @JsonProperty("author")
    private List<CrossrefContributor> author;
    @JsonProperty("editor")
    private List<CrossrefContributor> editor;
    @JsonProperty("chair")
    private List<CrossrefContributor> chair;
    @JsonProperty("translator")
    private List<CrossrefContributor> translator;
    @JsonProperty("update-to")
    private List<CrossrefUpdate> updateTo;
    @JsonProperty("update-policy")
    private String updatePolicy;
    @JsonProperty("link")
    private List<Link> link;
    @JsonProperty("clinical-trial-number")
    private List<CrossrefClinicalTrialNumber> clinicalTrialNumber;
    @JsonProperty("alternative-id")
    private List<String> alternativeId;
    @JsonProperty("reference")
    private List<CrossRefReference> reference;
    @JsonProperty("content-domain")
    private ContentDomain contentDomain;
    @JsonProperty("relation")
    private CrossrefRelation relation;
    @JsonProperty("review")
    private CrossrefReview review;
    @JsonProperty("publisher-location")
    private String publisherLocation;


    @JsonProperty("score")
    private float score;

    @JsonProperty("language")
    private String language;

    public void setIndexed(CrossrefDate indexed) {
        this.indexed = indexed;
    }

    public ContentDomain getContentDomain() {
        return contentDomain;
    }

    public void setContentDomain(ContentDomain contentDomain) {
        this.contentDomain = contentDomain;
    }

    public List<String> getShortContainerTitle() {
        return shortContainerTitle;
    }

    public void setShortContainerTitle(List<String> shortContainerTitle) {
        this.shortContainerTitle = shortContainerTitle;
    }

    public int getReferenceCount() {
        return referenceCount;
    }

    public void setReferenceCount(int input) {
        this.referenceCount = input;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String input) {
        this.publisher = input;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String input) {
        this.issue = input;
    }

    public List<License> getLicense() {
        return license;
    }

    @JacocoGenerated
    public void setLicense(List<License> input) {
        this.license = input;
    }

    public List<String> getContainerTitle() {
        return containerTitle;
    }

    public void setContainerTitle(List<String> input) {
        this.containerTitle = input;
    }

    public List<String> getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(List<String> input) {
        this.originalTitle = input;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String input) {
        this.language = input;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> input) {
        this.link = input;
    }

    public CrossrefDate getDeposited() {
        return deposited;
    }

    public void setDeposited(CrossrefDate input) {
        this.deposited = input;
    }

    public float getScore() {
        return score;
    }

    public List<String> getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(List<String> input) {
        this.subtitle = input;
    }

    public List<String> getShortTitle() {
        return shortTitle;
    }

    public void setShortTitle(List<String> input) {
        this.shortTitle = input;
    }

    public CrossrefDate getIssued() {
        return issued;
    }

    public void setIssued(CrossrefDate input) {
        this.issued = input;
    }

    public int getReferencesCount() {
        return referencesCount;
    }

    public void setReferencesCount(int input) {
        this.referencesCount = input;
    }

    public List<String> getAlternativeId() {
        return alternativeId;
    }

    public void setAlternativeId(List<String> input) {
        this.alternativeId = input;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String input) {
        this.url = input;
    }

    public CrossrefRelation getRelation() {
        return relation;
    }

    public void setRelation(CrossrefRelation input) {
        this.relation = input;
    }

    public List<String> getIssn() {
        return issn;
    }

    public void setIssn(List<String> input) {
        this.issn = input;
    }

    public List<Isxn> getIssnType() {
        return issnType;
    }

    public void setIssnType(List<Isxn> input) {
        this.issnType = input;
    }

    public void setIsbnType(List<Isxn> isbnType) {
        this.isbnType = isbnType;
    }

    public List<Isxn> getIsbnType() {
        return isbnType;
    }

    public CrossrefDate getPublishedPrint() {
        return publishedPrint;
    }

    public void setPublishedPrint(CrossrefDate publishedPrint) {
        this.publishedPrint = publishedPrint;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public CrossrefDate getCreated() {
        return created;
    }

    public void setCreated(CrossrefDate created) {
        this.created = created;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getIsReferencedByCount() {
        return isReferencedByCount;
    }

    public void setIsReferencedByCount(int isReferencedByCount) {
        this.isReferencedByCount = isReferencedByCount;
    }

    public List<String> getTitle() {
        return title;
    }

    public void setTitle(List<String> title) {
        this.title = title;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public List<CrossrefContributor> getAuthor() {
        return author;
    }

    public void setAuthor(List<CrossrefContributor> author) {
        this.author = author;
    }

    public CrossrefDate getIndexed() {
        return indexed;
    }

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public List<CrossRefReference> getReference() {
        return reference;
    }

    public void setReference(List<CrossRefReference> reference) {
        this.reference = reference;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public List<String> getSubject() {
        return subject;
    }

    public void setSubject(List<String> subject) {
        this.subject = subject;
    }

    public CrossrefDate getPosted() {
        return posted;
    }

    @JacocoGenerated
    public void setPosted(CrossrefDate posted) {
        this.posted = posted;
    }

    public CrossrefDate getAccepted() {
        return accepted;
    }

    @JacocoGenerated
    public void setAccepted(CrossrefDate accepted) {
        this.accepted = accepted;
    }

    public List<String> getGroupTitle() {
        return groupTitle;
    }

    @JacocoGenerated
    public void setGroupTitle(List<String> groupTitle) {
        this.groupTitle = groupTitle;
    }

    public String getArticleNumber() {
        return articleNumber;
    }

    @JacocoGenerated
    public void setArticleNumber(String articleNumber) {
        this.articleNumber = articleNumber;
    }

    public CrossrefDate getPublishedOnline() {
        return publishedOnline;
    }

    public void setPublishedOnline(CrossrefDate publishedOnline) {
        this.publishedOnline = publishedOnline;
    }

    public List<String> getIsbn() {
        return isbn;
    }

    public void setIsbn(List<String> isbn) {
        this.isbn = isbn;
    }

    public List<String> getArchive() {
        return archive;
    }

    @JacocoGenerated
    public void setArchive(List<String> archive) {
        this.archive = archive;
    }

    public List<CrossrefFunder> getFunder() {
        return funder;
    }

    @JacocoGenerated
    public void setFunder(List<CrossrefFunder> funder) {
        this.funder = funder;
    }

    public List<CrossrefAssertion> getAssertion() {
        return assertion;
    }

    @JacocoGenerated
    public void setAssertion(List<CrossrefAssertion> assertion) {
        this.assertion = assertion;
    }

    public List<CrossrefContributor> getEditor() {
        return editor;
    }

    public void setEditor(List<CrossrefContributor> editor) {
        this.editor = editor;
    }

    public List<CrossrefContributor> getChair() {
        return chair;
    }

    @JacocoGenerated
    public void setChair(List<CrossrefContributor> chair) {
        this.chair = chair;
    }

    public List<CrossrefContributor> getTranslator() {
        return translator;
    }

    @JacocoGenerated
    public void setTranslator(List<CrossrefContributor> translator) {
        this.translator = translator;
    }

    public List<CrossrefUpdate> getUpdateTo() {
        return updateTo;
    }

    @JacocoGenerated
    public void setUpdateTo(List<CrossrefUpdate> updateTo) {
        this.updateTo = updateTo;
    }

    public String getUpdatePolicy() {
        return updatePolicy;
    }

    @JacocoGenerated
    public void setUpdatePolicy(String updatePolicy) {
        this.updatePolicy = updatePolicy;
    }

    public List<CrossrefClinicalTrialNumber> getClinicalTrialNumber() {
        return clinicalTrialNumber;
    }

    @JacocoGenerated
    public void setClinicalTrialNumber(List<CrossrefClinicalTrialNumber> clinicalTrialNumber) {
        this.clinicalTrialNumber = clinicalTrialNumber;
    }

    public CrossrefReview getReview() {
        return review;
    }

//    @JacocoGenerated
    public void setReview(CrossrefReview review) {
        this.review = review;
    }

    public String getPublisherLocation() {
        return publisherLocation;
    }

    @JacocoGenerated
    public void setPublisherLocation(String publisherLocation) {
        this.publisherLocation = publisherLocation;
    }
}


