package no.unit.nva.metadata;

import org.apache.any23.validator.*;
import org.w3c.dom.Node;

import java.util.List;

public class NoLowercaseDCPrefixInMetadataFix implements Fix {

    @Override
    public String getHRName() {
        return "no-lowercase-dc-prefix-in-metadata-fix";
    }

    @Override
    public void execute(Rule rule, RuleContext context, DOMDocument document) {
        List<Node> metas = document.getNodes("/HTML/HEAD/META");
        for (Node meta : metas) {
            Node nameNode = meta.getAttributes().getNamedItem("name");
            if (nameNode != null && nameNode.getTextContent().startsWith("dc.")) {
                nameNode.setTextContent("DC." + nameNode.getTextContent().substring(3));
            }
        }
    }
}