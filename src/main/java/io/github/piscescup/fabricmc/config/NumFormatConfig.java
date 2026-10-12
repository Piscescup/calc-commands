package io.github.piscescup.fabricmc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.piscescup.fabricmc.References;
import io.github.piscescup.fabricmc.calculation.CalcRequest;
import net.fabricmc.loader.api.FabricLoader;
import org.matheclipse.core.expression.S;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Persistent, immediately replaceable settings for inventory-style number formatting.
 */
public final class NumFormatConfig {
    public static final String SB_STACK_ITEM_TEMPLATE = "\"{} S.B.(s) + {} Stack(s) + {} Item(s)\"";

    public static final String STACK_ITEM_TEMPLATE = "\"{} Stack(s) + {} Item(s)\"";

    public static final String ITEM_TEMPLATE = "\"{} Item(s)\"";

    public static final Set<String> SUGGESTION_TEMPLATES = Set.of(
        SB_STACK_ITEM_TEMPLATE,
        STACK_ITEM_TEMPLATE,
        ITEM_TEMPLATE
    );

    public static final int DEFAULT_STACK_SIZE = 64;

    public enum FormatMode {
        INTEGER("integer"),
        DOUBLE("double"),
        ALL("all");

        private final String command;

        FormatMode(String command) {
            this.command = command;
        }

        public String command() {
            return command;
        }

        private boolean allows(boolean integer) {
            return this == ALL || integer == (this == INTEGER);
        }

        private static FormatMode parse(String command) {
            for (FormatMode mode : values()) {
                if (mode.command.equals(command)) return mode;
            }
            throw new IllegalArgumentException("Unsupported number-format mode: " + command);
        }
    }

