package no.unit.nva.doi.transformer.utils;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import no.unit.nva.model.pages.Range;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class StringUtils {

    public static final String SPACE = " ";
    public static final String NOT_DIGIT = "\\D";
    public static final String DOUBLE_WHITESPACE = "\\s\\s";
    public static final String PATH_TO_TEXT = "//text()";

    /**
     * Removes XML-style tags from String.
     *
     * @param input A string input with or without XML tags.
     * @return A string without XML tags
     */
    public static String removeXmlTags(String input) {
        String output = null;
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
        try {
            Document document = createXmlDocumentFromInput(input, documentBuilderFactory);
            NodeList nodeList = getDocumentNodes(document);
            output = textWithoutXmlTags(nodeList);
        } catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        } finally {
            if (isNull(output)) {
                output = input;
            }
        }
        return StringUtils.removeMultipleWhiteSpaces(output).trim();
    }

    private static String textWithoutXmlTags(NodeList nodeList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int counter = 0; counter < nodeList.getLength(); counter++) {
            stringBuilder.append(SPACE).append(nodeList.item(counter).getTextContent());
        }
        return stringBuilder.toString();
    }

    private static NodeList getDocumentNodes(Document document) throws XPathExpressionException {
        XPathFactory xpathFactory = XPathFactory.newDefaultInstance();
        XPath xpath = xpathFactory.newXPath();
        XPathExpression expr = xpath.compile(PATH_TO_TEXT);
        return (NodeList) expr.evaluate(document, XPathConstants.NODESET);
    }

    private static Document createXmlDocumentFromInput(String input, DocumentBuilderFactory documentBuilderFactory)
        throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        try (Reader reader = new StringReader(input)) {
            InputSource inputSource = new InputSource(reader);
            inputSource.setEncoding(StandardCharsets.UTF_8.name());
            Document document = documentBuilder.parse(inputSource);
            document.getDocumentElement().normalize();
            return document;
        }
    }

    /**
     * Replaces all consecutive whitespaces with a single space.
     *
     * @param input A string with or without consecutive whitespaces.
     * @return A string without consecutive whitespaces.
     */
    public static String removeMultipleWhiteSpaces(String input) {
        String buffer = input.trim();
        String result = buffer.replaceAll(DOUBLE_WHITESPACE, SPACE);
        while (!result.equals(buffer)) {
            buffer = result;
            result = buffer.replaceAll(DOUBLE_WHITESPACE, SPACE);
        }
        return result;
    }

    /**
     * Parses "pages" strings to Pages object.
     *
     * @param pages A "pages" string
     * @return A {@link Range} object with the respective pages.
     */
    public static Range parsePage(String pages) {
        if (pages == null) {
            return null;
        }

        String[] array = pages.replaceAll(NOT_DIGIT, SPACE)
                              .strip()
                              .split(SPACE);
        String start = null;
        String end = null;
        if (isNotEmpty(array)) {
            start = array[0];
            if (hasSecondArg(array)) {
                end = array[1];
            }
        }
        return new Range.Builder().withBegin(start).withEnd(end).build();
    }

    private static boolean hasSecondArg(String[] array) {
        return array.length > 1;
    }

    private static boolean isNotEmpty(String[] array) {
        return array.length > 0;
    }
}
