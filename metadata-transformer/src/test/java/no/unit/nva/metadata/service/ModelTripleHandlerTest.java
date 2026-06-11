package no.unit.nva.metadata.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

class ModelTripleHandlerTest {

    @Test
    void shouldCollectReceivedTriplesAndNamespacesInModel() {
        var valueFactory = SimpleValueFactory.getInstance();
        var subject = valueFactory.createIRI("https://example.org/subject");
        var predicate = valueFactory.createIRI("https://example.org/predicate");
        var object = valueFactory.createLiteral("object");

        var handler = new ModelTripleHandler();
        handler.startDocument(subject);
        handler.openContext(null);
        handler.receiveTriple(subject, predicate, object, null, null);
        handler.receiveNamespace("example", "https://example.org/", null);
        handler.closeContext(null);
        handler.endDocument(subject);
        handler.setContentLength(0L);
        handler.close();

        var model = handler.getModel();
        assertTrue(model.contains(subject, predicate, object));
        assertTrue(model.getNamespace("example").isPresent());
    }
}
