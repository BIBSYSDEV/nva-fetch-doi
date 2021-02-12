package no.unit.nva.doi.fetch.language;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LanguageMapperTest {

    public static final String GARBAGE_INPUT = "xyz";
    private static final URI expected = URI.create("http://lexvo.org/id/iso639-3/eng");
    public static final String TWO_LETTER_ISO = "en";
    public static final String ISO_639_3 = "eng";

    @Test
    public void getUriFromIso639ReturnsUriForTwoLetterLanguageCodes() {
        Optional<URI> uri = LanguageMapper.getUriFromIsoAsOptional(TWO_LETTER_ISO);
        assertThat(uri.isPresent(), is(equalTo(true)));
        assertThat(uri.get(), is(equalTo(expected)));
    }

    @Test
    public void getUriFromIso639ReturnsUriForIso639LanguageCodes() {
        Optional<URI> uri = LanguageMapper.getUriFromIsoAsOptional(ISO_639_3);
        assertThat(uri.isPresent(), is(equalTo(true)));
        assertThat(uri.get(), is(equalTo(expected)));
    }

    @Test
    public void getUriFromIsoThrowsLanguageUriNotFoundExceptionOnGarbageInput() throws LanguageUriNotFoundException {
        LanguageUriNotFoundException exception = assertThrows(LanguageUriNotFoundException.class,
            () -> LanguageMapper.getUriFromIso(GARBAGE_INPUT));

        Assertions.assertEquals(LanguageMapper.URI_NOT_FOUND_ERROR + GARBAGE_INPUT,
            exception.getMessage());

        ;
    }
}