package io.github.piscescup.fabricmc.calculation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.github.piscescup.fabricmc.calculation.CalculatorEngine.MAX_ARGUMENTS;

/** Function syntax is shared by validation, completion tooltips and chat help. */
public record FunctionDefinition(String name, int minimumArguments, int maximumArguments,
        String parameters, String example, Category category) {
    public enum Category {
        TRIGONOMETRY, NUMBERS, LOGARITHMS, GEOMETRY, COLLECTIONS, OTHER;

        public String translationKey() {
            return "commands.calc.help.function_group." + name().toLowerCase(Locale.ROOT);
        }
    }

    static final FunctionDefinition SIN = new FunctionDefinition("sin", 1, 1, "x", "sin(pi/2)", Category.TRIGONOMETRY);
    static final FunctionDefinition COS = new FunctionDefinition("cos", 1, 1, "x", "cos(pi)", Category.TRIGONOMETRY);
    static final FunctionDefinition TAN = new FunctionDefinition("tan", 1, 1, "x", "tan(pi/4)", Category.TRIGONOMETRY);
    static final FunctionDefinition ASIN = new FunctionDefinition("asin", 1, 1, "x", "asin(1)", Category.TRIGONOMETRY);
    static final FunctionDefinition ACOS = new FunctionDefinition("acos", 1, 1, "x", "acos(0)", Category.TRIGONOMETRY);
    static final FunctionDefinition ATAN = new FunctionDefinition("atan", 1, 1, "x", "atan(1)", Category.TRIGONOMETRY);
    static final FunctionDefinition SINH = new FunctionDefinition("sinh", 1, 1, "x", "sinh(0)", Category.TRIGONOMETRY);
    static final FunctionDefinition COSH = new FunctionDefinition("cosh", 1, 1, "x", "cosh(0)", Category.TRIGONOMETRY);
    static final FunctionDefinition TANH = new FunctionDefinition("tanh", 1, 1, "x", "tanh(0)", Category.TRIGONOMETRY);
    static final FunctionDefinition SQRT = new FunctionDefinition("sqrt", 1, 1, "x", "sqrt(8)", Category.NUMBERS);
    static final FunctionDefinition ABS = new FunctionDefinition("abs", 1, 1, "x", "abs(-5)", Category.NUMBERS);
    static final FunctionDefinition CEIL = new FunctionDefinition("ceil", 1, 1, "x", "ceil(1/3)", Category.NUMBERS);
    static final FunctionDefinition FLOOR = new FunctionDefinition("floor", 1, 1, "x", "floor(5/3)", Category.NUMBERS);
    static final FunctionDefinition ROUND = new FunctionDefinition("round", 1, 2, "x[, digits]", "round(3.14159,2)", Category.NUMBERS);
    static final FunctionDefinition CEILDIV = new FunctionDefinition("ceildiv", 2, 2, "a, b", "ceildiv(130,64)", Category.NUMBERS);
    static final FunctionDefinition GCD = new FunctionDefinition("gcd", 2, MAX_ARGUMENTS, "a, b, ...", "gcd(12,18)", Category.NUMBERS);
    static final FunctionDefinition LCM = new FunctionDefinition("lcm", 2, MAX_ARGUMENTS, "a, b, ...", "lcm(12,18)", Category.NUMBERS);
    static final FunctionDefinition HYPOT = new FunctionDefinition("hypot", 2, MAX_ARGUMENTS, "x, y, ...", "hypot(3,4)", Category.GEOMETRY);
    static final FunctionDefinition RAD = new FunctionDefinition("rad", 1, 1, "degrees", "rad(180)", Category.GEOMETRY);
    static final FunctionDefinition DEG = new FunctionDefinition("deg", 1, 1, "radians", "deg(pi)", Category.GEOMETRY);
    static final FunctionDefinition LN = new FunctionDefinition("ln", 1, 1, "x", "ln(e)", Category.LOGARITHMS);
    static final FunctionDefinition LG = new FunctionDefinition("lg", 1, 1, "x", "lg(100)", Category.LOGARITHMS);
    static final FunctionDefinition LOG2 = new FunctionDefinition("log2", 1, 1, "x", "log2(8)", Category.LOGARITHMS);
    static final FunctionDefinition LOG = new FunctionDefinition("log", 2, 2, "base, x", "log(2,8)", Category.LOGARITHMS);
    static final FunctionDefinition ATAN2 = new FunctionDefinition("atan2", 2, 2, "y, x", "atan2(1,1)", Category.TRIGONOMETRY);
    static final FunctionDefinition MIN = new FunctionDefinition("min", 1, MAX_ARGUMENTS, "a, ...", "min(1,2,3)", Category.COLLECTIONS);
    static final FunctionDefinition MAX = new FunctionDefinition("max", 1, MAX_ARGUMENTS, "a, ...", "max(1,2,3)", Category.COLLECTIONS);
    static final FunctionDefinition SUM = new FunctionDefinition("sum", 1, MAX_ARGUMENTS, "a, ...", "sum(1,2,3)", Category.COLLECTIONS);
    static final FunctionDefinition AVG = new FunctionDefinition("avg", 1, MAX_ARGUMENTS, "a, ...", "avg(1,2,3)", Category.COLLECTIONS);
    static final FunctionDefinition RANDOM = new FunctionDefinition("random", 0, 0, "", "random()", Category.OTHER);
    static final FunctionDefinition N = new FunctionDefinition("n", 1, 2, "expression[, digits]", "n(sqrt(2),30)", Category.OTHER);

    static final List<FunctionDefinition> DEFINITIONS = List.of(
        SIN, COS, TAN, ASIN, ACOS, ATAN, ATAN2, SINH, COSH, TANH,
        ABS, SQRT, LN, LG, LOG, LOG2, CEIL, FLOOR, ROUND, CEILDIV,
        GCD, LCM, HYPOT, RAD, DEG, MIN, MAX, SUM, AVG, RANDOM, N);
    static final Map<String, FunctionDefinition> FUNCTIONS;

    static {
        Map<String, FunctionDefinition> functions = new LinkedHashMap<>();
        DEFINITIONS.forEach(function -> functions.put(function.name(), function));
        FUNCTIONS = Map.copyOf(functions);
    }

    public static FunctionDefinition find(String name) {
        return FUNCTIONS.get(name.toLowerCase(Locale.ROOT));
    }

    public String syntax() {
        return name + "(" + parameters + ")";
    }

    public String insertion() {
        return name + (maximumArguments == 0 ? "()" : "(");
    }
}
