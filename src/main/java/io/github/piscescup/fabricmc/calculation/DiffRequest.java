package io.github.piscescup.fabricmc.calculation;

import java.util.List;
import java.util.Set;

/**
 * Differentiation variables preserve order and repetitions such as x,x.
 */
public final class DiffRequest extends CalcRequest {
    private final List<String> variables;

    DiffRequest(String source, Part expression, Set<String> symbols, List<String> variables) {
        super(Operation.DIFF, source, expression, symbols);
        this.variables = List.copyOf(variables);
    }

    public List<String> variables() {
        return variables;
    }
}
