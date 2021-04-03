package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.EntityDescription;
import nva.commons.core.JacocoGenerated;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.nonNull;

public final class TagExtractor {
    public static final Set<IRI> TAG_IRIS = Set.of(DcTerms.COVERAGE.getIri(), DcTerms.TEMPORAL.getIri(),
            DcTerms.SPATIAL.getIri(), DcTerms.SUBJECT.getIri());

    @JacocoGenerated
    private TagExtractor() {

    }

    public static void extract(EntityDescription entityDescription, Statement statement) {
        if (isTag(statement.getPredicate())) {
            addTag(entityDescription, statement);
        }
    }

    private static boolean isTag(IRI candidate) {
        return TAG_IRIS.contains(candidate);
    }

    private static void addTag(EntityDescription entityDescription, Statement statement) {
        Value object = statement.getObject();
        if (ExtractorUtil.isNotLiteral(object)) {
            return;
        }
        List<String> tags = new ArrayList<>();
        List<String> existingTags = entityDescription.getTags();
        if (nonNull(existingTags) && !existingTags.isEmpty()) {
            tags.addAll(existingTags);
        }
        tags.add(object.stringValue());
        entityDescription.setTags(List.of(tags.toArray(new String[]{})));
    }
}
