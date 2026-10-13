package io.github.piscescup.fabricmc.config;

import net.fabricmc.loader.api.FabricLoader;
import io.github.piscescup.fabricmc.References;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Persistent calculator settings; changes never rewrite the saved answer. */
public final class DisplayConfig {
    public static final String POWER_ORDER_KEY = "power_order";
    public static final String COMPLEX_KEY = "complex";

    public static final OptionsConfig<Options.AbleOption> COMPLEX_CONFIG = new OptionsConfig<>(
        Options.AbleOption.DISABLE, Options.AbleOption::fromValue,
        Options.AbleOption.ENABLE, Options.AbleOption.DISABLE);

    public static final OptionsConfig<Options.PowerOrder> POWER_ORDER_CONFIG = new OptionsConfig<>(
        Options.PowerOrder.DESCENDING, Options.PowerOrder::fromValue,
        Options.PowerOrder.ASCENDING, Options.PowerOrder.DESCENDING);

    private static final ConfigStore STORE = new ConfigStore(FabricLoader.getInstance().getConfigDir()
        .resolve(References.MOD_ID).resolve("config").resolve("calc_cfg.json"))
        .register(POWER_ORDER_KEY, POWER_ORDER_CONFIG)
        .register(COMPLEX_KEY, COMPLEX_CONFIG);

    static {
        STORE.load();
    }

    private DisplayConfig() {}

    public static ConfigStore store() {
        return STORE;
    }

    public static void initialize() {
        STORE.initialize();
    }

    public static List<String> keys() {
        return STORE.keys();
    }

    public static String get(String key) {
        return STORE.get(key);
    }

    public static void set(String key, String value) throws IOException {
        STORE.set(key, value);
    }

    public static void reset(String key) throws IOException {
        STORE.reset(key);
    }

    public static void reset() throws IOException {
        STORE.reset();
    }

    public static Configurable<?> configOf(String key) {
        return STORE.configOf(key);
    }

    public static List<String> optionsOf(String key) {
        return configOf(key).options();
    }

    static Map<String, String> read(Path path) throws IOException {
        return STORE.read(path);
    }

    static void save(Path path, Map<String, String> values) throws IOException {
        STORE.save(path, values);
    }
}
