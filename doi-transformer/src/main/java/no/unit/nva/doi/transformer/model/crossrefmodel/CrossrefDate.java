package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Parses dates in the following (JSON) format.
 * <pre>
 * {@code
 *
 * {
 *  "date-parts": [
 *      [
 *          2019,
 *          11,
 *          18
 *      ]
 *  ],
 *  "date-time": "2019-11-18T17:57:31Z",
 *  "timestamp": 1574099851510
 *  }
 * }
 * </pre>
 */
@SuppressWarnings("PMD.MethodReturnsInternalArray")
public class CrossrefDate {


    public static final int YEAR_INDEX = 0;
    public static final int MONTH_INDEX = 1;
    public static final int DAY_INDEX = 2;
    public static final int FROM_DATE_INDEX_IN_DATE_ARRAY = 0;

    private static final String SELECT_ZONE_OFFSET_BY_CONSTANT = "";
    @JsonProperty("date-parts")
    private int[][] dateParts; //
    @JsonProperty("date-time")
    private String dateTime;
    @JsonProperty("timestamp")
    private long timestamp;

    @JacocoGenerated
    public int[][] getDateParts() {
        return dateParts;
    }

    public void setDateParts(int[]... input) {
        this.dateParts = input.clone();
    }

    @JacocoGenerated
    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String input) {
        this.dateTime = input;
    }

    @JacocoGenerated
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long input) {
        this.timestamp = input;
    }

    /**
     * Calculates the earliest year from all the dates stored in the object.
     *
     * @return the earliest year.
     */
    public Optional<Integer> extractEarliestYear() {
        List<Integer> allYears = new ArrayList<>();
        extractYearFromDateTime().ifPresent(allYears::add);
        extractYearFromTimeStamp().ifPresent(allYears::add);
        extractYearFromArray().forEach(allYears::add);
        return allYears.stream().min(Integer::compareTo);
    }

    private Stream<Integer> extractYearFromArray() {
        if (dateParts != null) {
            return Arrays.stream(dateParts)
                    .filter(this::hasYear)
                    .map(dateArray -> dateArray[0]);
        }
        return Stream.empty();
    }

    public boolean hasYear(int... dateArray) {
        return dateArray != null && dateArray.length > 0;
    }

    private Optional<Integer> extractYearFromTimeStamp() {
        if (timestamp == 0) {
            // timestamp=0 means 1-1-1970
            return Optional.empty();
        }
        return Optional.of(timestamp)
                .map(Instant::ofEpochMilli)
                .map(inst -> inst.atOffset(ZoneOffset.UTC).getYear());
    }

    private Optional<Integer> extractYearFromDateTime() {

        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.ofOffset(
                SELECT_ZONE_OFFSET_BY_CONSTANT, ZoneOffset.UTC));
        return Optional.ofNullable(this.dateTime)
                .map(d -> LocalDateTime.parse(this.dateTime, formatter))
                .map(LocalDateTime::getYear);
    }

    /**
     * Converts crossrefDate to Instant if timestamp or needed dateparts are present.
     * @return Instant of time given in either timestamp or dateparts[0]
     */
    public Instant toInstant() {
        if (isaTimestamp()) {
            return Instant.ofEpochMilli(getTimestamp());
        } else if (isaCompleteDate()) {
            return createInstant();
        } else {
            return null;
        }
    }

    private boolean isaTimestamp() {
        return getTimestamp() != 0;
    }

    private Instant createInstant() {
        LocalDate date = LocalDate.of(
                dateParts[FROM_DATE_INDEX_IN_DATE_ARRAY][YEAR_INDEX],
                dateParts[FROM_DATE_INDEX_IN_DATE_ARRAY][MONTH_INDEX],
                dateParts[FROM_DATE_INDEX_IN_DATE_ARRAY][DAY_INDEX]);
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private boolean isaCompleteDate() {
        return dateParts.length > 0 && dateParts[FROM_DATE_INDEX_IN_DATE_ARRAY].length > 2;
    }

}
