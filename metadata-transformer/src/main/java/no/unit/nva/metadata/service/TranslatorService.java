package no.unit.nva.metadata.service;

import org.apache.any23.Any23;
import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.filter.IgnoreAccidentalRDFa;
import org.apache.any23.filter.IgnoreTitlesOfEmptyDocuments;
import org.apache.any23.http.HTTPClient;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.HTTPDocumentSource;
import org.apache.any23.writer.JSONLDWriter;
import org.apache.any23.writer.ReportingTripleHandler;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class TranslatorService {

    public static final String NVA_USER_AGENT = "NVA-user-agent";
    public static final String FAILED_TO_EXTRACT_TRIPLES_FROM_DATA_SOURCE =
            "Failed to extract triples from data source";
    private final Any23 translator;
    private final JSONLDWriter rdfWriterHandler;
    private final HTTPClient httpClient;
    private final ByteArrayOutputStream outputStream;

    /**
     * Creates a new translator for HTML metadata to RDF.
     * @throws IOException On IO exceptions.
     */
    public TranslatorService() throws IOException {
        translator = new Any23();
        outputStream = new ByteArrayOutputStream();
        rdfWriterHandler = new JSONLDWriter(outputStream);
        translator.setHTTPUserAgent(NVA_USER_AGENT);
        httpClient = translator.getHTTPClient();
    }

    /**
     * Returns a ByteArrayOutputStream of the extracted metadata.
     * @return ByteArrayOutputStream
     */
    public ByteArrayOutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Dereference and process metadata from URI to RDF.
     * @param uri URI to be dereferenced.
     * @throws URISyntaxException If the URI is invalid.
     * @throws IOException If the IO fails.
     * @throws ExtractionException If the extraction fails.
     */
    public void get(URI uri) throws URISyntaxException, IOException, ExtractionException {
        DocumentSource source = new HTTPDocumentSource(httpClient, uri.toString());
        try (TripleHandler handler = new ReportingTripleHandler(
                new IgnoreAccidentalRDFa(new IgnoreTitlesOfEmptyDocuments(rdfWriterHandler), true))) {
            translator.extract(source, handler);
        } catch (TripleHandlerException e) {
            throw new RuntimeException(FAILED_TO_EXTRACT_TRIPLES_FROM_DATA_SOURCE);
        }
    }
}