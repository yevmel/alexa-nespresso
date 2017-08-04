package de.melnichuk.alexa_nespresso.alexa;

import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.Directive;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.dialog.directives.ElicitSlotDirective;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;

import java.util.Arrays;
import java.util.List;

abstract public class SpeechletSupport {

    protected SpeechletResponse constructAskResponse(final String text) {
        final PlainTextOutputSpeech speech = constructPlainTextOutputSpeech(text);
        final Reprompt reprompt = constructRepromt(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt);
    }

    protected SpeechletResponse constructTellResponse(final String text) {
        final PlainTextOutputSpeech speech = constructPlainTextOutputSpeech(text);

        return SpeechletResponse.newTellResponse(speech);
    }

    protected SpeechletResponse constructAskResponseWithElicitSlotDirective(final Slot slot, final String text) {
        final ElicitSlotDirective directive = constructElicitSlotDirective(slot);

        final List<Directive> directives = Arrays.asList(directive);
        final SpeechletResponse response = constructAskResponse(text);
        response.setDirectives(directives);

        return response;
    }

    private ElicitSlotDirective constructElicitSlotDirective(Slot slot) {
        final ElicitSlotDirective directive = new ElicitSlotDirective();
        directive.setSlotToElicit(slot.getName());

        return directive;
    }

    private PlainTextOutputSpeech constructPlainTextOutputSpeech(String text) {
        final PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(text);
        return speech;
    }

    private Reprompt constructRepromt(PlainTextOutputSpeech speech) {
        final Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return reprompt;
    }
}
