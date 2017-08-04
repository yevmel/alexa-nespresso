package de.melnichuk.alexa_nespresso.alexa;

import com.amazon.speech.speechlet.*;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * delegates alexa requests to a SpeechletRequestHandler using a AsyncRunner.
 */
public class AsyncSpeechletRequestHandlerDelegate implements AsyncSpeechletRequestHandler {
    private final SpeechletRequestHandler speechletRequestHandler;
    private final AsyncRunner<byte[]> asyncRunner;

    public AsyncSpeechletRequestHandlerDelegate(SpeechletRequestHandler speechletRequestHandler, AsyncRunner<byte[]> asyncRunner) {
        this.speechletRequestHandler = speechletRequestHandler;
        this.asyncRunner = asyncRunner;
    }

    @Override
    public void handleSpeechletCall(SpeechletV2 speechlet, byte[] serializedSpeechletRequest, Consumer<byte[]> responseConsumer) throws Exception {
        Supplier<byte[]> responseSupplier = () -> {
            try {
                return speechletRequestHandler.handleSpeechletCall(speechlet, serializedSpeechletRequest);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        asyncRunner.runAsync(responseConsumer, responseSupplier);
    }
}
