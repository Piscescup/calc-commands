package io.github.piscescup.fabricmc.calculation;

import java.util.Set;

/**
 * Only definite integration stores a variable and lower/upper bounds.
 */
public final class IntegrateRequest extends CalcRequest {
    private final String variable;
    private final Part lowerBound;
    private final Part upperBound;

    IntegrateRequest(
        String source, Part expression, Set<String> symbols,
        String variable, Part lowerBound, Part upperBound
    ) {
        super(Operation.INTEGRATE, source, expression, symbols);
        this.variable = variable;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public String variable() {
        return variable;
    }

    public String lowerBoundSource() {
        return lowerBound.text();
    }

    public String upperBoundSource() {
        return upperBound.text();
    }

    Part lowerBound() {
        return lowerBound;
    }

    Part upperBound() {
        return upperBound;
    }
}
