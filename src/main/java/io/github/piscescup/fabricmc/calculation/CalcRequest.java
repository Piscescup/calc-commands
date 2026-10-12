package io.github.piscescup.fabricmc.calculation;

import java.util.Set;

/** Common immutable input and diagnostics shared by all calculator requests. */
public abstract sealed class CalcRequest
        permits ExpressionRequest, SolveRequest, DiffRequest, IntegrateRequest
{
    public enum Operation {
        EXPRESSION("expression", "<expression>"),
        SOLVE("solve", "<equations> <variables>"),
        EXPAND("expand", "<expression>"),
        FACTOR("factor", "<expression>"),
        DIFF("diff", "<function> <variables>"),
        INTEGRATE("integrate", "<function> <variable> <from> <to>");

        private final String command;
        private final String arguments;

        Operation(String command, String arguments) {
            this.command = command;
            this.arguments = arguments;
        }

        public String command() {
            return command;
        }

        public String syntax() {
            return "/calc " + command + " " + arguments;
        }
    }

    record Part(String text, int offset) {}

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

    public final Operation operation() { return operation; }
    public final String source() { return source; }
    public final String expressionSource() { return expression.text(); }
    final Part expression() { return expression; }
    final Set<String> symbols() { return symbols; }
}
