package de.melnichuk.alexa_nespresso.alexa;

import com.amazon.speech.speechlet.SpeechletV2;

import java.util.function.Consumer;

/**
 * defines contract for handling alexa request in a asynchronous manner.
 */
public interface AsyncSpeechletRequestHandler {
    void handleSpeechletCall(SpeechletV2 speechlet, byte[] serializedSpeechletRequest, Consumer<byte[]> consumer) throws Exception;
}
