package io.github.piscescup.calccmd.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Validates a complete update, saves it, then publishes the new in-memory values. */
public final class ConfigStore {
    private static final Logger LOGGER = LogManager.getLogger("calc-commands");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private final Map<String, Configurable<?>> configs = new LinkedHashMap<>();
    private boolean loaded;

    public ConfigStore(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    public synchronized ConfigStore register(String key, Configurable<?> config) {
        if (loaded) throw new IllegalStateException("Register configuration before loading it");
        if (key == null || key.isBlank() || configs.containsKey(key) || configs.containsValue(config)) {
            throw new IllegalArgumentException("Duplicate or invalid configuration key: " + key);
        }
        configs.put(key, Objects.requireNonNull(config, "config"));
        return this;
    }

    public synchronized List<String> keys() {
        return List.copyOf(configs.keySet());
    }

    private Configurable<?> configOf(String key) {
        Configurable<?> config = configs.get(key);
        if (config == null) throw new IllegalArgumentException("Unknown configuration key: " + key);
        return config;
    }

    public synchronized List<String> optionsOf(String key) {
        return configOf(key).options();
    }

    public synchronized String get(String key) {
        return configOf(key).stringValue();
    }

    public synchronized Map<String, String> snapshot() {
        Map<String, String> values = new LinkedHashMap<>();
        configs.forEach((key, config) -> values.put(key, config.stringValue()));
        return Collections.unmodifiableMap(values);
    }

    public synchronized void set(String key, String value) throws IOException {
        update(Map.of(key, value));
    }

    public synchronized <T> void set(Configurable<T> config, T value) throws IOException {
        String key = keyOf(config);
        JsonObject replacement = currentJson();
        replacement.add(key, config.toJson(value));
        replace(replacement);
    }

    /** Multiple fields are saved together; failed validation or I/O publishes none of them. */
    public synchronized void update(Map<String, String> updates) throws IOException {
        JsonObject replacement = currentJson();
        updates.forEach((key, value) -> replacement.add(key, parse(configOf(key), value)));
        replace(replacement);
    }

    public synchronized void reset(String... keys) throws IOException {
        JsonObject replacement = currentJson();
        for (String key : keys) replacement.add(key, defaultJson(configOf(key)));
        replace(replacement);
    }

    public synchronized void reset() throws IOException {
        replace(defaultsJson());
    }

    public synchronized void load() {
        JsonObject values;
        try {
            values = readJson(path);
        } catch (RuntimeException | IOException error) {
            LOGGER.warn("Could not load {}; using defaults", path, error);
            values = defaultsJson();
        }
        apply(values);
        loaded = true;
    }

    public synchronized void initialize() {
        if (Files.exists(path)) return;
        try {
            write(path, currentJson());
        } catch (IOException error) {
            LOGGER.warn("Could not create configuration at {}", path, error);
        }
    }

    /** Reads and validates a file without replacing the active configuration. */
    public synchronized Map<String, String> read(Path source) throws IOException {
        JsonObject json = readJson(source);
        Map<String, String> values = new LinkedHashMap<>();
        configs.forEach((key, config) -> values.put(key, text(config, json.get(key))));
        return Collections.unmodifiableMap(values);
    }

    /** Writes a validated snapshot without replacing the active configuration. */
    public synchronized void save(Path target, Map<String, String> values) throws IOException {
        JsonObject json = defaultsJson();
        values.forEach((key, value) -> json.add(key, parse(configOf(key), value)));
        write(target, json);
    }

    private String keyOf(Configurable<?> config) {
        return configs.entrySet().stream().filter(entry -> entry.getValue() == config)
            .map(Map.Entry::getKey).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Configuration is not registered in this store"));
    }

    private JsonObject currentJson() {
        JsonObject json = new JsonObject();
        configs.forEach((key, config) -> json.add(key, currentJson(config)));
        return json;
    }

    private JsonObject defaultsJson() {
        JsonObject json = new JsonObject();
        configs.forEach((key, config) -> json.add(key, defaultJson(config)));
        return json;
    }

    private JsonObject readJson(Path source) throws IOException {
        JsonObject values = defaultsJson();
        if (!Files.exists(source)) return values;
        JsonObject json = JsonParser.parseString(Files.readString(source, StandardCharsets.UTF_8)).getAsJsonObject();
        configs.forEach((key, config) -> {
            if (!json.has(key)) return;
            try {
                values.add(key, canonicalJson(config, json.get(key)));
            } catch (RuntimeException error) {
                LOGGER.warn("Invalid configuration field {} in {}; using its default", key, source, error);
            }
        });
        return values;
    }

    private void replace(JsonObject replacement) throws IOException {
        write(path, replacement);
        apply(replacement);
    }

    private void apply(JsonObject values) {
        configs.forEach((key, config) -> apply(config, values.get(key)));
    }

    private static <T> JsonElement currentJson(Configurable<T> config) {
        return config.toJson(config.value());
    }

    private static <T> JsonElement defaultJson(Configurable<T> config) {
        return config.toJson(config.defaultValue());
    }

    private static <T> JsonElement parse(Configurable<T> config, String value) {
        return config.toJson(config.parse(value));
    }

    private static <T> JsonElement canonicalJson(Configurable<T> config, JsonElement value) {
        return config.toJson(config.fromJson(value));
    }

    private static <T> String text(Configurable<T> config, JsonElement value) {
        return config.serialize(config.fromJson(value));
    }

    private static <T> void apply(Configurable<T> config, JsonElement value) {
        config.setValue(config.fromJson(value));
    }

    private static void write(Path target, JsonObject json) throws IOException {
        Path absolute = target.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), absolute.getFileName() + ".", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
