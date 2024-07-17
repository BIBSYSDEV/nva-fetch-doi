package no.unit.nva.doi.transformer;

import static no.unit.nva.doi.transformer.MetadataLocation.CROSSREF_STRING;
import static no.unit.nva.doi.transformer.MetadataLocation.DATACITE_STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.file.Path;
import java.util.stream.IntStream;
import no.unit.nva.doi.transformer.utils.InvalidIssnException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.doi.DoiConverter;
import org.junit.jupiter.api.Test;

class DoiTransformServiceTest {

    public static final Path CROSSREF_JSON_PATH = Path.of("crossref.json");
    public static final Path CROSSREF_BOOK_JSON_PATH = Path.of("crossref_sample_book.json");
    public static final Path CROSSREF_SEQUENCE_SAMPLE_JSON_PATH = Path.of("crossref_sample_creator_sequence.json");
    public static final Path DATACITE_JSON_PATH = Path.of("datacite_response.json");
    private static final Path CROSSREF_WITH_XML_ASTRACT_JSON_PATH = Path.of("crossrefWithAbstract.json");
    public static final Path CROSSREF_EDIT_BOOK_JSON_PATH = Path.of("crossref_sample_edited_book.json");

    @Test
    void transFormPublicationReturnsPublicationOnValidCrossrefBody()
        throws JsonProcessingException, InvalidIssnException {
        DoiTransformService doiTransformService = getDoiTransformService();
        String crossrefBody = IoUtils.stringFromResources(CROSSREF_JSON_PATH);

        var publication = doiTransformService.transformPublication(crossrefBody, CROSSREF_STRING);

        assertNotNull(publication);
        // TODO: assertEquals(OWNER, publication.getResourceOwner().getOwner().getValue());
    }

    @Test
    void transFormBookPublicationReturnsPublicationOnValidCrossrefBody()
        throws InvalidIssnException, JsonProcessingException {

        DoiTransformService doiTransformService = getDoiTransformService();
        String crossrefBody = IoUtils.stringFromResources(CROSSREF_BOOK_JSON_PATH);

        var publication = doiTransformService.transformPublication(crossrefBody, CROSSREF_STRING);

        assertNotNull(publication);
        // TODO: assertEquals(OWNER, publication.getResourceOwner().getOwner().getValue());
    }

    @Test
    void transFormEditBookPublicationReturnsPublicationOnValidCrossrefBody()
        throws InvalidIssnException, JsonProcessingException {
        DoiTransformService doiTransformService = getDoiTransformService();
        String crossrefBody = IoUtils.stringFromResources(CROSSREF_EDIT_BOOK_JSON_PATH);

        var publication = doiTransformService.transformPublication(crossrefBody, CROSSREF_STRING);

        assertNotNull(publication);
        // TODO: assertEquals(OWNER, publication.getResourceOwner().getOwner().getValue());
    }

    @Test
    void transformPublicationWithXmlAbstractReturnsPublicationWithoutXml()
        throws InvalidIssnException, JsonProcessingException {

        DoiTransformService doiTransformService = getDoiTransformService();
        String crossRefBody = IoUtils.stringFromResources(CROSSREF_WITH_XML_ASTRACT_JSON_PATH);

        var publication = doiTransformService.transformPublication(crossRefBody, CROSSREF_STRING);

        String publicationAbstract = publication.getEntityDescription().getMainAbstract();
        assertFalse(publicationAbstract.contains("<"));
    }

    @Test
    void transFormPublicationReturnsPublicationOnValidDataciteBody()
        throws InvalidIssnException, JsonProcessingException {

        DoiTransformService doiTransformService = getDoiTransformService();
        String crossrefBody = IoUtils.stringFromResources(DATACITE_JSON_PATH);

        var publication = doiTransformService.transformPublication(crossrefBody, DATACITE_STRING);

        assertNotNull(publication);
        // TODO: assertEquals(OWNER, publication.getResourceOwner().getOwner().getValue());
    }

    @Test
    void transFormPublicationReturnsSequentialEnumeratedContributorsAndIgnoringTextualSequence()
        throws InvalidIssnException, JsonProcessingException {

        DoiTransformService doiTransformService = getDoiTransformService();
        String crossrefBody = IoUtils.stringFromResources(CROSSREF_SEQUENCE_SAMPLE_JSON_PATH);
        var publication = doiTransformService.transformPublication(crossrefBody, CROSSREF_STRING);

        var contributors = publication.getEntityDescription().getContributors();
        assertNotNull(contributors);
        IntStream.range(0, contributors.size())
            .forEachOrdered(i -> assertEquals(i + 1, contributors.get(i).sequence()));
    }

    private DoiTransformService getDoiTransformService() {
        DoiConverter doiConverter = new DoiConverter(uri -> true);
        return new DoiTransformService(new DataciteResponseConverter(doiConverter),
                                       new CrossRefConverter(doiConverter));
    }
}
