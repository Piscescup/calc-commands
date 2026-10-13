package io.github.piscescup.fabricmc.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;

public final class BooleanConfig extends AbstractConfig<Boolean> {
    public BooleanConfig(boolean defaultValue) {
        super(defaultValue);
    }

    @Override
    public Boolean parse(String value) {
        if ("true".equalsIgnoreCase(value) || "enable".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value) || "disable".equalsIgnoreCase(value)) return false;
        throw new IllegalArgumentException("Expected true/false or enable/disable");
    }

    @Override
    public List<String> options() {
        return List.of("enable", "disable");
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
