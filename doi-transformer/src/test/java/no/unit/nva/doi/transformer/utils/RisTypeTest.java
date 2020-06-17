package no.unit.nva.doi.transformer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RisTypeTest {

    @DisplayName("getByType returns NON_EXISTING_TYPE when the type does not exist")
    @ParameterizedTest
    @ValueSource(strings = {"", "X", "BELL", "INPL"})
    void getByTypeReturnsNullWhenValueDoesNotExist(String candidate) {
        assertEquals(RisType.NON_EXISTING_TYPE, RisType.getByType(candidate));
    }

    @DisplayName("getByType returns NON_EXISTING_TYPE when the type does not exist")
    @Test
    void getByTypeReturnsNullWhenValueIsNull() {
        assertEquals(RisType.NON_EXISTING_TYPE, RisType.getByType(null));
    }

    @DisplayName("getByType returns RisType when type exists ")
    @ParameterizedTest
    @ValueSource(strings = {"ABST", "ADVS", "ART", "BILL", "BOOK", "CASE", "CHAP", "COMP", "CONF", "CTLG", "DATA",
            "ELEC", "GEN", "HEAR", "ICOMM", "INPR", "JFULL", "JOUR", "MAP", "MGZN", "MPCT", "MUSIC", "NEWS", "PAMP",
            "PAT", "PCOMM", "RPRT", "SER", "SLIDE", "SOUND", "STAT", "THES", "UNBILL", "UNPB", "VIDEO"})
    void getByTypeReturnsRisTypeWhenTypeExists(String input) {
        assertNotEquals(RisType.NON_EXISTING_TYPE, RisType.getByType(input));
    }
}