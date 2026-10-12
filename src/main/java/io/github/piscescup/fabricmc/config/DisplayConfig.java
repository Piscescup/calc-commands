package io.github.piscescup.fabricmc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.piscescup.fabricmc.References;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Persistent presentation settings that never alter evaluated expressions or ans. */
public final class DisplayConfig {
    public enum PowerOrder {
        ASCENDING("ascending"),
        DESCENDING("descending");

        private final String command;

        PowerOrder(String command) {
            this.command = command;
        }

        public String command() {
            return command;
        }

        private static PowerOrder parse(String command) {
            for (PowerOrder order : values()) {
                if (order.command.equals(command)) return order;
            }
            throw new IllegalArgumentException("Unsupported power order: " + command);
        }
    }

    private static final Pattern NUMERIC_TERM = Pattern.compile(
        "[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[Ee][+-]?[0-9]+)?(?:/[0-9]+)?");
    private static final Pattern VARIABLE_FACTOR = Pattern.compile(
        "[a-zA-Z_][a-zA-Z_0-9]*(?:\\^([0-9]+))?");
    private static final Pattern SYMBOLIC_CONSTANT = Pattern.compile("Pi|E|I");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
        .resolve(References.MOD_ID)
        .resolve("calc_cfg.json");
    private static volatile PowerOrder powerOrder = load();

    private DisplayConfig() {}

    public static synchronized void initialize() {
        if (Files.exists(PATH)) return;
        try {
            save(powerOrder);
        } catch (IOException error) {
            References.MOD_LOGGER.warn("Could not create calculator display config at {}", PATH, error);
        }
    }

    public static synchronized void setPowerOrder(PowerOrder order) throws IOException {
        save(order);
        powerOrder = order;
    }

    public static PowerOrder powerOrder() {
        return powerOrder;
    }

    public static synchronized void reset() throws IOException {
        setPowerOrder(PowerOrder.ASCENDING);
    }

    public static String arrangeTerms(String source) {
        return arrange(source);
    }

    private static PowerOrder load() {
        if (!Files.exists(PATH)) return PowerOrder.ASCENDING;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(PATH, StandardCharsets.UTF_8))
                .getAsJsonObject();
            if (json.has("powerOrder")) {
                return PowerOrder.parse(json.get("powerOrder").getAsString());
            }
            if (json.has("termOrder")) {
                return "variable-first".equals(json.get("termOrder").getAsString())
                    ? PowerOrder.DESCENDING
                    : PowerOrder.ASCENDING;
            }
            return PowerOrder.ASCENDING;
        } catch (RuntimeException | IOException error) {
            References.MOD_LOGGER.warn("Could not load {}; using defaults", PATH, error);
            return PowerOrder.ASCENDING;
        }
    }

    private static void save(PowerOrder order) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("powerOrder", order.command());
        Files.createDirectories(PATH.getParent());
        Path temporary = PATH.resolveSibling(PATH.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String arrange(String source) {
        StringBuilder nested = new StringBuilder(source.length());
        for (int index = 0; index < source.length();) {
            if (source.charAt(index) != '(') {
                nested.append(source.charAt(index++));
                continue;
            }
            int close = matchingClose(source, index);
            if (close < 0) return source;
            nested.append('(').append(arrange(source.substring(index + 1, close))).append(')');
            index = close + 1;
        }
        return arrangeCommaSections(nested.toString());
    }

    private static String arrangeCommaSections(String source) {
        StringBuilder result = new StringBuilder(source.length());
        int depth = 0;
        int start = 0;
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '(') depth++;
            else if (character == ')') depth--;
            else if (character == ',' && depth == 0) {
                result.append(orderByPower(source.substring(start, index))).append(',');
                start = index + 1;
            }
        }
        return result.append(orderByPower(source.substring(start))).toString();
    }

    private static int matchingClose(String source, int open) {
        int depth = 1;
        for (int index = open + 1; index < source.length(); index++) {
            if (source.charAt(index) == '(') depth++;
            if (source.charAt(index) == ')' && --depth == 0) return index;
        }
        return -1;
    }

    private static String orderByPower(String source) {
        List<String> terms = splitTerms(source);
        if (terms.size() < 2) return source;
        if (terms.stream().anyMatch(term -> degree(term) < 0)) return source;
        Comparator<String> comparator = Comparator.comparingInt(DisplayConfig::degree);
        if (powerOrder == PowerOrder.DESCENDING) comparator = comparator.reversed();
        terms.sort(comparator);
        StringBuilder result = new StringBuilder(source.length());
        for (String term : terms) {
            if (result.isEmpty()) {
                if (term.charAt(0) == '+') result.append(term, 1, term.length());
                else result.append(term);
            } else if (term.charAt(0) == '+' || term.charAt(0) == '-') {
                result.append(term);
            } else {
                result.append('+').append(term);
            }
        }
        return result.toString();
    }

    private static List<String> splitTerms(String source) {
        List<String> terms = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '(') depth++;
            else if (character == ')') depth--;
            else if (depth == 0 && index > start && (character == '+' || character == '-')
                    && !isExponentSign(source, index) && !isUnarySign(source, index)) {
                terms.add(source.substring(start, index));
                start = index;
            }
        }
        terms.add(source.substring(start));
        return terms;
    }

    private static boolean isExponentSign(String source, int index) {
        char previous = source.charAt(index - 1);
        if ((previous != 'e' && previous != 'E') || index < 2) return false;
        char beforeExponent = source.charAt(index - 2);
        return Character.isDigit(beforeExponent) || beforeExponent == '.';
    }

    private static boolean isUnarySign(String source, int index) {
        char previous = source.charAt(index - 1);
        return previous == '+' || previous == '-' || previous == '*' || previous == '/'
            || previous == '^' || previous == ',' || previous == '(';
    }

    private static boolean isNumeric(String term) {
        return NUMERIC_TERM.matcher(term).matches();
    }

    private static int degree(String term) {
        String body = term;
        if (!body.isEmpty() && (body.charAt(0) == '+' || body.charAt(0) == '-')) {
            body = body.substring(1);
        }
        if (isNumeric(body) || SYMBOLIC_CONSTANT.matcher(body).matches()) return 0;
        int degree = 0;
        for (String factor : body.split("\\*", -1)) {
            if (isNumeric(factor) || SYMBOLIC_CONSTANT.matcher(factor).matches()) continue;
            Matcher variable = VARIABLE_FACTOR.matcher(factor);
            if (!variable.matches()) return -1;
            try {
                degree = Math.addExact(degree,
                    variable.group(1) == null ? 1 : Integer.parseInt(variable.group(1)));
            } catch (NumberFormatException | ArithmeticException error) {
                return -1;
            }
        }
        return degree;
    }
}
