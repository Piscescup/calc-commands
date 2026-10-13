package io.github.piscescup.fabricmc.config;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** A typed value selected from a fixed list of options. */
public final class OptionsConfig<T extends Option> extends AbstractConfig<T> {
    private final List<T> options;
    private final Function<String, T> parser;

    @SafeVarargs
    public OptionsConfig(T defaultValue, Function<String, T> parser, T... options) {
        this(defaultValue, List.of(options), parser);
    }

    public OptionsConfig(T defaultValue, List<T> options, Function<String, T> parser) {
        super(defaultValue);
        this.options = List.copyOf(options);
        this.parser = Objects.requireNonNull(parser, "parser");
        validate(defaultValue);
    }

    @Override
    public T parse(String value) {
        T parsed = parser.apply(value);
        validate(parsed);
        return parsed;
    }

    @Override
    public String serialize(T value) {
        return value.getValue();
    }

    @Override
    public void validate(T value) {
        if (value == null || !options.contains(value)) {
            throw new IllegalArgumentException("Invalid option value: " + value);
        }
    }

    @Override
    public List<String> options() {
        return options.stream().map(Option::getValue).toList();
    }
}
