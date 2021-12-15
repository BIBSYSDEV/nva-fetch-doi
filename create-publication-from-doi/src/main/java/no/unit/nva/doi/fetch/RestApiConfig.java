package no.unit.nva.doi.fetch;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class RestApiConfig {

    public static final ObjectMapper restServiceObjectMapper = JsonUtils.dtoObjectMapper;

    private RestApiConfig(){

    }

}
