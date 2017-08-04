package de.melnichuk.alexa_nespresso;

import com.amazon.speech.speechlet.SpeechletRequestHandler;
import com.amazon.speech.speechlet.verifier.SpeechletRequestEnvelopeVerifier;
import de.melnichuk.alexa_nespresso.alexa.AsyncSpeechletRequestHandler;
import de.melnichuk.alexa_nespresso.alexa.AsyncSpeechletRequestHandlerDelegate;
import de.melnichuk.alexa_nespresso.alexa.DefaultNespressoSpeechlet;
import de.melnichuk.alexa_nespresso.alexa.NespressoSpeechlet;
import de.melnichuk.alexa_nespresso.nespresso.NespressoAdapter;
import de.melnichuk.alexa_nespresso.nespresso.VertxNespressoAdapter;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final int _1MB = 1024 * 1024 * 1;

    public static void main(String... args) throws Exception {
        final Vertx vertx = Vertx.vertx();
        final Router router = setupRouter(vertx);

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }

    private static Router setupRouter(Vertx vertx) {
        final NespressoAdapter nespressoAdapter = new VertxNespressoAdapter(vertx);
        final NespressoSpeechlet nespressoSpeechlet = new DefaultNespressoSpeechlet(nespressoAdapter);
        final AsyncSpeechletRequestHandler asyncSpeechletRequestHandler = constructAsyncSpeechletRequestHandler(vertx);

        final Router router = Router.router(vertx);
        router.route(HttpMethod.POST, "/*").handler(BodyHandler.create().setBodyLimit(_1MB));
        router.route(HttpMethod.POST, "/alexa").handler(ctx -> {
            try {
                final String requestJson = ctx.getBodyAsString();
                asyncSpeechletRequestHandler.handleSpeechletCall(nespressoSpeechlet, requestJson.getBytes(), bytes -> {
                    ctx.response().putHeader("Content-Type", "application/json").end(Buffer.buffer(bytes));
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return router;
    }

    private static AsyncSpeechletRequestHandler constructAsyncSpeechletRequestHandler(Vertx vertx) {
        final List<SpeechletRequestEnvelopeVerifier> requestEnvelopeVerifiers = Collections.emptyList();
        final SpeechletRequestHandler speechletRequestHandler = new SpeechletRequestHandler(requestEnvelopeVerifiers);

        return new AsyncSpeechletRequestHandlerDelegate(speechletRequestHandler, (responseConsumer, responseSupplier) -> {
            vertx.executeBlocking((Future<byte[]> future) -> {
                future.complete(responseSupplier.get());
            }, asyncResult -> {
                if (asyncResult.succeeded()) {
                    responseConsumer.accept(asyncResult.result());
                } else {
                    throw new RuntimeException(asyncResult.cause());
                }
            });
        });
    }
}
