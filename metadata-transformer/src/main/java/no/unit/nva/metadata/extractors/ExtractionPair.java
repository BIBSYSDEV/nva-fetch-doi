package no.unit.nva.metadata.extractors;

import no.unit.nva.model.EntityDescription;
import org.eclipse.rdf4j.model.Statement;

public class ExtractionPair {
    private final Statement statement;
    private final EntityDescription entityDescription;
    private final boolean noAbstract;

    public ExtractionPair(Statement statement,
                          EntityDescription entityDescription,
                          boolean noAbstract) {
        this.statement = statement;
        this.entityDescription = entityDescription;
        this.noAbstract = noAbstract;
    }

    public Statement getStatement() {
        return statement;
    }

    public EntityDescription getEntityDescription() {
        return entityDescription;
    }

    public boolean isNoAbstract() {
        return noAbstract;
    }
}
