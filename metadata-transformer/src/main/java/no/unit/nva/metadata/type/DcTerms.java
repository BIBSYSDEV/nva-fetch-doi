package no.unit.nva.metadata.type;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum DcTerms implements OntologyProperty {
    ABSTRACT("abstract"),
    ACCESS_RIGHTS("accessRights"),
    ACCRUAL_METHOD("accrualMethod"),
    ACCRUAL_PERIODICITY("accrualPeriodicity"),
    ACCRUAL_POLICY("accrualPolicy"),
    ALTERNATIVE("alternative"),
    AUDIENCE("audience"),
    AVAILABLE("available"),
    BIBLIOGRAPHIC_CITATION("bibliographicCitation"),
    CONFORMS_TO("conformsTo"),
    CONTRIBUTOR("contributor"),
    COVERAGE("coverage"),
    CREATED("created"),
    CREATOR("creator"),
    DATE("date"),
    DATE_ACCEPTED("dateAccepted"),
    DATE_COPYRIGHTED("dateCopyrighted"),
    DATE_SUBMITTED("dateSubmitted"),
    DESCRIPTION("description"),
    EDUCATION_LEVEL("educationLevel"),
    EXTENT("extent"),
    FORMAT("format"),
    HAS_FORMAT("hasFormat"),
    HAS_PART("hasPart"),
    HAS_VERSION("hasVersion"),
    IDENTIFIER("identifier"),
    INSTRUCTIONAL_METHOD("instructionalMethod"),
    IS_FORMATOF("isFormatOf"),
    IS_PART_OF("isPartOf"),
    IS_REFERENCED_BY("isReferencedBy"),
    IS_REPLACED_BY("isReplacedBy"),
    IS_REQUIRED_BY("isRequiredBy"),
    ISSUED("issued"),
    IS_VERSION_OF("isVersionOf"),
    LANGUAGE("language"),
    LICENSE("license"),
    MEDIATOR("mediator"),
    MEDIUM("medium"),
    MODIFIED("modified"),
    PROVENANCE("provenance"),
    PUBLISHER("publisher"),
    REFERENCES("references"),
    RELATION("relation"),
    REPLACES("replaces"),
    REQUIRES("requires"),
    RIGHTS("rights"),
    RIGHTS_HOLDER("rightsHolder"),
    SOURCE("source"),
    SPATIAL("spatial"),
    SUBJECT("subject"),
    TABLE_OF_CONTENTS("tableOfContents"),
    TEMPORAL("temporal"),
    TITLE("title"),
    TYPE("type"),
    VALID("valid");

    private static final String DCTERMS_PREFIX = "http://purl.org/dc/terms/";
    public static final String DC = "dc.";
    public static final String DCTERMS = "dcterms.";
    public static final String EMPTY_STRING = "";

    private final String localName;

    DcTerms(String localName) {
        this.localName = localName;
    }

    @Override
    public IRI getIri() {
        return SimpleValueFactory.getInstance().createIRI(DCTERMS_PREFIX, localName);
    }

    public String getMetaTagName() {
        return "DC." + localName;
    }

    public static Optional<DcTerms> getTermByValue(String candidate) {
        String term = replaceInternalPrefixes(candidate);
        return Arrays.stream(values())
                .filter(dcTerm -> dcTerm.localName.equalsIgnoreCase(term))
                .findAny();
    }

    private static String replaceInternalPrefixes(String input) {
        String replaced = input.toLowerCase(Locale.ROOT).replace(DCTERMS, EMPTY_STRING);
        return replaced.replace(DC, EMPTY_STRING);
    }
}
