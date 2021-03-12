package no.unit.nva.metadata.validation;

import org.apache.any23.validator.DOMDocument;
import org.apache.any23.validator.Fix;
import org.apache.any23.validator.Rule;
import org.apache.any23.validator.RuleContext;

public class NoLowercaseDctermsPrefixFix extends NoLowercasePrefix implements Fix {

    public static final String UPPERCASE_DCTERMS_PREFIX = "DCTERMS.";

    @Override
    public String getHRName() {
        return "no-lowercase-dcterms-prefix-in-metadata-fix";
    }

    @Override
    public void execute(Rule rule, RuleContext context, DOMDocument document) {
        document.getNodes("/HTML/HEAD/META").stream()
            .map(this::getNameAttributeNode)
            .filter(this::containsLowercaseDctermsPrefix)
            .forEach(nameAttributeNode -> nameAttributeNode.setTextContent(
                UPPERCASE_DCTERMS_PREFIX + nameAttributeNode.getTextContent()
                    .substring(UPPERCASE_DCTERMS_PREFIX.length())));
    }
}