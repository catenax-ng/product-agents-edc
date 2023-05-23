//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.http;

import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

/**
 * Wrapper that hides OkHttpClient behind a java.net.http.HttpClient
 */
public class HttpClientAdapter extends HttpClient {

    protected final OkHttpClient delegate;

    /**
     * creates a new wrapper
     * @param delegate the real client
     */
    public HttpClientAdapter(OkHttpClient delegate) {
        this.delegate=delegate;
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
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        var builder=new Request.Builder();
        request.headers().map().forEach( (key,values) -> values.forEach( value -> builder.header(key,value)));
        if(request.bodyPublisher().isPresent()) {
            var bodyPublisher = request.bodyPublisher().get();

            var subscriber = new Flow.Subscriber<ByteBuffer>() {

                private ByteBuffer body;
                private Throwable problem;
                private boolean ready;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(bodyPublisher.contentLength());
                }

                @Override
                public void onNext(ByteBuffer item) {
                    if(body==null) {
                        body = item;
                    } else if(item!=null) {
                        ByteBuffer combined=ByteBuffer.allocate(body.capacity()+item.capacity());
                        combined.put(body);
                        combined.put(item);
                        combined.flip();
                        body=combined;
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    problem = throwable;
                }

                @Override
                public void onComplete() {
                    ready = true;
                }
            };

            bodyPublisher.subscribe(subscriber);
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 2000 && !subscriber.ready) {
                Thread.sleep(100);
            }
            if (!subscriber.ready) {
                throw new IOException("Could not wrap request because body cannot be read");
            }
            if (subscriber.problem != null) {
                throw new IOException("Could not wrap request because body cannot be read",subscriber.problem);
            }
            builder.method(request.method(), RequestBody.create(subscriber.body.array(),MediaType.parse(request.headers().firstValue("Content-Type").get())));
        } else {
            builder.method(request.method(), null);
        }
        builder.url(request.uri().toURL());
        Request okRequest=builder.build();
        Call okCall = delegate.newCall(okRequest);
        Response okResponse=okCall.execute();
        return (HttpResponse<T>) new HttpResponseAdapter(okResponse,request);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        throw new UnsupportedOperationException("sendAsync");
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        throw new UnsupportedOperationException("sendAsync");
    }
}
