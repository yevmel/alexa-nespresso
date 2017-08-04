package de.melnichuk.alexa_nespresso.alexa;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;

public interface NespressoSpeechlet extends SpeechletV2 {
    SpeechletResponse onItemIntent(Session session, Intent intent) throws Exception;

    SpeechletResponse onCartIntent(Session session, Intent intent) throws Exception;

    SpeechletResponse onHelpIntent(Session session, Intent intent) throws Exception;

    SpeechletResponse onCheckoutIntent(Session session, Intent intent) throws Exception;
}
