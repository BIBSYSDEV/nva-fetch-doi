package no.unit.nva.doi.fetch;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class RestApiConfig {

    public static final ObjectMapper REST_SERVICE_OBJECT_MAPPER = JsonUtils.dtoObjectMapper;

    private RestApiConfig(){

    }

}
