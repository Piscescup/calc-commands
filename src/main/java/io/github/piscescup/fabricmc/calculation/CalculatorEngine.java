package io.github.piscescup.fabricmc.calculation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matheclipse.core.interfaces.IExpr;

/**
 * Validation never evaluates expressions or initializes Symja.
 */
public final class CalculatorEngine {
    public static final int MAX_LENGTH = 4096;
    static final int MAX_TOKENS = 2048;
    static final int MAX_DEPTH = 128;
    static final int MAX_ARGUMENTS = 64;
    static final int MAX_PRECISION = 1000;
    static final int MAX_NUMBER_EXPONENT = 4096;
    static final int MAX_OUTPUT_LENGTH = 4096;
    static final int MAX_SYMBOLIC_ITEMS = 16;

    /**
     * Keep the symbolic value for ans, separately from display text.
     */
    public record Result(
        IExpr value,
        String exact,
        String approximate
    ) {
    }

    private final Map<String, FunctionDefinition> functions = FunctionDefinition.FUNCTIONS;

    private SymbolicEvaluator evaluator;

    void validate(String source) {
        new ExpressionValidator(source, functions).validate();
    }

    void validate(String source, Set<String> symbols) {
        new ExpressionValidator(source, functions, symbols).validate();
    }

    boolean reservedName(String name) {
        return functions.containsKey(name) || Set.of("pi", "e", "ans")
            .contains(name);
    }

    public CalcRequest validate(CalcRequest.Operation operation, String source) {
        return SymbolicRequestParser.parse(operation, source, this);
    }

    public ExpressionValidator.Completion complete(CalcRequest.Operation operation, String source) {
        Set<String> symbols = operation == CalcRequest.Operation.EXPRESSION
            ? Set.of() : Set.of("x", "y", "z", "t");
        try {
            return new ExpressionValidator(source, functions, symbols).complete();
        } catch (CalcCommandsException ignored) {
            return new ExpressionValidator.Completion(source.length(), List.of());
        }
    }

    public List<String> functionNames() {
        return functions.keySet()
            .stream()
            .sorted()
            .toList();
    }

    /**
     * Call from a background worker. Preparing the engine never changes a calculator answer.
     */
    public synchronized void warmUp() {
        if (evaluator == null) evaluator = new SymbolicEvaluator();
        evaluator.warmUp();
    }

    public synchronized Result evaluate(CalcRequest request, Result answer) {
        CalcRequest validated = validate(request.operation(), request.source());
        if (evaluator == null) evaluator = new SymbolicEvaluator();
        return evaluator.evaluate(validated, answer);
    }
}
