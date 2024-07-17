package no.unit.nva.doi.transformer.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import no.unit.nva.doi.fetch.commons.publication.model.Pages;
import no.unit.nva.doi.fetch.commons.publication.model.Range;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class StringUtilsTest {

    @DisplayName("removeXMl reuturns String with not XML tags.")
    @Test
    public void removeXmlTagsReturnsStringWithNoXmlTags() {
        String input = "<xmlTag> Hello world<jap>something else</jap> hello again</xmlTag>";
        String expectedOutput = "Hello world something else hello again";
        String actualOutput = StringUtils.removeXmlTags(input);
        assertThat(actualOutput, is(equalTo(expectedOutput)));
    }

    @Test
    @DisplayName("parsePage returns a begin and end page for pages split with dash")
    public void parsePageReturnsABeginAndEndPageForPagesSplitWithDash() {
        String begins = "12";
        String ends = "34";
        String delimiter = "-";
        String pageString = String.join(delimiter, begins, ends);
        Pages expected = new Range(begins, ends);
        Pages actual = StringUtils.parsePage(pageString);
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    @DisplayName("parsePage returns a \"begin\" repeating \"begin\" for page string with as single number")
    public void parsePageReturnsABeginWithoutAnEndPageForPagesBeingASingleNumber() {
        String pagesString = "12";
        Pages expected = new Range(pagesString, pagesString);
        Pages actual = StringUtils.parsePage(pagesString);
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    @DisplayName("parsePage returns a \"begin\" and \"end\" for page strings that have prefix.")
    public void parsePageReturnsABeginAndEndPageForPagesThatHaveAPrefix() {
        String prefix = "p.";
        String begins = "12";
        String ends = "34";
        String delimiter = "-";
        String pageString = prefix + begins + delimiter + ends;
        Pages expected = new Range(begins, ends);
        Pages actual = StringUtils.parsePage(pageString);
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    @DisplayName("parsePage returns a \"begin\" and \"end\" for page strings that have prefix word with space.")
    public void parsePageReturnsABeginAndEndPageForPagesThatHaveAPrefixWordWithSpace() {
        String prefix = "pages ";
        String begins = "12";
        String ends = "34";
        String delimiter = "-";
        String pageString = prefix + begins + delimiter + ends;
        Pages expected = new Range(begins, ends);
        Pages actual = StringUtils.parsePage(pageString);
        assertThat(actual, is(equalTo(expected)));
    }
}
