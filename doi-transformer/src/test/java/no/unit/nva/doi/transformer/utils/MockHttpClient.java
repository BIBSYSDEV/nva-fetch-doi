package no.unit.nva.doi.transformer.utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MockHttpClient<R> extends HttpClient {

    protected HttpRequest httpRequest;
    protected final AbstractHttpResponse<R> response;

    public MockHttpClient(AbstractHttpResponse<R> response) {
        this.response = response;
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    // T must be the same with R
    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                            BodyHandler<T> responseBodyHandler) {

        this.httpRequest = request;
        return CompletableFuture.completedFuture(((HttpResponse<T>) response));
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                            BodyHandler<T> responseBodyHandler,
                                                            PushPromiseHandler<T> pushPromiseHandler) {
        this.httpRequest = request;
        return null;
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return Optional.empty();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.empty();
    }

    @Override
    public Redirect followRedirects() {
        return null;
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return Optional.empty();
    }

    @Override
    public SSLContext sslContext() {
        return null;
    }

    @Override
    public SSLParameters sslParameters() {
        return null;
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return Optional.empty();
    }

    @Override
    public Version version() {
        return null;
    }

    @Override
    public Optional<Executor> executor() {
        return Optional.empty();
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler) {
        this.httpRequest = request;
        return null;
    }
}
