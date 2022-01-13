package no.sikt.nva.scopus;

import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class PublicationConverterTest {
    private final static String SCOPUS_XML_0000469852_MEDIUM = "/2-s2.0-0000469852.xml";
    private final static String SCOPUS_XML_85020959898_SMALL = "/2-s2.0-85020959898.xml";
    private final static String SCOPUS_XML_85021139537_LARGE = "/2-s2.0-85021139537.xml";
    private final static String SCOPUS_XML_85019990935_SUP_TITLE = "/2-s2.0-85019990935.xml";
    private final static String SCOPUS_XML_85041615872_SUPPELEMENT_IN_CHANNEL = "/2-s2.0-85041615872.xml";
    //find xml with pagecount in channel: grep -H -r -l  "<volisspag><voliss.*<pagecount.*<\/volisspag>" .
    private final static String SCOPUS_XML_0016778795_PAGECOUNT_IN_CHANNEL ="/2-s2.0-0016778795.xml";

    @Test
    public void testConvert() throws JAXBException {
        PublicationConverter publicationConverter = new PublicationConverter();
        InputStream xmlInput = getClass().getResourceAsStream(SCOPUS_XML_0000469852_MEDIUM);
        ScopusPublication scopusPub = publicationConverter.convert(xmlInput);
        assertEquals("Hjortstam K.", scopusPub.getAuthors().get(0).getAuthorName());
    }

    @Test
    public void testConvertSmallXML() throws JAXBException {
        PublicationConverter publicationConverter = new PublicationConverter();
        InputStream xmlInput = getClass().getResourceAsStream(SCOPUS_XML_85020959898_SMALL);
        ScopusPublication scopusPub = publicationConverter.convert(xmlInput);
        assertEquals("Lie M.", scopusPub.getAuthors().get(0).getAuthorName());
    }

    @Test
    public void testConvertLargeXML() throws JAXBException {
        PublicationConverter publicationConverter = new PublicationConverter();
        InputStream xmlInput = getClass().getResourceAsStream(SCOPUS_XML_85021139537_LARGE);
        ScopusPublication scopusPub = publicationConverter.convert(xmlInput);
        assertEquals("Jackson P.", scopusPub.getAuthors().get(0).getAuthorName());
    }

    @Test
    public void testConvertXMLWithSupTitle() throws JAXBException {
        PublicationConverter publicationConverter = new PublicationConverter();
        InputStream xmlInput = getClass().getResourceAsStream(SCOPUS_XML_85019990935_SUP_TITLE);
        ScopusPublication scopusPub = publicationConverter.convert(xmlInput);
        assertTrue(scopusPub.getLanguages().get(0).getTitle().contains("<sup>") && scopusPub.getLanguages().get(0).getTitle().contains("</sup>") );
    }

    @Test
    public void canConvertPublicationWithSupplementsInChannel() throws JAXBException {
        PublicationConverter publicationConverter = new PublicationConverter();
        InputStream xmlInput = getClass().getResourceAsStream(SCOPUS_XML_85041615872_SUPPELEMENT_IN_CHANNEL);
        ScopusPublication scopusPub = publicationConverter.convert(xmlInput);
        assertEquals("Supplement 1", scopusPub.getChannel().getSupplement() );
    }

    @Test
    public void canConvertPublicationWithPagecountInChannel() throws JAXBException {
        PublicationConverter publicationConverter = new PublicationConverter();
        InputStream xmlInput = getClass().getResourceAsStream(SCOPUS_XML_0016778795_PAGECOUNT_IN_CHANNEL);
        ScopusPublication scopusPub = publicationConverter.convert(xmlInput);
        assertEquals(25, scopusPub.getChannel().getNumberOfPages() );
    }
}