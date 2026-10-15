package io.github.piscescup.calccmd.calculation;

import java.util.Set;

/** Ordinary evaluation, expansion, factorization and simplification only need an expression. */
final class ExpressionRequest extends CalcRequest {
    ExpressionRequest(Operation operation, String source, Part expression, Set<String> symbols) {
        super(operation, source, expression, symbols);
        if (operation != Operation.EXPRESSION && operation != Operation.EXPAND
                && operation != Operation.FACTOR && operation != Operation.SIMPLIFY) {
            throw new IllegalArgumentException("Not an expression operation: " + operation);
        }
    }
}
