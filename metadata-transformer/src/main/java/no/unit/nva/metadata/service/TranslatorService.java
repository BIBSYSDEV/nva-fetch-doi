package no.unit.nva.metadata.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import no.unit.nva.metadata.validation.NoLowercaseDcPrefixFix;
import no.unit.nva.metadata.validation.NoLowercaseDcPrefixRule;
import no.unit.nva.metadata.validation.NoLowercaseDctermsPrefixFix;
import no.unit.nva.metadata.validation.NoLowercaseDctermsPrefixRule;
import org.apache.any23.Any23;
import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.extractor.html.TagSoupParser;
import org.apache.any23.filter.IgnoreAccidentalRDFa;
import org.apache.any23.filter.IgnoreTitlesOfEmptyDocuments;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.HTTPDocumentSource;
import org.apache.any23.validator.DefaultValidator;
import org.apache.any23.validator.Validator;
import org.apache.any23.validator.ValidatorException;
import org.apache.any23.writer.JSONLDWriter;
import org.apache.any23.writer.ReportingTripleHandler;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;

public class TranslatorService {

    public static final String NVA_USER_AGENT = "NVA-user-agent";
    public static final String FAILED_TO_EXTRACT_TRIPLES_FROM_DOCUMENT = "Failed to extract triples from the document";
    public static final String ERROR_WHEN_VALIDATING_DOCUMENT = "Error when validating the document";
    private final Validator validator;
    private ByteArrayOutputStream outputStream;

    /**
     * Creates a new translator for HTML metadata to RDF.
     */
    public TranslatorService() {
        validator = new DefaultValidator();
        validator.addRule(NoLowercaseDcPrefixRule.class, NoLowercaseDcPrefixFix.class);
        validator.addRule(NoLowercaseDctermsPrefixRule.class, NoLowercaseDctermsPrefixFix.class);
    }

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
            validator.validate(uri, new TagSoupParser(source.openInputStream(), source.getDocumentIRI()).getDOM(),
                true);
            translator.extract(source, handler);
        } catch (TripleHandlerException e) {
            throw new RuntimeException(FAILED_TO_EXTRACT_TRIPLES_FROM_DOCUMENT);
        } catch (ValidatorException e) {
            throw new RuntimeException(ERROR_WHEN_VALIDATING_DOCUMENT);
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
        return new ReportingTripleHandler(
            new IgnoreAccidentalRDFa(new IgnoreTitlesOfEmptyDocuments(rdfWriterHandler), true));
    }
}