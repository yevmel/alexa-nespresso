package de.melnichuk.alexa_nespresso.alexa;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface AsyncRunner<T> {

    /**
     * passes the result from responseSupplier to responseConsumer. Implementation is expected to do it in a asynchronous manner.
     *
     * @param responseConsumer
     * @param responseSupplier
     */
    void runAsync(Consumer<T> responseConsumer, Supplier<T> responseSupplier);
}
