package io.github.piscescup.fabricmc.config;

import java.util.Objects;

/** Shared state and validated assignment for typed configuration values. */
public abstract class AbstractConfig<T> implements Configurable<T> {
    private final T defaultValue;
    private volatile T value;

    protected AbstractConfig(T defaultValue) {
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.value = defaultValue;
    }

    @Override
    public final T value() {
        return value;
    }

    @Override
    public final T defaultValue() {
        return defaultValue;
    }

    @Override
    public final void setValue(T value) {
        validate(value);
        this.value = copy(value);
    }

    protected T copy(T value) {
        return value;
    }

    @Override
    public String serialize(T value) {
        return String.valueOf(value);
    }
}
