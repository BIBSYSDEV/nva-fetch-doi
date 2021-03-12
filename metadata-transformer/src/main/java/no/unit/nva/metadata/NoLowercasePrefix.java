package no.unit.nva.metadata;

import org.w3c.dom.Node;

public class NoLowercasePrefix {

    public Node getNameAttributeNode(Node meta) {
        return meta.getAttributes().getNamedItem("name");
    }

    public boolean containsLowercaseDcPrefix(Node nameAttributeNode) {
        return (nameAttributeNode != null && nameAttributeNode.getTextContent().startsWith("dc."));
    }

    public boolean containsLowercaseDctermsPrefix(Node nameAttributeNode) {
        return (nameAttributeNode != null && nameAttributeNode.getTextContent().startsWith("dcterms."));
    }
}