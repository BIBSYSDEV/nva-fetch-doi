package no.unit.nva.doi.fetch;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public class FetchDoiCommonsTestConfig {

    public static final ObjectMapper fetchDoiTestingObjectMapper = JsonUtils.dtoObjectMapper;

    private FetchDoiCommonsTestConfig(){

    }

}
