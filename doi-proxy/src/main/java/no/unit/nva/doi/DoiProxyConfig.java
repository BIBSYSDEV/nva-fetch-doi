package no.unit.nva.doi;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class DoiProxyConfig {

    public static final ObjectMapper doiProxyObjectMapper = JsonUtils.dtoObjectMapper;

    private DoiProxyConfig() {

    }
}
