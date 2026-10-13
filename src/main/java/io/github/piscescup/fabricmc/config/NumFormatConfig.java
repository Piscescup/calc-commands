package io.github.piscescup.fabricmc.config;

import io.github.piscescup.fabricmc.References;
import io.github.piscescup.fabricmc.calculation.CalcRequest;
import io.github.piscescup.fabricmc.presentation.QuantityFormatter;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

/** Persistent quantity settings; formatting itself lives in QuantityFormatter. */
public final class NumFormatConfig {
    public static final String ENABLED_KEY = "enabled";
    public static final String SCOPE_KEY = "scope";
    public static final String FORMAT_KEY = "format";
    public static final String TEMPLATE_KEY = "template";
    public static final String STACK_KEY = "stack";
    public static final int DEFAULT_STACK_SIZE = 64;

    private static final Set<CalcRequest.Operation> ALLOWED_OPERATIONS = Set.of(
        CalcRequest.Operation.EXPRESSION, CalcRequest.Operation.INTEGRATE,
        CalcRequest.Operation.DIFF, CalcRequest.Operation.LIMIT, CalcRequest.Operation.SOLVE);
    private static final BooleanConfig ENABLED = new BooleanConfig(false);
    private static final IntConfig STACK = new IntConfig(DEFAULT_STACK_SIZE, 1);
    private static final EnumSetConfig<CalcRequest.Operation> SCOPE =
        new EnumSetConfig<>(ALLOWED_OPERATIONS, ALLOWED_OPERATIONS);
    private static final OptionsConfig<Options.FormatMode> FORMAT = new OptionsConfig<>(
        Options.FormatMode.INTEGER, Options.FormatMode.values());
    private static final TemplateConfig TEMPLATE = new TemplateConfig();

    private static final class Holder {
        private static final java.nio.file.Path PATH = FabricLoader.getInstance().getConfigDir()
            .resolve(References.MOD_ID).resolve("num_fmt_cfg.json");
        private static final ConfigStore STORE = new ConfigStore(PATH)
            .register(ENABLED_KEY, ENABLED).register(SCOPE_KEY, SCOPE).register(FORMAT_KEY, FORMAT)
            .register(TEMPLATE_KEY, TEMPLATE).register(STACK_KEY, STACK);
        static { STORE.load(); }
    }

    private NumFormatConfig() {}

    public static Set<CalcRequest.Operation> allowedOperations() {
        return ALLOWED_OPERATIONS;
    }

    public static void initialize() {
        Holder.STORE.initialize();
    }

    /** Read all fields together, then release the store lock before formatting. */
    public static QuantityFormatter.Settings snapshot() {
        synchronized (Holder.STORE) {
            return new QuantityFormatter.Settings(ENABLED.value(), SCOPE.value(), FORMAT.value(),
                TEMPLATE.value(), STACK.value());
        }
    }

    public static void setEnabled(boolean enabled) throws IOException {
        Holder.STORE.set(ENABLED, enabled);
    }

    public static void setScope(CalcRequest.Operation operation, boolean included) throws IOException {
        if (operation == null || !ALLOWED_OPERATIONS.contains(operation)) {
            throw new IllegalArgumentException("Unsupported number-format scope: " + operation);
        }
        synchronized (Holder.STORE) {
            EnumSet<CalcRequest.Operation> selected = EnumSet.noneOf(CalcRequest.Operation.class);
            selected.addAll(SCOPE.value());
            if (included) selected.add(operation);
            else selected.remove(operation);
            Holder.STORE.set(SCOPE, selected);
        }
    }

    public static void setTemplate(String template, int stackSize) throws IOException {
        Holder.STORE.update(Map.of(TEMPLATE_KEY, template, STACK_KEY, Integer.toString(stackSize)));
    }

    public static void setFormatMode(Options.FormatMode mode) throws IOException {
        Holder.STORE.set(FORMAT, mode);
    }

    public static void reset() throws IOException {
        Holder.STORE.reset();
    }

    public static void resetScope() throws IOException {
        Holder.STORE.reset(SCOPE_KEY);
    }

    public static void resetFormatMode() throws IOException {
        Holder.STORE.reset(FORMAT_KEY);
    }

    public static void resetTemplate() throws IOException {
        Holder.STORE.reset(TEMPLATE_KEY, STACK_KEY);
    }
}
