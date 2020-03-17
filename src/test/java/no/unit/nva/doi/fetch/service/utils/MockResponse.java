package no.unit.nva.doi.fetch.service.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import no.bibsys.aws.tools.JsonUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

/**
 * Mockito has problems creating a mock of the class {@link javax.ws.rs.core.Response}.
 */
public class MockResponse extends Response {

    public static final String AuthorizationHeaderValue = "AuthorizationHeader";
    public static final String ContentLocationHeaderValue = "ContentLocationHeader";
    public static final ObjectNode mockJsonObject = JsonUtils.jsonParser.createObjectNode();

    @Override
    public int getStatus() {
        return HttpStatus.SC_OK;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put(HttpHeaders.AUTHORIZATION, Collections.singletonList(AuthorizationHeaderValue));
        map.put(HttpHeaders.CONTENT_LOCATION, Collections.singletonList(ContentLocationHeaderValue));
        return map;
    }

    @Override
    public StatusType getStatusInfo() {
        return null;
    }

    @Override
    public Object getEntity() {
        return mockJsonObject;
    }

    @Override
    public <T> T readEntity(Class<T> entityType) {
        return (T) getEntity();
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType) {
        return null;
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        return null;
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        return null;
    }

    @Override
    public boolean hasEntity() {
        return false;
    }

    @Override
    public boolean bufferEntity() {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public MediaType getMediaType() {
        return null;
    }

    @Override
    public Locale getLanguage() {
        return null;
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public Set<String> getAllowedMethods() {
        return null;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return null;
    }

    @Override
    public EntityTag getEntityTag() {
        return null;
    }

    @Override
    public Date getDate() {
        return null;
    }

    @Override
    public Date getLastModified() {
        return null;
    }

    @Override
    public URI getLocation() {
        return null;
    }

    @Override
    public Set<Link> getLinks() {
        return null;
    }

    @Override
    public boolean hasLink(String relation) {
        return false;
    }

    @Override
    public Link getLink(String relation) {
        return null;
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return null;
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return null;
    }

    @Override
    public String getHeaderString(String name) {
        return null;
    }
}
