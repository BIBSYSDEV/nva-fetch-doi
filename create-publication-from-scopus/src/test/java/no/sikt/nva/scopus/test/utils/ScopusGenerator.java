package no.sikt.nva.scopus.test.utils;

import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.ORCID_DOMAIN_URL;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFieldsAndClasses;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import jakarta.xml.bind.JAXB;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.xml.bind.JAXBElement;
import no.scopus.generated.AbstractTp;
import no.scopus.generated.AbstractsTp;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorKeywordTp;
import no.scopus.generated.AuthorKeywordsTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.BibrecordTp;
import no.scopus.generated.CitationInfoTp;
import no.scopus.generated.CitationTitleTp;
import no.scopus.generated.CitationTypeTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.DateSortTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.IssnTp;
import no.scopus.generated.ItemInfoTp;
import no.scopus.generated.ItemTp;
import no.scopus.generated.ItemidTp;
import no.scopus.generated.ItemidlistTp;
import no.scopus.generated.MetaTp;
import no.scopus.generated.ObjectFactory;
import no.scopus.generated.OrigItemTp;
import no.scopus.generated.PagerangeTp;
import no.scopus.generated.PersonalnameType;
import no.scopus.generated.ProcessInfo;
import no.scopus.generated.PublisherTp;
import no.scopus.generated.PublishercopyrightTp;
import no.scopus.generated.RichstringWithMMLType;
import no.scopus.generated.ShortTitle;
import no.scopus.generated.SourceTp;
import no.scopus.generated.SourcetitleTp;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.VolissTp;
import no.scopus.generated.VolisspagTp;
import no.scopus.generated.YesnoAtt;
import no.sikt.nva.scopus.ScopusConstants;
import no.sikt.nva.scopus.ScopusSourceType;
import no.unit.nva.language.LanguageConstants;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;

public final class ScopusGenerator {

    private int minimumSequenceNumber;
    public static final Set<Class<?>> NOT_BEAN_CLASSES = Set.of(XMLGregorianCalendar.class);
    public static final int SMALL_NUMBER = 10;
    public static final String SCOPUS_IDENTIFIER_TYPE = "SCP";
    private static final Set<String> IGNORED_FIELDS = readIgnoredFields();
    private final DocTp document;
    private final AbstractsTp abstractsTp;
    private static final String ISSN_DELIMINETER = "-";
    private final URI doi;
    private CitationtypeAtt citationtypeAtt;
    private final String srcType;

    public ScopusGenerator() {
        this.doi = randomDoi();
        this.minimumSequenceNumber = 1;
        this.abstractsTp = randomAbstracts();
        this.srcType = ScopusSourceType.JOURNAL.getCode();
        this.document = randomDocument();
    }

    private ScopusGenerator(AbstractsTp abstractsTp) {
        this.doi = randomDoi();
        this.srcType = ScopusSourceType.JOURNAL.getCode();
        this.minimumSequenceNumber = 1;
        this.abstractsTp = abstractsTp;
        this.document = randomDocument();
    }

    private ScopusGenerator(URI doi) {
        this.doi = doi;
        this.minimumSequenceNumber = 1;
        this.srcType = ScopusSourceType.JOURNAL.getCode();
        this.abstractsTp = randomAbstracts();
        this.document = randomDocument();
    }

    private ScopusGenerator(String srcType) {
        this.srcType = srcType;
        this.minimumSequenceNumber = 1;
        this.doi = randomDoi();
        this.abstractsTp = randomAbstracts();
        this.document = randomDocument();
    }

    private ScopusGenerator(CitationtypeAtt citationtypeAtt) {
        this.doi = randomDoi();
        this.citationtypeAtt = citationtypeAtt;
        this.srcType = ScopusSourceType.JOURNAL.getCode();
        this.abstractsTp = randomAbstracts();
        this.document = randomDocument();
    }

    public static ScopusGenerator createWithSpecifiedAbstract(AbstractsTp abstractsTp) {
        return new ScopusGenerator(abstractsTp);
    }

    public static ScopusGenerator createScopusGeneratorWithSpecificDoi(URI doi) {
        return new ScopusGenerator(doi);
    }

