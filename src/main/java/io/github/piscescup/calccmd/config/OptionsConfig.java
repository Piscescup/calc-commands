package io.github.piscescup.calccmd.config;

import java.util.List;

/** A typed value selected from a fixed list of options. */
public final class OptionsConfig<T extends Option> extends AbstractConfig<T> {
    private final List<T> options;

    @SafeVarargs
    public OptionsConfig(T defaultValue, T... options) {
        super(defaultValue);
        this.options = List.of(options);
        validate(defaultValue);
    }

    @Override
    public T parse(String value) {
        return options.stream().filter(option -> option.getValue().equalsIgnoreCase(value))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid option value: " + value));
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
