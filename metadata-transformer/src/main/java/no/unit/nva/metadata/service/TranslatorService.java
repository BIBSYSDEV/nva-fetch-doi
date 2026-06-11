package no.unit.nva.metadata.service;

import org.apache.any23.Any23;
import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.filter.IgnoreAccidentalRDFa;
import org.apache.any23.source.HTTPDocumentSource;
import org.apache.any23.writer.TripleHandlerException;
import org.eclipse.rdf4j.model.Model;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class TranslatorService {

    public static final String NVA_USER_AGENT = "NVA-user-agent";
    public static final String FAILED_TO_EXTRACT_TRIPLES_FROM_DOCUMENT = "Failed to extract triples from the document";
    public static final boolean SUPPRESS_CSS_TRIPLES = true;
    // Any23 2.7 is compiled against rdf4j 4.x; extractors touching rdf4j APIs removed in 5.x
    // (e.g. html-rdfa11) fail with NoSuchFieldError at runtime and must stay disabled.
    private static final String[] METADATA_EXTRACTORS = {"html-head-meta", "html-head-title"};

    /**
     * Dereference a URI and extract its metadata as RDF.
     *
     * @param uri URI to be dereferenced.
     * @return Model containing the extracted triples.
     * @throws URISyntaxException  If the URI is invalid.
     * @throws IOException         If the IO fails.
     * @throws ExtractionException If the extraction fails.
     */
    public Model loadMetadataFromUri(URI uri) throws URISyntaxException, IOException, ExtractionException {
        var modelHandler = new ModelTripleHandler();
        try (var handler = new IgnoreAccidentalRDFa(modelHandler, SUPPRESS_CSS_TRIPLES)) {
            var translator = createAny23();
            var source = new HTTPDocumentSource(translator.getHTTPClient(), uri.toString());
            translator.extract(source, handler);
        } catch (TripleHandlerException e) {
            throw new RuntimeException(FAILED_TO_EXTRACT_TRIPLES_FROM_DOCUMENT, e);
        }
        return modelHandler.getModel();
    }

    private Any23 createAny23() {
        var translator = new Any23(METADATA_EXTRACTORS);
        translator.setHTTPUserAgent(NVA_USER_AGENT);
        return translator;
    }
}
