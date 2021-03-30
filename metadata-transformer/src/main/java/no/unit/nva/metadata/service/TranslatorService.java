package no.unit.nva.metadata.service;

import org.apache.any23.Any23;
import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.filter.IgnoreAccidentalRDFa;
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
    public static final String FAILED_TO_EXTRACT_TRIPLES_FROM_DOCUMENT = "Failed to extract triples from the document";
    public static final boolean SUPPRESS_CSS_TRIPLES = true;
    private ByteArrayOutputStream outputStream;

    /**
     * Returns a ByteArrayOutputStream of the extracted metadata.
     *
     * @return ByteArrayOutputStream
     */
    public ByteArrayOutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Dereference and process metadata from URI to RDF.
     *
     * @param uri URI to be dereferenced.
     * @throws URISyntaxException  If the URI is invalid.
     * @throws IOException         If the IO fails.
     * @throws ExtractionException If the extraction fails.
     */
    public void loadMetadataFromUri(URI uri) throws URISyntaxException, IOException, ExtractionException {
        try (TripleHandler handler = createTripleHandler()) {
            Any23 translator = createAny23();
            DocumentSource source = new HTTPDocumentSource(translator.getHTTPClient(), uri.toString());
            translator.extract(source, handler);
        } catch (TripleHandlerException e) {
            throw new RuntimeException(FAILED_TO_EXTRACT_TRIPLES_FROM_DOCUMENT);
        }
    }

    private Any23 createAny23() {
        Any23 translator = new Any23();
        translator.setHTTPUserAgent(NVA_USER_AGENT);
        return translator;
    }

    private ReportingTripleHandler createTripleHandler() {
        outputStream = new ByteArrayOutputStream();
        JSONLDWriter rdfWriterHandler = new JSONLDWriter(outputStream);
        return new ReportingTripleHandler(new IgnoreAccidentalRDFa(rdfWriterHandler, SUPPRESS_CSS_TRIPLES));
    }
}
