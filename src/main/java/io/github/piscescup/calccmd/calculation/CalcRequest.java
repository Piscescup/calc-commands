package io.github.piscescup.calccmd.calculation;

import io.github.piscescup.calccmd.config.Option;

import java.util.Set;

/**
 * Common immutable input and diagnostics shared by all calculator requests.
 */
public abstract sealed class CalcRequest
    permits ExpressionRequest, SolveRequest, DiffRequest, IntegrateRequest, LimitRequest
{
    public enum Operation implements Option {
        EXPRESSION("expression", "<expression>"),
        SOLVE("solve", "<equations> <variables>"),
        EXPAND("expand", "<expression>"),
        FACTOR("factor", "<expression>"),
        SIMPLIFY("simplify", "<expression>"),
        LIMIT("limit", "<function> <variable> <point> [both|left|right]"),
        DIFF("diff", "<function> <variables> [--at <variable=value,...>]"),
        INTEGRATE("integrate", "<integrand> <variable> [--internal <lower-limit> <upper-limit>]");

        private final String command;
        private final String arguments;

        Operation(String command, String arguments) {
            this.command = command;
            this.arguments = arguments;
        }

        @Override
        public String getValue() {
            return command;
        }

        public String syntax() {
            return "/calc " + command + " " + arguments;
        }
    }

    record Part(
        String text,
        int offset
    ) {
    }

    private final Operation operation;
    private final String source;
    private final Part expression;
    private final Set<String> symbols;

    CalcRequest(Operation operation, String source, Part expression, Set<String> symbols) {
        this.operation = operation;
        this.source = source;
        this.expression = expression;
        this.symbols = Set.copyOf(symbols);
    }

    public final Operation operation() {
        return operation;
    }

    final String source() {
        return source;
    }

    public final String expressionSource() {
        return expression.text();
    }

    final Part expression() {
        return expression;
    }

    final Set<String> symbols() {
        return symbols;
    }
}
