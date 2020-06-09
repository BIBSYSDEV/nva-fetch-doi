package no.unit.nva.doi.fetch.exceptions;

import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.doi.fetch.service.PublicationPersistenceService;
import no.unit.nva.doi.transformer.exception.MissingClaimException;
import org.zalando.problem.Status;

public class ExceptionsMap {

    private ExceptionsMap() {

    }

    public static Map<String, Status> createExceptionMap() {
        Map<String, Status> exceptionMap = new ConcurrentHashMap<>();
        exceptionMap.put(MetadataNotFoundException.class.getName(), BAD_GATEWAY);
        exceptionMap.put(NoContentLocationFoundException.class.getName(), BAD_GATEWAY);
        exceptionMap.put(TransformFailedException.class.getName(), BAD_GATEWAY);
        exceptionMap.put(InsertPublicationException.class.getName(), BAD_GATEWAY);
        exceptionMap.put(PublicationPersistenceService.class.getName(),BAD_GATEWAY);
        exceptionMap.put(MalformedRequestException.class.getName(), BAD_REQUEST);
        exceptionMap.put(MissingClaimException.class.getName(), BAD_REQUEST);
        return exceptionMap;
    }

}