    public ScopusGenerator createWithSpecifiedSrcType(String srcType) {
        return new ScopusGenerator(srcType);
    }

    public static ScopusGenerator create(CitationtypeAtt citationtypeAtt) {
        return new ScopusGenerator(citationtypeAtt);
    }

    public DocTp randomDocument() {
        DocTp docTp = new DocTp();
        docTp.setItem(randomItemTp());
        docTp.setMeta(randomMetaTp());
        assertThat(docTp, doesNotHaveEmptyValuesIgnoringFieldsAndClasses(NOT_BEAN_CLASSES, IGNORED_FIELDS));
        return docTp;
    }

    public DocTp getDocument() {
        return document;
    }

    public String toXml() {
        StringWriter xmlWriter = new StringWriter();
        JAXB.marshal(document, xmlWriter);
        return xmlWriter.toString();
    }

    public static String toXml(RichstringWithMMLType serializable) {
        StringWriter xmlWriter = new StringWriter();
        JAXB.marshal(serializable, xmlWriter);
        return xmlWriter.toString();
    }

    private MetaTp randomMetaTp() {
        var meta = new MetaTp();
        meta.setDoi(randomScopusDoi());
        meta.setSrctype(srcType);
        meta.setEid(randomString());
        return meta;
    }

    private String randomScopusDoi() {
        return nonNull(doi)
                   ? new UriWrapper(doi).getPath().removeRoot().toString()
                   : null;
    }

    private ItemTp randomItemTp() {
        var item = new ItemTp();
        item.setItem(randomOriginalItem());
        return item;
    }

    private OrigItemTp randomOriginalItem() {
        var item = new OrigItemTp();
        item.setBibrecord(randomBibRecord());
        item.setProcessInfo(randomProcessInfo());
        return item;
    }

    private static ProcessInfo randomProcessInfo() {
        var processInfo = new ProcessInfo();
        processInfo.setDateSort(randomDateSort());
        return processInfo;
    }

    private static DateSortTp randomDateSort() {
        var date = new DateSortTp();
        date.setTimestamp(randomGregorianCalendar());
        date.setDay(randomDay().toString());
        date.setMonth(randomMonth().toString());
        date.setYear(randomYear().toString());
        return date;
    }

    private static Integer randomYear() {
        return 1 + randomInteger(currentYear());
    }

    private static int currentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    private static Integer randomMonth() {
        return 1 + randomInteger(12);
    }

    private static Integer randomDay() {
        return 1 + randomInteger(30);
    }

    private static XMLGregorianCalendar randomGregorianCalendar() {
        var calendar = (GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(randomInstant().toEpochMilli());
        return attempt(() -> DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar)).orElseThrow();
    }

    private BibrecordTp randomBibRecord() {
        var bibRecord = new BibrecordTp();
        bibRecord.setItemInfo(randomItemInfo());
        bibRecord.setHead(randomHeadTp());
        return bibRecord;
    }

    private HeadTp randomHeadTp() {
        List<?> authorsAndCollaborations = randomAuthorOrCollaborations();
        var head = new HeadTp();
        head.getAuthorGroup().addAll(randomAuthorGroups(authorsAndCollaborations));
        head.setCitationTitle(randomCitationTitle());
        head.setAbstracts(abstractsTp);
        head.setCitationInfo(randomCitationInfo());
        head.setSource(randomSource());
        return head;
    }

    private static SourcetitleTp randomSourceTitle() {
        SourcetitleTp sourcetitleTp = new SourcetitleTp();
        sourcetitleTp.getContent().add(randomString());
        return sourcetitleTp;
    }

    private static SourceTp randomSource() {
        SourceTp sourceTp = new SourceTp();
        sourceTp.setArticleNumber(randomString());
        sourceTp.setSourcetitle(randomSourceTitle());
        sourceTp.setArticleNumber(randomString());
        sourceTp.getIssn().addAll(randomIssnTypes());
        sourceTp.getPublisher().add(randomPublisher());
        return sourceTp;
    }

