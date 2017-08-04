package de.melnichuk.alexa_nespresso.nespresso;

import de.melnichuk.alexa_nespresso.nespresso.model.Item;

import java.util.Collection;

public class NoopNespressoAdapter implements NespressoAdapter {

    @Override
    public void executeOrder(Collection<Item> items, String username, String password) {
    }
}
