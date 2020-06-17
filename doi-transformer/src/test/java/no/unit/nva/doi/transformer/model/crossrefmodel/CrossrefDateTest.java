package no.unit.nva.doi.transformer.model.crossrefmodel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CrossrefDateTest {

    private CrossrefDate date;

    @BeforeEach
    public void init() {
        date = new CrossrefDate();
    }

    @DisplayName("extractEarliestYear returns empty Optional for null values")
    @Test
    public void extractEarliestYearReturns() {
        CrossrefDate date = new CrossrefDate();
        assertThat(date.extractEarliestYear(), is(equalTo(Optional.empty())));
    }

    @DisplayName("extractEarliestYear returns the earliest year for two array entries")
    @Test
    public void extractEarliestYearReturnsTheEarliestYearFor2ArrayEntries() {
        int expectedYear = 2019;
        insertDateArray(expectedYear);
        assertThat(date.extractEarliestYear(), is(equalTo(Optional.of(expectedYear))));
    }

    @DisplayName("extractEarliestYear parses date string when it is in ISO-INSTANT format")
    @Test
    public void extractEarliestTearParsesDateStringWhenItIsInIsoInstantFormat() {
        int expectedYear = 2020;
        insertDateTime(expectedYear);
        assertThat(date.extractEarliestYear().isPresent(), is(true));
        assertThat(date.extractEarliestYear().get(), is(expectedYear));
    }

    @DisplayName("extractEarliestYear parses date string as found in json sample")
    @Test
    public void exrtactEarliestYearParsesDateStringSample() {
        String sample = "2002-07-25T17:19:59Z";
        date.setDateTime(sample);
        assertThat(date.extractEarliestYear().get(), is(2002));
    }

    @DisplayName("extractEarliestYear parses date from millis")
    @Test
    public void extractEarlietYearParsesDateFromTimeStamp() {
        int expectedYear = 2000;
        insertTimestamp(expectedYear);
        assertThat(date.extractEarliestYear().get(), is(equalTo(expectedYear)));
    }

    @DisplayName("extractEarliestYear returns min of mixed dates")
    @Test
    public void extractEarliestYearReturnMinOfMixedDates() {
        int expectedYear = 2020;
        int notExpectedYear1 = expectedYear + 1;
        int notExpectedYear2 = expectedYear + 2;

        insertTimestamp(expectedYear);
        insertDateArray(notExpectedYear1);
        insertDateTime(notExpectedYear2);

        assertThat(date.extractEarliestYear().get(), is(equalTo(expectedYear)));
    }

    @DisplayName("extractEarliestYear returns the earliest year for mixed entries")
    @Test
    public void extractEarliestYearReturnsTheEarliestYearForMixedEntries() {
        int expectedYear = 2019;
        int notExpectedYear = expectedYear + 1;
        int[][] insertedDates = new int[][]{{expectedYear, 1, 1}, {notExpectedYear}};

        CrossrefDate date = new CrossrefDate();
        date.setDateParts(insertedDates);
        assertThat(date.extractEarliestYear(), is(equalTo(Optional.of(expectedYear))));
    }

    private void insertTimestamp(int expectedYear) {
        LocalDateTime beginningOfYear =
            LocalDateTime.of(expectedYear, 1, 1, 0, 0, 0, 0);
        long millis = beginningOfYear.toInstant(ZoneOffset.UTC).toEpochMilli();
        date.setTimestamp(millis);
    }

    private void insertDateTime(int expectedYear) {
        String validIsoInstantString = createValidIsoInstantString(expectedYear);
        date.setDateTime(validIsoInstantString);
    }

    private void insertDateArray(int expectedYear) {
        int notExpectedYear = expectedYear + 1;
        int[][] insertedDates = new int[][]{{expectedYear, 1, 1}, {notExpectedYear}};
        date.setDateParts(insertedDates);
    }

    private String createValidIsoInstantString(int expectedYear) {
        Instant instant = LocalDateTime.of(expectedYear, Month.JANUARY, 1, 0, 0, 0, 0)
                                       .toInstant(ZoneOffset.UTC);
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
