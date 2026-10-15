package io.github.piscescup.calccmd.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Integer configuration with inclusive bounds. */
public final class IntConfig extends AbstractConfig<Integer> {
    private final int min;
    private final int max;

    public IntConfig(int defaultValue) {
        this(defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public IntConfig(int defaultValue, int min) {
        this(defaultValue, min, Integer.MAX_VALUE);
    }

    public IntConfig(int defaultValue, int min, int max) {
        super(defaultValue);
        if (min > max) throw new IllegalArgumentException("Minimum exceeds maximum");
        this.min = min;
        this.max = max;
        validate(defaultValue);
    }

    @Override
    public Integer parse(String value) {
        int parsed = Integer.parseInt(value);
        validate(parsed);
        return parsed;
    }

    @Override
    public void validate(Integer value) {
        if (value == null || value < min || value > max) {
            throw new IllegalArgumentException("Expected an integer between " + min + " and " + max);
        }
    }

    @Override
    public JsonElement toJson(Integer value) {
        validate(value);
        return new JsonPrimitive(value);
    }
}
