package no.sikt.nva.scopus;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import no.scopus.generated.DocTp;

import java.io.InputStream;

public class PublicationConverter {

    protected DocTp unMarshalXmlToDocTp(InputStream scopusXmlInputSteam ) throws JAXBException{
        JAXBContext jaxbContext     = JAXBContext.newInstance( DocTp.class );
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement element = (JAXBElement) jaxbUnmarshaller.unmarshal(scopusXmlInputSteam);
       return (DocTp) element.getValue();
    }

    public ScopusPublication convert (InputStream scopusXmlInputSteam ) throws JAXBException {
        DocTp docTp = unMarshalXmlToDocTp(scopusXmlInputSteam);
        return new ScopusPublication(docTp);
    }
}
