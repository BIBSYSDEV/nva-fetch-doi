package no.unit.nva.metadata.service;

import org.apache.any23.extractor.ExtractionContext;
import org.apache.any23.writer.TripleHandler;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

public class ModelTripleHandler implements TripleHandler {

  private final Model model = new LinkedHashModel();

  public Model getModel() {
    return model;
  }

  @Override
  public void startDocument(IRI documentIri) {
    // No document level state to track
  }

  @Override
  public void openContext(ExtractionContext context) {
    // No extractor context state to track
  }

  @Override
  public void receiveTriple(
      Resource subject, IRI predicate, Value object, IRI graph, ExtractionContext context) {
    model.add(subject, predicate, object);
  }

  @Override
  public void receiveNamespace(String prefix, String uri, ExtractionContext context) {
    model.setNamespace(prefix, uri);
  }

  @Override
  public void closeContext(ExtractionContext context) {
    // No extractor context state to track
  }

  @Override
  public void endDocument(IRI documentIri) {
    // No document level state to track
  }

  @Override
  public void setContentLength(long contentLength) {
    // Content length is irrelevant for model collection
  }

  @Override
  public void close() {
    // Nothing to release
  }
}
