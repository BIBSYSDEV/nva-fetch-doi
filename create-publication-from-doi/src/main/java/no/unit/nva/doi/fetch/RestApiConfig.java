package no.unit.nva.doi.fetch;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class RestApiConfig {

    public static final ObjectMapper restServiceObjectMapper = JsonUtils.dtoObjectMapper;

    private RestApiConfig(){

    }

}
