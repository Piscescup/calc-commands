package io.github.piscescup.fabricmc.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/** A typed in-memory value. Use ConfigStore to persist changes. */
public interface Configurable<T> {
    T value();
    void setValue(T value);
    T defaultValue();
    T parse(String value);
    String serialize(T value);
    void validate(T value);

    default String stringValue() {
        return serialize(value());
    }

    default String defaultValueString() {
        return serialize(defaultValue());
    }

    default void setValueByString(String value) {
        setValue(parse(value));
    }

    default void resetToDefault() {
        setValue(defaultValue());
    }

    default void validateStringValue(String value) {
        validate(parse(value));
    }

    default List<String> options() {
        return List.of();
    }

    default JsonElement toJson(T value) {
        validate(value);
        return new JsonPrimitive(serialize(value));
    }

    default T fromJson(JsonElement json) {
        T value = parse(json.getAsString());
        validate(value);
        return value;
    }
}
