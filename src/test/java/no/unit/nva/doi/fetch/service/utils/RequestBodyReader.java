package no.unit.nva.doi.fetch.service.utils;

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

public class RequestBodyReader {

    /**
     * Extracts the body of a {@link HttpRequest} into a String.
     *
     * @param request the {@link HttpRequest}.
     * @return a String containing the contents of hte body.
     */
    public static String requestBody(HttpRequest request) {
        BodyPublisher bodyPubisher = request.bodyPublisher().get();
        RequestBodySubscriber subscriber = new RequestBodySubscriber();
        bodyPubisher.subscribe(subscriber);
        return subscriber.getBody();
    }

    private static class RequestBodySubscriber implements Subscriber<ByteBuffer> {

        public static final int NUMBER_OF_REQUESTS_TO_PUBLISHER = 1000;

        private ByteBuffer cache = ByteBuffer.allocate(100);

        public String getBody() {
            return new String(cache.array(), StandardCharsets.UTF_8);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(NUMBER_OF_REQUESTS_TO_PUBLISHER);
        }

        @Override
        public void onNext(ByteBuffer item) {
            cache.put(item);
        }

        @Override
        public void onError(Throwable throwable) {
            System.err.println(throwable.getMessage());
        }

        @Override
        public void onComplete() {

        }
    }
}


