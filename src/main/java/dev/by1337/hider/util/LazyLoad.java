package dev.by1337.hider.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class LazyLoad<T> {
    private final Supplier<@NotNull T> supplier;
    private T value;
    private boolean modified;
    private boolean loaded;
    private final @Nullable LazyLoad<?> loadBefore;

    public LazyLoad(Supplier<T> supplier, @Nullable LazyLoad<?> loadBefore) {
        this.supplier = supplier;
        this.loadBefore = loadBefore;
    }
    public LazyLoad(Supplier<T> supplier, @Nullable LazyLoad<?> loadBefore, boolean modified) {
        this.supplier = supplier;
        this.loadBefore = loadBefore;
        this.modified = modified;
    }

    public T get() {
        if (loadBefore != null && !loadBefore.loaded) {
            loadBefore.get();
        }
        return value == null ? value = supplier.get() : value;
    }

    public void set(T value) {
        this.value = value;
        modified = true;
    }

    public boolean isModified() {
        return modified;
    }

    public boolean isLoaded() {
        return loaded;
    }

    @Nullable
    public T forceGet() {
        return value;
    }

}
