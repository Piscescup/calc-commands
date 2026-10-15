package io.github.piscescup.calccmd.config;

import io.github.piscescup.calccmd.References;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;

/** Persistent calculator settings; runtime storage is initialized only when first used. */
public final class DisplayConfig {
    public static final String POWER_ORDER_KEY = "power_order";
    public static final String COMPLEX_KEY = "complex";

    private static final class Holder {
        private static final ConfigStore STORE = createStore(FabricLoader.getInstance().getConfigDir()
            .resolve(References.MOD_ID).resolve("calc_cfg.json"));
        static { STORE.load(); }
    }

    private DisplayConfig() {}

    static ConfigStore createStore(Path path) {
        return new ConfigStore(path)
            .register(POWER_ORDER_KEY, new OptionsConfig<>(Options.PowerOrder.ASCENDING, Options.PowerOrder.values()))
            .register(COMPLEX_KEY, new BooleanConfig(false));
    }

    public static void initialize() {
        Holder.STORE.initialize();
    }

    public static List<String> keys() {
        return Holder.STORE.keys();
    }

    public static List<String> optionsOf(String key) {
        return Holder.STORE.optionsOf(key);
    }

    public static boolean complexEnabled() {
        return Boolean.parseBoolean(get(COMPLEX_KEY));
    }

    public static Options.PowerOrder powerOrder() {
        return Options.PowerOrder.valueOf(get(POWER_ORDER_KEY).toUpperCase(Locale.ROOT));
    }

    public static String get(String key) {
        return Holder.STORE.get(key);
    }

    public static void set(String key, String value) throws IOException {
        Holder.STORE.set(key, value);
    }

    public static void reset(String key) throws IOException {
        Holder.STORE.reset(key);
    }

    public static void reset() throws IOException {
        Holder.STORE.reset();
    }
}
