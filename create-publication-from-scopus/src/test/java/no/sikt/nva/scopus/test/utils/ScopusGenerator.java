package no.sikt.nva.scopus.test.utils;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFieldsAndClasses;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.xml.datatype.XMLGregorianCalendar;
import no.scopus.generated.BibrecordTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.ItemInfoTp;
import no.scopus.generated.ItemTp;
import no.scopus.generated.ItemidTp;
import no.scopus.generated.ItemidlistTp;
import no.scopus.generated.OrigItemTp;
import nva.commons.core.ioutils.IoUtils;

public final class ScopusGenerator {

    public static final Set<Class<?>> NOT_BEAN_CLASSES = Set.of(XMLGregorianCalendar.class);
    private static final Set<String> IGNORED_FIELDS = readIgnoredFields();

    public ScopusGenerator() {

    }

    public static DocTp randomDocument() {
        DocTp docTp = new DocTp();
        docTp.setItem(randomItemTp());
        assertThat(docTp, doesNotHaveEmptyValuesIgnoringFieldsAndClasses(NOT_BEAN_CLASSES, IGNORED_FIELDS));
        return docTp;
    }

    private static ItemTp randomItemTp() {
        var item = new ItemTp();
        item.setItem(randomOriginalItem());
        return item;
    }

    private static OrigItemTp randomOriginalItem() {
        var item = new OrigItemTp();
        item.setBibrecord(randomBibRecord());
        return item;
    }

    private static BibrecordTp randomBibRecord() {
        var bibRecord = new BibrecordTp();
        bibRecord.setItemInfo(randomItemInfo());
        return bibRecord;
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
        return smallList().map(ignored -> randomItemidTp()).collect(Collectors.toList());
    }

    private static ItemidTp randomItemidTp() {
        var itemIdtp = new ItemidTp();
        itemIdtp.setIdtype(randomString());
        itemIdtp.setValue(randomString());
        return itemIdtp;
    }

    private static Stream<Integer> smallList() {
        return IntStream.range(0, 1 + randomInteger(10))
            .boxed();
    }

    private static Set<String> readIgnoredFields() {
        return new HashSet<>(IoUtils.linesfromResource(Path.of("conversion", "ignoredScopusFields.txt")));
    }
}
