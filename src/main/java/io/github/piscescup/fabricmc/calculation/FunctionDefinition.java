package io.github.piscescup.fabricmc.calculation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.piscescup.fabricmc.calculation.CalculatorEngine.MAX_ARGUMENTS;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
record FunctionDefinition(
    String name,
    int minimumArguments, int maximumArguments
) {
    public static final FunctionDefinition SIN = new FunctionDefinition("sin", 1, 1);
    public static final FunctionDefinition COS = new FunctionDefinition("cos", 1, 1);
    public static final FunctionDefinition TAN = new FunctionDefinition("tan", 1, 1);
    public static final FunctionDefinition ASIN = new FunctionDefinition("asin", 1, 1);
    public static final FunctionDefinition ACOS = new FunctionDefinition("acos", 1, 1);
    public static final FunctionDefinition ATAN = new FunctionDefinition("atan", 1, 1);
    public static final FunctionDefinition SINH = new FunctionDefinition("sinh", 1, 1);
    public static final FunctionDefinition COSH = new FunctionDefinition("cosh", 1, 1);
    public static final FunctionDefinition TANH = new FunctionDefinition("tanh", 1, 1);
    public static final FunctionDefinition SQRT = new FunctionDefinition("sqrt", 1, 1);
    public static final FunctionDefinition ABS = new FunctionDefinition("abs", 1, 1);
    public static final FunctionDefinition CEIL = new FunctionDefinition("ceil", 1, 1);
    public static final FunctionDefinition FLOOR = new FunctionDefinition("floor", 1, 1);
    public static final FunctionDefinition ROUND = new FunctionDefinition("round", 1, 1);
    public static final FunctionDefinition LN = new FunctionDefinition("ln", 1, 1);
    public static final FunctionDefinition LG = new FunctionDefinition("lg", 1, 1);
    public static final FunctionDefinition LOG2 = new FunctionDefinition("log2", 1, 1);
    public static final FunctionDefinition LOG = new FunctionDefinition("log", 2, 2);
    public static final FunctionDefinition ATAN2 = new FunctionDefinition("atan2", 2, 2);
    public static final FunctionDefinition MIN = new FunctionDefinition("min", 1, MAX_ARGUMENTS);
    public static final FunctionDefinition MAX = new FunctionDefinition("max", 1, MAX_ARGUMENTS);
    public static final FunctionDefinition SUM = new FunctionDefinition("sum", 1, MAX_ARGUMENTS);
    public static final FunctionDefinition AVG = new FunctionDefinition("avg", 1, MAX_ARGUMENTS);
    public static final FunctionDefinition RANDOM = new FunctionDefinition("random", 0, 0);
    public static final FunctionDefinition N = new FunctionDefinition("n", 1, 2);

    /** Shared immutable registry used for validation and command suggestions. */
    public static final Map<String, FunctionDefinition> FUNCTIONS = createFunctions();

    private static Map<String, FunctionDefinition> createFunctions() {
        Map<String, FunctionDefinition> names = new LinkedHashMap<>();
        for (FunctionDefinition function : List.of(
                SIN, COS, TAN, ASIN, ACOS, ATAN, SINH, COSH, TANH,
                SQRT, ABS, CEIL, FLOOR, ROUND, LN, LG, LOG2,
                LOG, ATAN2, MIN, MAX, SUM, AVG, RANDOM, N)) {
            names.put(function.name(), function);
        }
        return Map.copyOf(names);
    }
}
