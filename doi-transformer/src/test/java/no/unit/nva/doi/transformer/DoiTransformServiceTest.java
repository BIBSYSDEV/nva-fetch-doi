package no.unit.nva.doi.transformer;

import static no.unit.nva.doi.transformer.MetadataLocation.CROSSREF_STRING;
import static no.unit.nva.doi.transformer.MetadataLocation.DATACITE_STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.stream.IntStream;

import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.doi.DoiConverter;
import org.junit.jupiter.api.Test;

public class DoiTransformServiceTest {

    public static final String OWNER = "owner";
    public static final URI CUSTOMER_ID = URI.create("http://example.org/publisher/123");
    public static final Path CROSSREF_JSON_PATH = Path.of("crossref.json");
    public static final Path CROSSREF_BOOK_JSON_PATH = Path.of("crossref_sample_book.json");
    public static final Path CROSSREF_SEQUENCE_SAMPLE_JSON_PATH = Path.of("crossref_sample_creator_sequence.json");
    public static final Path DATACITE_JSON_PATH = Path.of("datacite_response.json");
    private static final Path CROSSREF_WITH_XML_ASTRACT_JSON_PATH = Path.of("crossrefWithAbstract.json");

    @Test
    public void transFormPublicationReturnsPublicationOnValidCrossrefBody()
            throws URISyntaxException, InvalidIssnException,
            JsonProcessingException, InvalidIsbnException, UnsupportedDocumentTypeException {
        DoiTransformService doiTransformService = getDoiTransformService();
        String crossrefBody = IoUtils.stringFromResources(CROSSREF_JSON_PATH);

        Publication publication = doiTransformService.transformPublication(crossrefBody, CROSSREF_STRING, OWNER,
            CUSTOMER_ID);

        assertNotNull(publication);
        assertEquals(publication.getOwner(), OWNER);
    }

    @Test
    public void transFormBookPublicationReturnsPublicationOnValidCrossrefBody()
            throws URISyntaxException, InvalidIssnException,
            JsonProcessingException, InvalidIsbnException, UnsupportedDocumentTypeException {
        DoiTransformService doiTransformService = getDoiTransformService();
        String crossrefBody = IoUtils.stringFromResources(CROSSREF_BOOK_JSON_PATH);

        Publication publication = doiTransformService.transformPublication(crossrefBody, CROSSREF_STRING, OWNER,
                CUSTOMER_ID);

        assertNotNull(publication);
        assertEquals(publication.getOwner(), OWNER);
    }

    @Test
    public void transformPublicationWithXmlAbstractReturnsPublicationWithoutXml() throws URISyntaxException,
            InvalidIssnException, JsonProcessingException, InvalidIsbnException, UnsupportedDocumentTypeException {
        DoiTransformService doiTransformService = getDoiTransformService();
        String crossRefBody = IoUtils.stringFromResources(CROSSREF_WITH_XML_ASTRACT_JSON_PATH);
        Publication publication = doiTransformService.transformPublication(crossRefBody, CROSSREF_STRING, OWNER,
                CUSTOMER_ID);
        String abstrakt = publication.getEntityDescription().getAbstract();
        assertFalse(abstrakt.contains("<"));
    }

    @Test
    public void transFormPublicationReturnsPublicationOnValidDataciteBody() throws URISyntaxException,
            InvalidIssnException, JsonProcessingException, InvalidIsbnException, UnsupportedDocumentTypeException {
        DoiTransformService doiTransformService = getDoiTransformService();
        String crossrefBody = IoUtils.stringFromResources(DATACITE_JSON_PATH);

        Publication publication = doiTransformService.transformPublication(crossrefBody, DATACITE_STRING, OWNER,
            CUSTOMER_ID);

        assertNotNull(publication);
        assertEquals(publication.getOwner(), OWNER);
    }

    @Test
    public void transFormPublicationReturnsSequentialEnumeratedContributorsAndIgnoringTextualSequence()
            throws URISyntaxException, InvalidIssnException,
            JsonProcessingException, InvalidIsbnException, UnsupportedDocumentTypeException {
        DoiTransformService doiTransformService = getDoiTransformService();
        String crossrefBody = IoUtils.stringFromResources(CROSSREF_SEQUENCE_SAMPLE_JSON_PATH);
        Publication publication = doiTransformService.transformPublication(crossrefBody, CROSSREF_STRING, OWNER,
                CUSTOMER_ID);

        var contributors = publication.getEntityDescription().getContributors();
        assertNotNull(contributors);
        IntStream.range(0, contributors.size())
                .forEachOrdered(i -> assertEquals(i + 1, contributors.get(i).getSequence()));
    }

    private DoiTransformService getDoiTransformService() {
        DoiConverter doiConverter = new DoiConverter(uri -> true);
        return new DoiTransformService(new DataciteResponseConverter(doiConverter),
                new CrossRefConverter(doiConverter));
    }
}