    private static Collection<? extends IssnTp> randomIssnTypes() {
        List<IssnTp> issnTpList = new ArrayList<>();
        if (randomBoolean()) {
            issnTpList.add(randomIssnType(ScopusConstants.ISSN_TYPE_ELECTRONIC));
        }
        if (randomBoolean()) {
            issnTpList.add(randomIssnType(ScopusConstants.ISSN_TYPE_PRINT));
        }
        if (randomBoolean()) {
            issnTpList.add(randomIssnType(randomString()));
        }
        return issnTpList;
    }

    private static IssnTp randomIssnType(String issnType) {
        IssnTp issnTp = new IssnTp();
        //the issn from scopus xml comes without "-".
        var issnCode = randomIssn().replace(ISSN_DELIMINETER, "");
        issnTp.setContent(issnCode);
        issnTp.setType(issnType);
        return issnTp;
    }

    private static PublisherTp randomPublisher() {
        var publisher = new PublisherTp();
        publisher.setPublishername(randomString());
        return publisher;
    }

    private static Collection<? extends AuthorGroupTp> randomAuthorGroups(List<?> authorsAndCollaborations) {
        int maxNumberOfAuthorGroups = 200;
        return IntStream.range(0, randomInteger(maxNumberOfAuthorGroups) + 1)
            .boxed()
            .map(ignored -> randomAuthorGroup(authorsAndCollaborations))
            .collect(Collectors.toList());
    }

    private List<?> randomAuthorOrCollaborations() {
        int maxNumbersOfAuthors = 200;
        return IntStream.range(0, randomInteger(maxNumbersOfAuthors) + 1)
            .boxed()
            .map(index -> randomAuthorOrCollaboration())
            .collect(Collectors.toList());
    }

    private static AuthorGroupTp randomAuthorGroup(List<?> authorsAndCollaborations) {
        var authorGroup = new AuthorGroupTp();
        authorGroup.getAuthorOrCollaboration()
            .addAll(randomSubsetRandomAuthorsOrCollaborations(authorsAndCollaborations));
        return authorGroup;
    }

    private static List<?> randomSubsetRandomAuthorsOrCollaborations(List<?> authorsAndCollaborations) {
        int min = 0;
        var numbersOfAuthorOrCollaborations = randomInteger(authorsAndCollaborations.size());
        Collections.shuffle(authorsAndCollaborations);
        return authorsAndCollaborations.subList(min, numbersOfAuthorOrCollaborations);
    }

    private Object randomAuthorOrCollaboration() {
        var shouldReturnAuthorTyp = randomBoolean();
        return shouldReturnAuthorTyp ? randomAuthorTp() : randomCollaborationTp();
    }

    private String generateSequenceNumber() {
        var maxGapInSequenceNumber = 200;
        var sequenceNumber = getMinimumSequenceNumber() + randomInteger(maxGapInSequenceNumber) + 1;
        setMinimumSequenceNumber(sequenceNumber);
        return Integer.toString(sequenceNumber);
    }

    private static String randomOrcid() {
        var shouldCreateOrcid = randomBoolean();
        return shouldCreateOrcid ? randomPotentialOrcidUriString() : null;
    }

    private static String randomPotentialOrcidUriString() {
        return randomBoolean() ? ORCID_DOMAIN_URL + randomString() : randomString();
    }

    private CollaborationTp randomCollaborationTp() {
        var collaborationTp = new CollaborationTp();
        collaborationTp.setIndexedName(randomString());
        collaborationTp.setSeq(generateSequenceNumber());
        return collaborationTp;
    }

    private AuthorTp randomAuthorTp() {
        var authorTp = new AuthorTp();
        authorTp.setOrcid(randomOrcid());
        authorTp.setAuid(randomString());
        authorTp.setSeq(generateSequenceNumber());
        PersonalnameType personalnameType = randomPersonalnameType();
        authorTp.setPreferredName(personalnameType);
        authorTp.setIndexedName(personalnameType.getIndexedName());
        authorTp.setGivenName(personalnameType.getGivenName());
        authorTp.setSurname(personalnameType.getSurname());
        return authorTp;
    }

