package io.github.piscescup.fabricmc.calculation;

import java.util.Set;

/**
 * Only definite integration stores a variable and lower/upper bounds.
 */
public final class IntegrateRequest extends CalcRequest {
    private final String variable;
    private final Part from;
    private final Part to;

    IntegrateRequest(
        String source, Part expression, Set<String> symbols,
        String variable, Part from, Part to
    ) {
        super(Operation.INTEGRATE, source, expression, symbols);
        this.variable = variable;
        this.from = from;
        this.to = to;
    }

    public String variable() {
        return variable;
    }

    public String lowerBoundSource() {
        return from.text();
    }

    public String upperBoundSource() {
        return to.text();
    }

    Part from() {
        return from;
    }

    Part to() {
        return to;
    }
}
