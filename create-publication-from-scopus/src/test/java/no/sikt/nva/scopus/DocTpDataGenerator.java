package no.sikt.nva.scopus;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringClasses;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import no.scopus.generated.AuthEAddressTp;
import no.scopus.generated.CountriesTp;
import no.scopus.generated.DateTimeTp;
import no.scopus.generated.DateTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.MetaTp;
import no.scopus.generated.OpenAccessType;
import no.scopus.generated.PrefnameauidsTp;
import no.scopus.generated.SubjAreasTp;
import no.scopus.generated.UniqueAuthorTp;
import no.scopus.generated.UpwOaLocationType;
import no.scopus.generated.UpwOpenAccessType;
import no.scopus.generated.YesNoStringTp;
import no.unit.nva.testutils.RandomDataGenerator;
import org.junit.jupiter.api.Test;

class DocTpDataGenerator {

    public static final int SMALL_NUMBER = 10;
    public static final Set<Class<?>> NOT_BEAN_CLASSES = Set.of(XMLGregorianCalendar.class);
    private final String randomCountry = randomString();
    private final String randomUniqueAuthor = randomString();

    @Test
    void generateDocTp() {
        DocTp docTp = new DocTp();
        MetaTp meta = randomMeta();
        docTp.setMeta(meta);
        assertThat(docTp, doesNotHaveEmptyValues());
    }

    private MetaTp randomMeta() {
        MetaTp meta = new MetaTp();
        meta.setAbsavail(randomBigInt());
        meta.setVolume(randomString());
        meta.setAuthorFi(randomString());
        meta.setDoi(randomDoi().toString());
        meta.setCountries(randomCountries());
        meta.setDatesort(randomBigInt());
        meta.setAuthorSurname(randomString());
        meta.setUniqueAuthCount(randomBigInt());
        meta.getUniqueAuthor().addAll(randomUniqueAuthors());
        meta.setTimestamp(randomDateTimeTp());
        meta.setSuppressdummy(randomYesNo());
        meta.setSubjareas(randomSubjAreas());
        meta.setSrctype(randomString());
        meta.setSortYear(randomString());
        meta.setSortYyyymm(randomYyyymm());
        meta.setSdfullavail(randomBigInt());
        meta.getRefId().addAll(randomCollectionOfBigInts());
        meta.setSrctitle(randomString());
        meta.setPui(randomString());
        meta.setPubYear(randomString());
        meta.setPrefnameauids(randomPrefNameUids());
        meta.setPmid(randomString());
        meta.setPii(randomString());
        meta.setOrigLoadDate(randomDateTp());
        meta.setOpenAccess(randomOpenAccess());

        assertThat(meta, doesNotHaveEmptyValuesIgnoringClasses(NOT_BEAN_CLASSES));

        return meta;
    }

    private OpenAccessType randomOpenAccess() {
        var openAccess = new OpenAccessType();
        var upwOpenAccess= randomUpwOpenAccessType();
        openAccess.setUpwOpenAccess(upwOpenAccess);
        return openAccess;
    }

    private UpwOpenAccessType randomUpwOpenAccessType() {
        var openAccessType= new UpwOpenAccessType();
        openAccessType.setUpwBestOaLocation(randomUpwBestOaLocation());
        return openAccessType;
    }

    private UpwOaLocationType randomUpwBestOaLocation() {
        var type = new UpwOaLocationType();
        type.setUpwEvidence(randomString());
        type.setUpwHostType(randomString());
        type.setUpwHostType(randomString());
        type.setUpwIsBest(randomString());
        type.setUpwUrl(randomUri().toString());
        type.setUpwUrlForLandingPage(randomUri().toString());


        return type;
    }

    private DateTp randomDateTp() {
        var date = new DateTp();
        date.setValue(randomGregorianCalendarDate());
        date.setYyyymmdd(randomYyyymmdd());
        return date;
    }

    private PrefnameauidsTp randomPrefNameUids() {
        var ids = new PrefnameauidsTp();
        ids.getPrefnameauid().addAll(randomStringsCollection());
        return ids;
    }

    private List<BigInteger> randomCollectionOfBigInts() {
        return IntStream.range(0, 1 + randomInteger(SMALL_NUMBER))
            .boxed()
            .map(ignored -> BigInteger.valueOf(randomInteger()))
            .collect(Collectors.toList());
    }

    private String randomYyyymm() {
        return String.format("%04d%02d", currentYear(), randomMonth());
    }

    private int randomMonth() {
        return 1 + randomInteger(12);
    }

    private int currentYear() {
        return LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).getYear();
    }

    private SubjAreasTp randomSubjAreas() {
        var areas = new SubjAreasTp();
        areas.getSubjarea().addAll(randomStringsCollection());
        return areas;
    }

    private YesNoStringTp randomYesNo() {
        return randomElement(YesNoStringTp.values());
    }

    private DateTimeTp randomDateTimeTp() {
        var dateTime = new DateTimeTp();
        dateTime.setValue(randomGregorianCalendarDate());
        dateTime.setYyyymmdd(randomYyyymmdd());
        return dateTime;
    }

    private BigInteger randomYyyymmdd() {
        var yyyymm = randomYyyymm();
        int dd = randomDay();
        var yyyymmdd = String.format("%s%02d", yyyymm, dd);
        return BigInteger.valueOf(Long.parseLong(yyyymmdd));
    }

    private int randomDay() {
        return 1 + randomInteger(30);
    }

    private XMLGregorianCalendar randomGregorianCalendarDate() {
        var calendar = (GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(randomInstant().toEpochMilli());
        return attempt(() -> DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar)).orElseThrow();
    }

    private List<UniqueAuthorTp> randomUniqueAuthors() {
        return IntStream.range(0, 10).boxed()
            .map(ingored -> randomoUniqueAuthor())
            .collect(Collectors.toList());
    }

    private UniqueAuthorTp randomoUniqueAuthor() {
        UniqueAuthorTp uniqueAuthorTp = new UniqueAuthorTp();
        uniqueAuthorTp.setAuthId(randomBigInt());
        uniqueAuthorTp.setAuthEAddress(randomAuthAddress());
        uniqueAuthorTp.setSeq(randomString());
        uniqueAuthorTp.setAuthSuffix(randomString());
        uniqueAuthorTp.setAuthIndexedName(randomString());
        uniqueAuthorTp.setAuthSurname(randomString());
        uniqueAuthorTp.setOrcid(randomString());
        uniqueAuthorTp.setAuthInitials(randomString());
        return uniqueAuthorTp;
    }

    private AuthEAddressTp randomAuthAddress() {
        AuthEAddressTp authEAddressTp = new AuthEAddressTp();
        authEAddressTp.setContent(randomString());
        authEAddressTp.setType(randomString());
        return authEAddressTp;
    }

    private BigInteger randomBigInt() {
        return BigInteger.valueOf(randomInteger());
    }

    private CountriesTp randomCountries() {
        var countries = new CountriesTp();
        countries.getCountry().addAll(randomStringsCollection());
        return countries;
    }

    private List<String> randomStringsCollection() {
        return IntStream.range(0, 1 + RandomDataGenerator.randomInteger(SMALL_NUMBER))
            .boxed()
            .map(ingored -> randomString())
            .collect(Collectors.toList());
    }
}