    private static final int MAX_TEMPLATE_LENGTH = 256;
    private static final int STACKS_PER_SHULKER_BOX = 27;
    private static final Set<CalcRequest.Operation> ALLOWED_OPERATIONS = Set.of(
        CalcRequest.Operation.EXPRESSION,
        CalcRequest.Operation.INTEGRATE,
        CalcRequest.Operation.SOLVE
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
        .resolve(References.MOD_ID)
        .resolve("num_fmt_cfg.json");
    private static volatile Settings settings = load();

    private NumFormatConfig() {
    }

    private record Settings(
        boolean enabled,
        Set<CalcRequest.Operation> scope,
        String template,
        int stackSize,
        FormatMode formatMode
    )
    {
        private Settings {
            scope = Set.copyOf(scope);
        }
    }

    public static Set<CalcRequest.Operation> scope() {
        return settings.scope();
    }

    public static boolean enabled() {
        return settings.enabled();
    }

    public static String template() {
        return settings.template();
    }

    public static int stackSize() {
        return settings.stackSize();
    }

    public static FormatMode formatMode() {
        return settings.formatMode();
    }

    /**
     * Creates the default file on first launch; subsequent changes replace the in-memory state immediately.
     */
    public static synchronized void initialize() {
        if (Files.exists(PATH)) return;
        try {
            save(settings);
        } catch (IOException error) {
            References.MOD_LOGGER.warn("Could not create default number-format config at {}", PATH, error);
        }
    }

    public static synchronized void setEnabled(boolean enabled) throws IOException {
        Settings current = settings;
        replace(new Settings(enabled, current.scope(), current.template(), current.stackSize(), current.formatMode()));
    }

    public static synchronized void setScope(CalcRequest.Operation operation, boolean included) throws IOException {
        if (!ALLOWED_OPERATIONS.contains(operation)) {
            throw new IllegalArgumentException("Unsupported number-format scope: " + operation.command());
        }
        Settings current = settings;
        EnumSet<CalcRequest.Operation> scope = current.scope()
            .isEmpty()
            ? EnumSet.noneOf(CalcRequest.Operation.class)
            : EnumSet.copyOf(current.scope());
        if (included) {
            scope.add(operation);
        } else {
            scope.remove(operation);
        }
        replace(new Settings(current.enabled(), scope, current.template(), current.stackSize(), current.formatMode()));
    }

    public static synchronized void setTemplate(String template, int stackSize) throws IOException {
        validateTemplate(template);
        if (stackSize < 1) throw new IllegalArgumentException("Stack size must be positive");
        Settings current = settings;
        replace(new Settings(current.enabled(), current.scope(), template, stackSize, current.formatMode()));
    }

    public static synchronized void setFormatMode(FormatMode formatMode) throws IOException {
        Settings current = settings;
        replace(new Settings(current.enabled(), current.scope(), current.template(), current.stackSize(), formatMode));
    }

    public static synchronized void reset() throws IOException {
        replace(defaults());
    }

    public static synchronized void resetScope() throws IOException {
        Settings current = settings;
        replace(new Settings(current.enabled(), ALLOWED_OPERATIONS, current.template(),
            current.stackSize(), current.formatMode()));
    }

    public static synchronized void resetFormatMode() throws IOException {
        setFormatMode(FormatMode.INTEGER);
    }

    public static synchronized void resetTemplate() throws IOException {
        Settings current = settings;
        replace(new Settings(current.enabled(), current.scope(), SB_STACK_ITEM_TEMPLATE,
            DEFAULT_STACK_SIZE, current.formatMode()));
    }

    /**
     * Returns empty when this value should retain the calculator's normal presentation.
     */
    public static Optional<String> format(CalcRequest.Operation operation, String exactValue) {
        return format(operation, exactValue, false);
    }

    /** Formats an evaluator approximation while retaining decimal notation for its remainder. */
    public static Optional<String> formatApproximate(CalcRequest.Operation operation, String approximateValue) {
        return format(operation, approximateValue, true);
    }

    private static Optional<String> format(CalcRequest.Operation operation, String source, boolean decimalOutput) {
        Settings snapshot = settings;
        if (!snapshot.enabled() || !snapshot.scope()
            .contains(operation)) {
            return Optional.empty();
        }
        Optional<Rational> parsed = Rational.parse(source);
        if (parsed.isEmpty()) return Optional.empty();
        Rational total = parsed.get();
        if (!snapshot.formatMode().allows(total.isInteger())) return Optional.empty();
        String fractionSuffix = "";
        Rational templateValue = total;
        if (!decimalOutput && !total.isInteger()) {
            fractionSuffix = " + " + total.fractionalPart();
            templateValue = total.integerPart();
        }

        int placeholders = validateTemplate(snapshot.template());
        String formatted;
        if (placeholders == 0) {
            formatted = snapshot.template() + fractionSuffix;
            return Optional.of(complete(source, formatted, snapshot.stackSize()));
        }
        if (placeholders == 1) {
            formatted = applyTemplate(snapshot.template(), valueText(templateValue, decimalOutput)) + fractionSuffix;
            return Optional.of(complete(source, formatted, snapshot.stackSize()));
        }
        if (placeholders == 2) {
            Division stacks = templateValue.divideAndRemainder(snapshot.stackSize());
            formatted = applyTemplate(snapshot.template(), stacks.quotient().toString(),
                valueText(stacks.remainder(), decimalOutput)) + fractionSuffix;
            return Optional.of(complete(source, formatted, snapshot.stackSize()));
        }
        Division boxes = templateValue.divideAndRemainder(
            (long) snapshot.stackSize() * STACKS_PER_SHULKER_BOX);
        Division stacks = boxes.remainder().divideAndRemainder(snapshot.stackSize());
        String[] values = new String[placeholders];
        values[0] = boxes.quotient().toString();
        values[1] = stacks.quotient().toString();
        for (int index = 2; index < values.length; index++) {
            values[index] = valueText(stacks.remainder(), decimalOutput);
        }
        formatted = applyTemplate(snapshot.template(), values) + fractionSuffix;
        return Optional.of(complete(source, formatted, snapshot.stackSize()));
    }

    private static Settings load() {
        Settings defaults = defaults();
        if (!Files.exists(PATH)) return defaults;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(PATH, StandardCharsets.UTF_8))
                .getAsJsonObject();
            boolean enabled = json.has("enabled") && json.get("enabled")
                .getAsBoolean();
            String template = json.has("template") ? json.get("template")
                .getAsString() : SB_STACK_ITEM_TEMPLATE;
            int stackSize = json.has("stack") ? json.get("stack")
                .getAsInt() : DEFAULT_STACK_SIZE;
            FormatMode formatMode = json.has("format")
                ? FormatMode.parse(json.get("format").getAsString())
                : FormatMode.INTEGER;
            validateTemplate(template);
            if (stackSize < 1) throw new IllegalArgumentException("Stack size must be positive");

            EnumSet<CalcRequest.Operation> scope = EnumSet.noneOf(CalcRequest.Operation.class);
            if (json.has("scope")) {
                for (JsonElement entry : json.getAsJsonArray("scope")) {
                    String command = entry.getAsString();
                    scope.add(parseOperation(command));
                }
            } else {
                scope.addAll(ALLOWED_OPERATIONS);
            }
            return new Settings(enabled, scope, template, stackSize, formatMode);
        } catch (RuntimeException | IOException error) {
            References.MOD_LOGGER.warn("Could not load {}; using defaults", PATH, error);
            return defaults;
        }
    }

    private static Settings defaults() {
        return new Settings(false, ALLOWED_OPERATIONS, SB_STACK_ITEM_TEMPLATE, DEFAULT_STACK_SIZE,
            FormatMode.INTEGER);
    }

    private static CalcRequest.Operation parseOperation(String command) {
        for (CalcRequest.Operation operation : ALLOWED_OPERATIONS) {
            if (operation.command().equals(command)) return operation;
        }
        throw new IllegalArgumentException("Unsupported number-format scope: " + command);
    }

    private static void replace(Settings replacement) throws IOException {
        save(replacement);
        settings = replacement;
    }

    private static void save(Settings value) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", value.enabled());
        JsonArray scope = new JsonArray();
        value.scope()
            .stream()
            .map(CalcRequest.Operation::command)
            .sorted()
            .forEach(scope::add);
        json.add("scope", scope);
        json.addProperty("template", value.template());
        json.addProperty("stack", value.stackSize());
        json.addProperty("format", value.formatMode().command());

        Files.createDirectories(PATH.getParent());
        Path temporary = PATH.resolveSibling(PATH.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Validates braces and returns the number of supported placeholders. */
    public static int validateTemplate(String template) {
        if (template == null || template.isBlank() || template.length() > MAX_TEMPLATE_LENGTH) {
            throw new IllegalArgumentException("Invalid template length");
        }
        int placeholders = 0;
        for (int offset = 0; offset < template.length(); offset++) {
            char character = template.charAt(offset);
            if (character == '{') {
                if (offset + 1 >= template.length() || template.charAt(offset + 1) != '}') {
                    throw new IllegalArgumentException("Unmatched template brace");
                }
                placeholders++;
                offset++;
            } else if (character == '}') {
                throw new IllegalArgumentException("Unmatched template brace");
            }
        }
        return placeholders;
    }

    private static String applyTemplate(String template, String... values) {
        StringBuilder output = new StringBuilder(template.length() + 32);
        int from = 0;
        for (String value : values) {
            int placeholder = template.indexOf("{}", from);
            output.append(template, from, placeholder)
                .append(value);
            from = placeholder + 2;
        }
        return output.append(template, from, template.length())
            .toString();
    }

    private static String valueText(Rational value, boolean decimalOutput) {
        return decimalOutput ? value.decimalText() : value.toString();
    }

    private static String complete(String source, String formatted, int stackSize) {
        return source + "=" + formatted + " at Stack=" + stackSize;
    }

    private record Division(BigInteger quotient, Rational remainder) {}

    /** Non-negative reduced rational used so exact calculator results stay exact in the final remainder. */
    private record Rational(BigInteger numerator, BigInteger denominator) {
        private Rational {
            if (numerator.signum() < 0 || denominator.signum() <= 0) {
                throw new IllegalArgumentException("Rational must be non-negative");
            }
            BigInteger divisor = numerator.gcd(denominator);
            numerator = numerator.divide(divisor);
            denominator = denominator.divide(divisor);
        }

        private static Optional<Rational> parse(String source) {
            try {
                int slash = source.indexOf('/');
                if (slash >= 0) {
                    if (slash == 0 || slash != source.lastIndexOf('/') || slash == source.length() - 1) {
                        return Optional.empty();
                    }
                    return Optional.of(new Rational(
                        new BigInteger(source.substring(0, slash)),
                        new BigInteger(source.substring(slash + 1))));
                }
                BigDecimal decimal = new BigDecimal(source);
                if (decimal.signum() < 0) return Optional.empty();
                BigInteger numerator = decimal.unscaledValue();
                int scale = decimal.scale();
                return scale < 0
                    ? Optional.of(new Rational(numerator.multiply(BigInteger.TEN.pow(-scale)), BigInteger.ONE))
                    : Optional.of(new Rational(numerator, BigInteger.TEN.pow(scale)));
            } catch (IllegalArgumentException | ArithmeticException error) {
                return Optional.empty();
            }
        }

        private boolean isInteger() {
            return denominator.equals(BigInteger.ONE);
        }

        private Rational integerPart() {
            return new Rational(numerator.divide(denominator), BigInteger.ONE);
        }

        private Rational fractionalPart() {
            return new Rational(numerator.remainder(denominator), denominator);
        }

        private Division divideAndRemainder(long divisor) {
            BigInteger[] result = numerator.divideAndRemainder(
                denominator.multiply(BigInteger.valueOf(divisor)));
            return new Division(result[0], new Rational(result[1], denominator));
        }

        private String decimalText() {
            BigDecimal decimal = new BigDecimal(numerator).divide(new BigDecimal(denominator));
            BigDecimal normalized = decimal.stripTrailingZeros();
            return normalized.scale() < 0
                ? normalized.setScale(0).toPlainString()
                : normalized.toPlainString();
        }

        @Override
        public String toString() {
            return isInteger() ? numerator.toString() : numerator + "/" + denominator;
        }
    }
}