    private CorrespondenceTp createCorrespondenceTp(AuthorTp authorTp) {
        var correspondenceTp = new CorrespondenceTp();
        correspondenceTp.setPerson(createPersonalnameType(authorTp));
        return correspondenceTp;
    }

    private PersonalnameType createPersonalnameType(AuthorTp authorTp) {
        var personalnameType = new PersonalnameType();
        personalnameType.setIndexedName(authorTp.getIndexedName());
        personalnameType.setGivenName(authorTp.getGivenName());
        personalnameType.setSurname(authorTp.getSurname());
        return personalnameType;
    }

    private static PersonalnameType randomPersonalnameType() {
        var personalNameType = new PersonalnameType();
        personalNameType.setIndexedName(randomString());
        personalNameType.setGivenName(randomString());
        personalNameType.setSurname(randomString());
        return personalNameType;
    }

    private CitationInfoTp randomCitationInfo() {
        var citationInfo = new CitationInfoTp();
        citationInfo.setAuthorKeywords(randomAuthorKeywordsTp());
        citationInfo.getCitationType().add(createCitationType());
        return citationInfo;
    }

    private CitationTypeTp createCitationType() {
        var citationType = new CitationTypeTp();
        citationType.setCode(createCitationTypeAtt());
        return citationType;
    }

    private CitationtypeAtt createCitationTypeAtt() {
        return nonNull(citationtypeAtt) ? citationtypeAtt : randomSupportedCitationType();
    }

    //TODO: enrich method when we are supporting more citationTypes
    private CitationtypeAtt randomSupportedCitationType() {
        return CitationtypeAtt.AR;
    }

    private static AuthorKeywordsTp randomAuthorKeywordsTp() {
        var authorKeywords = new AuthorKeywordsTp();
        authorKeywords.getAuthorKeyword().addAll(randomAuthorKeywords());
        return authorKeywords;
    }

    private static List<AuthorKeywordTp> randomAuthorKeywords() {
        return smallStream().map(ignored -> randomAuthorKeyword()).collect(Collectors.toList());
    }

    private static AuthorKeywordTp randomAuthorKeyword() {
        var authorKeyword = new AuthorKeywordTp();

        authorKeyword.setLang(randomScopusLanguageCode());
        authorKeyword.setOriginal(randomYesOrNo());
        authorKeyword.setPerspective(randomString());
        authorKeyword.getContent().addAll(randomSerializables());
        return authorKeyword;
    }

    private static AbstractsTp randomAbstracts() {
        var abstracts = new AbstractsTp();
        abstracts.getAbstract().addAll(randomAbstractsList());
        return abstracts;
    }

    private static List<AbstractTp> randomAbstractsList() {
        return smallStream().map(ignored -> randomAbstract()).collect(Collectors.toList());
    }

    private static AbstractTp randomAbstract() {
        var abstractTp = new AbstractTp();

        abstractTp.setLang(randomScopusLanguageCode());
        abstractTp.setOriginal(randomYesOrNo());
        abstractTp.setSource(randomString());
        abstractTp.setPublishercopyright(randomPublisherCopyrightTp());
        return abstractTp;
    }

    private static PublishercopyrightTp randomPublisherCopyrightTp() {
        var copyright = new PublishercopyrightTp();
        copyright.getContent().addAll(randomSerializables());
        return copyright;
    }

    private static YesnoAtt randomYesOrNo() {
        return randomElement(YesnoAtt.values());
    }

    private static CitationTitleTp randomCitationTitle() {
        var citationTitle = new CitationTitleTp();
        citationTitle.getShortTitle().addAll(randomShortTitles());
        citationTitle.getTitletext().add(randomOriginalTitle());
        citationTitle.getTitletext().addAll(randomNonOriginalTitles());
        return citationTitle;
    }

    private static Collection<? extends TitletextTp> randomNonOriginalTitles() {
        return smallStream().map(ignored -> randomNonOriginalTitle()).collect(Collectors.toList());
    }

    private static TitletextTp randomOriginalTitle() {
        return randomTitle(YesnoAtt.Y);
    }

    private static TitletextTp randomNonOriginalTitle() {
        return randomTitle(YesnoAtt.N);
    }

