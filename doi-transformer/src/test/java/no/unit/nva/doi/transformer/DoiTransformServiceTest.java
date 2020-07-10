package no.unit.nva.doi.transformer;

import static no.unit.nva.doi.transformer.MetadataLocation.CROSSREF_STRING;
import static no.unit.nva.doi.transformer.MetadataLocation.DATACITE_STRING;
import static no.unit.nva.model.util.OrgNumberMapper.UNIT_ORG_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.utils.IoUtils;
import org.junit.jupiter.api.Test;

public class DoiTransformServiceTest {

    public static final String OWNER = "owner";
    public static final Path CROSSREF_JSON_PATH = Path.of("crossref.json");
    public static final Path DATACITE_JSON_PATH = Path.of("datacite_response.json");

    @Test
    public void transFormPublicationReturnsPublicationOnValidCrossrefBody()
        throws URISyntaxException, InvalidIssnException,
               JsonProcessingException {
        DoiTransformService doiTransformService = new DoiTransformService();
        String crossrefBody = IoUtils.stringFromResources(CROSSREF_JSON_PATH);

        Publication publication = doiTransformService.transformPublication(crossrefBody, CROSSREF_STRING, OWNER,
            UNIT_ORG_NUMBER);

        assertNotNull(publication);
        assertEquals(publication.getOwner(), OWNER);
    }

    @Test
    public void transFormPublicationReturnsPublicationOnValidDataciteBody() throws URISyntaxException,
            InvalidIssnException, JsonProcessingException {
        DoiTransformService doiTransformService = new DoiTransformService();
        String crossrefBody = IoUtils.stringFromResources(DATACITE_JSON_PATH);

        Publication publication = doiTransformService.transformPublication(crossrefBody, DATACITE_STRING, OWNER,
            UNIT_ORG_NUMBER);

        assertNotNull(publication);
        assertEquals(publication.getOwner(), OWNER);
    }
}
