package no.unit.nva.doi;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Optional;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoiProxyService {

    public static final String ERROR_READING_METADATA = "Could not get publication metadata.DOI:";
    public static final String SPACE = " ";
    public static final String GETING_DOI_METADATA_INFO_MESSAGE = "getting doi metadata for doi:";

    private final CrossRefClient crossRefClient;
    private final DataciteClient dataciteClient;

    private static final Logger logger = LoggerFactory.getLogger(DoiProxyService.class);

    /**
     * Constructor for DoiProxyService.
     *
     * @param crossRefClient crossRefClient
     * @param dataciteClient dataciteClient
     */
    public DoiProxyService(CrossRefClient crossRefClient, DataciteClient dataciteClient) {
        this.crossRefClient = crossRefClient;
        this.dataciteClient = dataciteClient;
    }

    /**
     * Default constructor for DoiProxyService.
     */
    @JacocoGenerated
    public DoiProxyService() {
        this(new CrossRefClient(HttpClient.newBuilder().build()), new DataciteClient());
    }

    /**
     * Look up metadata for doi url.
     *
     * @param doiUrl              doiUrl
     * @param dataciteContentType dataciteContentType
     * @return metadata and content location
     * @throws MetadataNotFoundException when no metadata found
     * @throws IOException               when error reading from IO
     * @throws URISyntaxException        when malformed doi url
     */
    public MetadataAndContentLocation lookupDoiMetadata(String doiUrl, DataciteContentType dataciteContentType)
        throws MetadataNotFoundException, IOException, URISyntaxException {
        logger.info(GETING_DOI_METADATA_INFO_MESSAGE + doiUrl);
        MetadataAndContentLocation metadataAndContentLocation;
        Optional<MetadataAndContentLocation> crossRefResult = crossRefClient.fetchDataForDoi(doiUrl);
        if (crossRefResult.isEmpty()) {
            metadataAndContentLocation = dataciteClient.fetchMetadata(doiUrl, dataciteContentType);
        } else {
            metadataAndContentLocation = crossRefResult.get();
        }

        if (isNull(metadataAndContentLocation)) {
            throw new MetadataNotFoundException(ERROR_READING_METADATA + SPACE + doiUrl);
        }

        return metadataAndContentLocation;
    }
}