    private static TitletextTp randomTitle(YesnoAtt n) {
        var titleText = new TitletextTp();
        titleText.setOriginal(n);
        titleText.setLang(randomScopusLanguageCode());
        titleText.setPerspective(randomString());
        titleText.setLang(randomScopusLanguageCode());
        titleText.getContent().addAll(randomSerializables());
        return titleText;
    }

    private static List<ShortTitle> randomShortTitles() {
        return smallStream().map(ignored -> randomShortTitle()).collect(Collectors.toList());
    }

    private static ShortTitle randomShortTitle() {
        ShortTitle shortTitle = new ShortTitle();
        shortTitle.setLang(randomScopusLanguageCode());
        shortTitle.getContent().addAll(randomSerializables());
        return shortTitle;
    }

    private static List<Serializable> randomSerializables() {
        return randomStrings();
    }

    private static List<Serializable> randomStrings() {
        return smallStream().map(ignored -> randomString()).collect(Collectors.toList());
    }

    private static String randomScopusLanguageCode() {
        return randomElement(LanguageConstants.ALL_LANGUAGES).getIso6391Code();
    }

    private static ItemInfoTp randomItemInfo() {
        var itemInfo = new ItemInfoTp();
        itemInfo.setItemidlist(randomItemIdList());
        return itemInfo;
    }

    private static ItemidlistTp randomItemIdList() {
        var list = new ItemidlistTp();
        list.getItemid().addAll(randomItemIdTps());
        return list;
    }

    private static List<ItemidTp> randomItemIdTps() {
        var list = smallStream().map(ignored -> randomItemidTp()).collect(Collectors.toList());
        list.addAll(randomScopusIdentifiers());
        return list;
    }

    private static List<ItemidTp> randomScopusIdentifiers() {
        return smallStream().map(ignored -> randomScopusIdentifier()).collect(Collectors.toList());
    }

    private static ItemidTp randomScopusIdentifier() {
        var scopusIdentifier = new ItemidTp();
        scopusIdentifier.setIdtype(SCOPUS_IDENTIFIER_TYPE);
        scopusIdentifier.setValue(randomString());
        return scopusIdentifier;
    }

    private static ItemidTp randomItemidTp() {
        var itemIdtp = new ItemidTp();
        itemIdtp.setIdtype(randomString());
        itemIdtp.setValue(randomString());
        return itemIdtp;
    }

    private static Stream<Integer> smallStream() {
        return IntStream.range(0, 1 + randomInteger(SMALL_NUMBER)).boxed();
    }

    private static Set<String> readIgnoredFields() {
        return new HashSet<>(IoUtils.linesfromResource(Path.of("conversion", "ignoredScopusFields.txt")));
    }

    private int getMinimumSequenceNumber() {
        return minimumSequenceNumber;
    }

    private void setMinimumSequenceNumber(int minimumSequenceNumber) {
        this.minimumSequenceNumber = minimumSequenceNumber;
    }

    public void setCorrespondence(AuthorTp authorTp) {
        var correspondenceTp = createCorrespondenceTp(authorTp);
        document.getItem().getItem().getBibrecord().getHead().getCorrespondence().add(correspondenceTp);
    }

    public void setJournalInfo(String volume, String issue, String pages) {
        ObjectFactory factory = new ObjectFactory();
        VolissTp volissTp = factory.createVolissTp();
        volissTp.setVolume(volume);
        volissTp.setIssue(issue);
        JAXBElement<VolissTp> volisspagTpVoliss = factory.createVolisspagTpVoliss(volissTp);
        VolisspagTp volisspagTp = factory.createVolisspagTp();
        volisspagTp.getContent().add(volisspagTpVoliss);
        PagerangeTp pagerangeTp = factory.createPagerangeTp();
        JAXBElement<PagerangeTp> volisspagTpPagerange = factory.createVolisspagTpPagerange(pagerangeTp);
        pagerangeTp.setFirst("0");
        pagerangeTp.setLast(pages);
        volisspagTp.getContent().add(volisspagTpPagerange);
        document.getItem().getItem().getBibrecord().getHead().getSource().setVolisspag(volisspagTp);
    }
}