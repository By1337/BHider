package dev.by1337.hider.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class LazyLoad<T> extends ValueHolder<T> {
    private final Supplier<@NotNull T> supplier;
    private boolean loaded;
    private final @Nullable Supplier<?> loadBefore;

    public LazyLoad(Supplier<T> supplier, @Nullable Supplier<?> loadBefore) {
        this.supplier = supplier;
        this.loadBefore = loadBefore;
    }

    public T get() {
        if (loadBefore instanceof LazyLoad<?> lazyLoad && !lazyLoad.loaded) {
            loadBefore.get();
        }
        if (!loaded) {
            value = supplier.get();
            loaded = true;
        }
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public boolean isLoaded() {
        return loaded;
    }

    @Nullable
    public T forceGet() {
        return value;
    }

}
