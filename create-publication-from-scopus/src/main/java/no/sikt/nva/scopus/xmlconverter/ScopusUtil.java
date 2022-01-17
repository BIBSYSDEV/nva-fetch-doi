package no.sikt.nva.scopus.xmlconverter;

import jakarta.xml.bind.JAXBElement;
import no.scopus.generated.InfTp;
import no.scopus.generated.SupTp;
import java.util.List;

public interface ScopusUtil {

    static String extractTextFromDataWithSupInfTags(List<Object> textParts) {
        StringBuilder sb = new StringBuilder();
        textParts.forEach(elm -> {
            if (elm instanceof JAXBElement) {
                JAXBElement elmJaxb = (JAXBElement)elm;
                if (elmJaxb.getValue() instanceof SupTp) {
                    SupTp supTp = (SupTp)elmJaxb.getValue();
                    supTp.getContent().forEach(content -> {
                        sb.append("<sup>");
                        sb.append(content.toString());
                        sb.append("</sup>");
                    });
                }
                else if (elmJaxb.getValue() instanceof InfTp) {
                    InfTp infTp = (InfTp)elmJaxb.getValue();
                    infTp.getContent().forEach(content -> {
                        sb.append("<inf>");
                        sb.append(content.toString());
                        sb.append("</inf>");
                    });
                }
            }
            else {
                sb.append(elm.toString());
            }
        });
        return sb.toString();
    }
}
