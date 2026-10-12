package io.github.piscescup.fabricmc.calculation;

import java.util.Set;

/** Ordinary evaluation, expansion and factorization only need an expression. */
final class ExpressionRequest extends CalcRequest {
    ExpressionRequest(Operation operation, String source, Part expression, Set<String> symbols) {
        super(operation, source, expression, symbols);
        if (operation != Operation.EXPRESSION && operation != Operation.EXPAND && operation != Operation.FACTOR) {
            throw new IllegalArgumentException("Not an expression operation: " + operation);
        }
    }
}
