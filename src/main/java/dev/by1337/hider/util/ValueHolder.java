package dev.by1337.hider.util;

import java.util.function.Supplier;

public abstract class ValueHolder<T> implements Supplier<T> {
    protected T value;
    public abstract T get();
    public abstract void set(T value);
}
