package de.melnichuk.alexa_nespresso.alexa;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.melnichuk.alexa_nespresso.Environment;
import de.melnichuk.alexa_nespresso.nespresso.*;
import de.melnichuk.alexa_nespresso.nespresso.model.Item;
import de.melnichuk.alexa_nespresso.nespresso.model.Product;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultNespressoSpeechlet extends SpeechletSupport implements NespressoSpeechlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNespressoSpeechlet.class);
    public static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Function<Item, String> ITEM_TO_STRING = item -> item.getQuantity() + " Kapseln " + item.getProduct().getName();
    private static final Function<Map<String, String>, Item> MAP_TO_ITEM = m -> objectMapper.convertValue(m, Item.class);

    private final NespressoAdapter nespressoAdapter;

    public DefaultNespressoSpeechlet(NespressoAdapter nespressoAdapter) {
        this.nespressoAdapter = nespressoAdapter;
    }

    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
        LOGGER.info("session with id={} started.", requestEnvelope.getSession().getSessionId());
    }

    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
        LOGGER.info("session with id={} ended.", requestEnvelope.getSession().getSessionId());
    }

    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        return constructAskResponse("Was soll ich bestellen?");
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        final Session session = requestEnvelope.getSession();
        final IntentRequest request = requestEnvelope.getRequest();
        final Intent intent = request.getIntent();

        try {
            if ("Item".equals(intent.getName())) {
                return onItemIntent(session, intent);
            }

            if ("Cart".equals(intent.getName())) {
                return onCartIntent(session, intent);
            }

            if ("Checkout".equals(intent.getName())) {
                return onCheckoutIntent(session, intent);
            }

            if ("AMAZON.HelpIntent".equals(intent.getName())) {
                return onHelpIntent(session, intent);
            }

            if ("AMAZON.StopIntent".equals(intent.getName())) {
                return constructTellResponse("Tschüss");
            }

            if ("AMAZON.CancelIntent".equals(intent.getName())) {
                return constructTellResponse("Tschüss");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        throw new UnsupportedOperationException("unknown intent " + intent.getName() + ".");
    }

    @Override
    public SpeechletResponse onHelpIntent(Session session, Intent intent) {
        return constructAskResponse("Ich warte auf eine Anweisung, wie zum Beispiel: ich möchte zwanzig Kapseln Ristretto bestellen.");
    }

    @Override
    public SpeechletResponse onCheckoutIntent(Session session, Intent intent) throws Exception {
        initItemsIfNecessary(session);

        final Environment environment = Environment.getInstance();
        final String password = environment.getPassword();
        final String username = environment.getUsername();
        final List<Item> items = getItemsFromSession(session);

        nespressoAdapter.executeOrder(items, username, password);
        return constructTellResponse("Ich habe die Bestellung abgeschickt. Du solltest in Kürze eine Bestätigung per E-Mail erhalten.");
    }

    @Override
    public SpeechletResponse onCartIntent(Session session, Intent intent) {
        initItemsIfNecessary(session);

        List<Item> items = getItemsFromSession(session);
        if (CollectionUtils.isEmpty(items)) {
            return constructAskResponse("Meine Einkaufsliste ist leer. Was soll ich bestellen?");
        }

        final String itemsText = String.join(", ",
                items.stream().map(ITEM_TO_STRING).collect(Collectors.toList())
        );

        final String text = "Ich habe Folgendes auf meiner Einkaufsliste stehen: " + itemsText + ". Was soll ich noch bestellen? Oder willst du die Bestellung abschliessen?";
        return constructAskResponse(text);
    }

    @Override
    public SpeechletResponse onItemIntent(Session session, Intent intent) {
        initItemsIfNecessary(session);

        Slot anzahl = intent.getSlot("anzahl");
        if (StringUtils.isBlank(anzahl.getValue())) {
            return constructAskResponseWithElicitSlotDirective(anzahl, "wie viele Kapseln soll ich bestellen?");
        }

        Slot geschmacksrichtung = intent.getSlot("geschmacksrichtung");
        if (StringUtils.isBlank(geschmacksrichtung.getValue())) {
            return constructAskResponseWithElicitSlotDirective(geschmacksrichtung, "welche Geschmacksrichtung soll ich bestellen?");
        }

        addItemToSession(session, anzahl, geschmacksrichtung);
        return constructAskResponse("alles klar, was soll ich sonst bestellen?");
    }

    private void initItemsIfNecessary(Session session) {
        Object items = session.getAttribute("items");
        if (items == null) {
            session.setAttribute("items", new ArrayList<>());
        }
    }

    private void addItemToSession(Session session, Slot anzahl, Slot geschmacksrichtung) {
        final Product product = Product.valueOf(geschmacksrichtung.getValue().toLowerCase());
        final Integer quantity = Integer.valueOf(anzahl.getValue());

        final Item item = new Item();
        item.setProduct(product);
        item.setQuantity(quantity);

        final List<Item> items = getItemsFromSession(session);
        items.add(item);

        session.setAttribute("items", items);
    }

    private List<Item> getItemsFromSession(Session session) {
        return ((List<Map<String, String>>) session.getAttribute("items")).stream().map(MAP_TO_ITEM).collect(Collectors.toList());
    }
}
