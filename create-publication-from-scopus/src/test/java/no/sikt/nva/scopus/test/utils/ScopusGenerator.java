package no.sikt.nva.scopus.test.utils;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFieldsAndClasses;
import static org.hamcrest.MatcherAssert.assertThat;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;
import no.scopus.generated.DocTp;
import nva.commons.core.ioutils.IoUtils;

public final class ScopusGenerator {

    public static final Set<Class<?>> NOT_BEAN_CLASSES = Set.of(XMLGregorianCalendar.class);
    private static final Set<String> IGNORED_FIELDS = readIgnoredFields();

    public ScopusGenerator() {

    }

    public static DocTp randomDocument() {
        DocTp docTp = new DocTp();
        assertThat(docTp, doesNotHaveEmptyValuesIgnoringFieldsAndClasses(NOT_BEAN_CLASSES, IGNORED_FIELDS));
        return docTp;
    }

    private static Set<String> readIgnoredFields() {
        return new HashSet<>(IoUtils.linesfromResource(Path.of("conversion", "ignoredScopusFields.txt")));
    }
}
