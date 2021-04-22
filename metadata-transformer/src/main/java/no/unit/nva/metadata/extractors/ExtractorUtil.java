package no.unit.nva.metadata.extractors;

import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;

import static java.util.Objects.nonNull;

public final class ExtractorUtil {

    private ExtractorUtil() {

    }

    public static Reference getReference(EntityDescription entityDescription) {
        return nonNull(entityDescription.getReference())
                ? entityDescription.getReference()
                : createReference(entityDescription);
    }

    private static Reference createReference(EntityDescription entityDescription) {
        Reference reference = new Reference();
        entityDescription.setReference(reference);
        return reference;
    }
}
