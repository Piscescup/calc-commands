package io.github.piscescup.calccmd.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** An immutable selection from a fixed set of enum options. */
public final class EnumSetConfig<T extends Enum<T> & Option> extends AbstractConfig<Set<T>> {
    private final Set<T> options;

    public EnumSetConfig(Set<T> defaultValue, Set<T> options) {
        super(Set.copyOf(defaultValue));
        this.options = Set.copyOf(options);
        validate(defaultValue);
    }

    @Override
    public Set<T> parse(String value) {
        Set<T> selected = new LinkedHashSet<>();
        if (!value.isBlank()) {
            for (String name : value.split(",", -1)) {
                selected.add(options.stream().filter(option -> option.getValue().equals(name.strip()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown option: " + name)));
            }
        }
        return Set.copyOf(selected);
    }

    @Override
    protected Set<T> copy(Set<T> value) {
        return Set.copyOf(value);
    }

    @Override
    public String serialize(Set<T> value) {
        return value.stream().map(Option::getValue).sorted().collect(Collectors.joining(","));
    }

    @Override
    public void validate(Set<T> value) {
        if (value == null || !options.containsAll(value)) {
            throw new IllegalArgumentException("Selection contains unsupported options");
        }
    }

    @Override
    public List<String> options() {
        return options.stream().map(Option::getValue).sorted().toList();
    }

    @Override
    public JsonElement toJson(Set<T> value) {
        validate(value);
        JsonArray array = new JsonArray();
        value.stream().map(Option::getValue).sorted().forEach(array::add);
        return array;
    }

    @Override
    public Set<T> fromJson(JsonElement json) {
        return parse(StreamSupport.stream(json.getAsJsonArray().spliterator(), false)
            .map(JsonElement::getAsString).collect(Collectors.joining(",")));
    }
}
