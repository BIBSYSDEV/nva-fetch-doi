package no.sikt.nva.scopus.test.utils;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFieldsAndClasses;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import jakarta.xml.bind.JAXB;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Collection;
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
import no.scopus.generated.AbstractTp;
import no.scopus.generated.AbstractsTp;
import no.scopus.generated.AuthorKeywordTp;
import no.scopus.generated.AuthorKeywordsTp;
import no.scopus.generated.BibrecordTp;
import no.scopus.generated.CitationInfoTp;
import no.scopus.generated.CitationTitleTp;
import no.scopus.generated.CitationTypeTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.DateSortTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.ItemInfoTp;
import no.scopus.generated.ItemTp;
import no.scopus.generated.ItemidTp;
import no.scopus.generated.ItemidlistTp;
import no.scopus.generated.MetaTp;
import no.scopus.generated.OrigItemTp;
import no.scopus.generated.ProcessInfo;
import no.scopus.generated.PublishercopyrightTp;
import no.scopus.generated.RichstringWithMMLType;
import no.scopus.generated.ShortTitle;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.YesnoAtt;
import no.unit.nva.language.LanguageConstants;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;

public final class ScopusGenerator {

    public static final Set<Class<?>> NOT_BEAN_CLASSES = Set.of(XMLGregorianCalendar.class);
    public static final int SMALL_NUMBER = 10;
    public static final String SCOPUS_IDENTIFIER_TYPE = "SCP";
    private static final Set<String> IGNORED_FIELDS = readIgnoredFields();
    private final DocTp document;

    public ScopusGenerator() {
        this.document = randomDocument();
    }

    public static DocTp randomDocument() {
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

    private static MetaTp randomMetaTp() {
        var meta = new MetaTp();
        meta.setDoi(randomScopusDoi());
        meta.setEid(randomString());
        return meta;
    }

    private static String randomScopusDoi() {
        return new UriWrapper(randomDoi()).getPath().removeRoot().toString();
    }

    private static ItemTp randomItemTp() {
        var item = new ItemTp();
        item.setItem(randomOriginalItem());
        return item;
    }

    private static OrigItemTp randomOriginalItem() {
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

    private static BibrecordTp randomBibRecord() {
        var bibRecord = new BibrecordTp();
        bibRecord.setItemInfo(randomItemInfo());
        bibRecord.setHead(randomHeadTp());
        return bibRecord;
    }

    private static HeadTp randomHeadTp() {
        var head = new HeadTp();
        head.setCitationTitle(randomCitationTitle());
        head.setAbstracts(randomAbstracts());
        head.setCitationInfo(randomCitationInfo());

        return head;
    }

    private static CitationInfoTp randomCitationInfo() {
        var citationInfo = new CitationInfoTp();
        citationInfo.setAuthorKeywords(randomAuthorKeywordsTp());
        citationInfo.getCitationType().add(randomCitationType());
        return citationInfo;
    }

    private static CitationTypeTp randomCitationType() {
        var citationType = new CitationTypeTp();
        citationType.setCode(articleCitationTypeAtt());
        return citationType;
    }

    private static CitationtypeAtt articleCitationTypeAtt() {
        return CitationtypeAtt.AR;
    }

    private static CitationTypeTp randumUnsupportedCitationType() {
        var citationType = new CitationTypeTp();
        citationType.setCode(randomUnSupportedCitationTypeAtt());
        return citationType;
    }

    private static CitationtypeAtt randomUnSupportedCitationTypeAtt() {
        List<CitationtypeAtt> citationTypeAttList = new ArrayList<>(
            List.of(CitationtypeAtt.AB, CitationtypeAtt.BK,
                    CitationtypeAtt.BR, CitationtypeAtt.BZ,
                    CitationtypeAtt.CB,
                    CitationtypeAtt.CH, CitationtypeAtt.CP,
                    CitationtypeAtt.CR, CitationtypeAtt.DI,
                    CitationtypeAtt.DP, CitationtypeAtt.ED,
                    CitationtypeAtt.ER, CitationtypeAtt.IP,
                    CitationtypeAtt.LE, CitationtypeAtt.MM,
                    CitationtypeAtt.NO, CitationtypeAtt.PA,
                    CitationtypeAtt.PP, CitationtypeAtt.RE,
                    CitationtypeAtt.RF, CitationtypeAtt.RP,
                    CitationtypeAtt.SH, CitationtypeAtt.ST,
                    CitationtypeAtt.TB, CitationtypeAtt.WP));
        Collections.shuffle(citationTypeAttList);
        return citationTypeAttList.stream().findFirst().get();
    }

    public void replaceSupportedCitationTypeWithUnsuportedCitationType() {
        document
            .getItem()
            .getItem()
            .getBibrecord()
            .getHead()
            .getCitationInfo()
            .getCitationType()
            .set(0, randumUnsupportedCitationType());
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
}
