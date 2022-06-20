package no.sikt.nva.scopus.conversion.model.cristin;

import java.util.Map;
import nva.commons.core.JacocoGenerated;

public class Affiliation {

    private final Institution institution;

    private final Unit unit;

    private final boolean active;

    private final Map<String, String> position;

    @JacocoGenerated
    public Affiliation(Institution institution, Unit unit, boolean active, Map<String, String> position) {
        this.institution = institution;
        this.unit = unit;
        this.active = active;
        this.position = position;
    }

    @JacocoGenerated
    public Institution getInstitution() {
        return institution;
    }

    @JacocoGenerated
    public Unit getUnit() {
        return unit;
    }

    @JacocoGenerated
    public boolean isActive() {
        return active;
    }

    @JacocoGenerated
    public Map<String, String> getPosition() {
        return position;
    }
}
