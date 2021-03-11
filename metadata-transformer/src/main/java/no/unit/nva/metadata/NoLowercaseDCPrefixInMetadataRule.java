package no.unit.nva.metadata;

import org.apache.any23.validator.*;
import org.w3c.dom.Node;

import java.util.List;

public class NoLowercaseDCPrefixInMetadataRule implements Rule {

    @Override
    public String getHRName() {
        return "no-lowercase-dc-prefix-in-metadata-rule";
    }

    @Override
    public boolean applyOn(DOMDocument document, RuleContext<?> context,
                           ValidationReportBuilder validationReportBuilder) {
        List<Node> metas = document.getNodes("/HTML/HEAD/META");
        for (Node meta : metas) {
            Node nameNode = meta.getAttributes().getNamedItem("name");
            if (nameNode != null && nameNode.getTextContent().startsWith("dc.")) {
                validationReportBuilder.reportIssue(ValidationReport.IssueLevel.ERROR,
                        "Lowercase datacite prefix in metadata.", meta);
                return true;
            }
        }

        return false;
    }
}