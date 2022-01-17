package no.sikt.nva.scopus.xmlconverter;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import no.scopus.generated.DocTp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;

public class PublicationConverter {
    private static final Logger logger = LoggerFactory.getLogger(PublicationConverter.class);


    protected DocTp unMarshalXmlToDocTp(StringReader scopusXmlInput ) throws JAXBException{
        JAXBContext jaxbContext     = JAXBContext.newInstance( DocTp.class );
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement element = (JAXBElement) jaxbUnmarshaller.unmarshal(scopusXmlInput);
       return (DocTp) element.getValue();
    }

    public ScopusPublication convert (StringReader scopusXmlInput ) throws JAXBException {
        DocTp docTp = unMarshalXmlToDocTp(scopusXmlInput);
        logger.info("DocTP from unmarshaling", docTp);
        return new ScopusPublication(docTp);
    }
}
