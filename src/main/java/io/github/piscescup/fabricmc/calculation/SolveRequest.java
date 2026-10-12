package io.github.piscescup.fabricmc.calculation;

import java.util.List;
import java.util.Set;

/**
 * A system of equations and the variables to solve for.
 */
public final class SolveRequest extends CalcRequest {
    record Equation(Part left, Part right) {
    }

    private final List<String> variables;
    private final List<Equation> equations;

    SolveRequest(
        String source, Part expression, Set<String> symbols,
        List<String> variables, List<Equation> equations
    ) {
        super(Operation.SOLVE, source, expression, symbols);
        this.variables = List.copyOf(variables);
        this.equations = List.copyOf(equations);
    }

    public List<String> variables() {
        return variables;
    }

    List<Equation> equations() {
        return equations;
    }
}
