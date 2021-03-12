package no.unit.nva.metadata;

import org.apache.any23.validator.DOMDocument;
import org.apache.any23.validator.Rule;
import org.apache.any23.validator.RuleContext;
import org.apache.any23.validator.ValidationReportBuilder;

public class NoLowercaseDctermsPrefixRule extends NoLowercasePrefix implements Rule {

    @Override
    public String getHRName() {
        return "no-lowercase-dcterms-prefix-in-metadata-rule";
    }

    @Override
    public boolean applyOn(DOMDocument document, RuleContext<?> context,
                           ValidationReportBuilder validationReportBuilder) {
        return document.getNodes("/HTML/HEAD/META").stream()
            .map(this::getNameAttributeNode)
            .anyMatch(this::containsLowercaseDctermsPrefix);
    }
}