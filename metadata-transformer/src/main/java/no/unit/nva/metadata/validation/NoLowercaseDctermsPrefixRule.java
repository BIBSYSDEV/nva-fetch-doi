package no.unit.nva.metadata.validation;

import org.apache.any23.validator.DOMDocument;
import org.apache.any23.validator.Rule;
import org.apache.any23.validator.RuleContext;
import org.apache.any23.validator.ValidationReport.IssueLevel;
import org.apache.any23.validator.ValidationReportBuilder;
import org.w3c.dom.Node;

public class NoLowercaseDctermsPrefixRule extends NoLowercasePrefix implements Rule {

    @Override
    public String getHRName() {
        return "no-lowercase-dcterms-prefix-in-metadata-rule";
    }

    @Override
    public boolean applyOn(DOMDocument document, RuleContext<?> context,
                           ValidationReportBuilder validationReportBuilder) {
        for (Node node : document.getNodes("/HTML/HEAD/META")) {
            Node nameAttributeNode = getNameAttributeNode(node);
            if (containsLowercaseDctermsPrefix(nameAttributeNode)) {
                validationReportBuilder.reportIssue(IssueLevel.ERROR, "Lowercase dcterms prefix in metadata.",
                    nameAttributeNode);
                return true;
            }
        }
        return false;
    }
}