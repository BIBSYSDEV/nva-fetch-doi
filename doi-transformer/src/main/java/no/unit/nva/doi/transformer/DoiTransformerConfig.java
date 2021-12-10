package no.unit.nva.doi.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class DoiTransformerConfig {

    public static final ObjectMapper doiTransformerObjectMapper = JsonUtils.dtoObjectMapper;

    private DoiTransformerConfig(){

    }

}