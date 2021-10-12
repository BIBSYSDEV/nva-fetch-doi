package no.unit.nva.doi.fetch;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public class FetchDoiCommonsTestConfig {

    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;

    private FetchDoiCommonsTestConfig(){

    }

}
