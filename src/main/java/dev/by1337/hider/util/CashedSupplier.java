package dev.by1337.hider.util;

import java.util.function.Supplier;

public class CashedSupplier<T> implements Supplier<T> {
    private final Supplier<T> supplier;
    private T value;

    public CashedSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        if (value != null) return value;
        return value = supplier.get();
    }

    public void invalidate() {
        value = null;
    }
}
