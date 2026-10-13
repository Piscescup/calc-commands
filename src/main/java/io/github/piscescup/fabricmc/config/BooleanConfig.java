package io.github.piscescup.fabricmc.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BooleanConfig extends AbstractConfig<Boolean> {
    public BooleanConfig(boolean defaultValue) {
        super(defaultValue);
    }

    @Override
    public Boolean parse(String value) {
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        throw new IllegalArgumentException("Expected true or false");
    }

    @Override
    public void validate(Boolean value) {
        if (value == null) throw new IllegalArgumentException("Boolean value cannot be null");
    }

    @Override
    public JsonElement toJson(Boolean value) {
        validate(value);
        return new JsonPrimitive(value);
    }
}
