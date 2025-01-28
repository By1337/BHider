package dev.by1337.hider.util;

public class WrappedValueHolder<T> extends ValueHolder<T> {

    public WrappedValueHolder() {
    }

    public WrappedValueHolder(T val) {
        value = val;
    }

    public static <T> WrappedValueHolder<T> of(T val) {
        return new WrappedValueHolder<>(val);
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public void set(T value) {
        this.value = value;
    }
}
