package io.github.piscescup.calccmd.calculation;

import java.util.List;
import java.util.Set;

/**
 * Differentiation variables preserve order and repetitions such as x,x.
 */
public final class DiffRequest extends CalcRequest {
    record Assignment(String variable, Part value) {}

    private final List<String> variables;
    private final List<Assignment> assignments;

    DiffRequest(String source, Part expression, Set<String> symbols, List<String> variables, List<Assignment> assignments) {
        super(Operation.DIFF, source, expression, symbols);
        this.variables = List.copyOf(variables);
        this.assignments = List.copyOf(assignments);
    }

    public List<String> variables() {
        return variables;
    }

    public List<String> pointSources() {
        return assignments.stream().map(point -> point.variable() + "=" + point.value().text()).toList();
    }

    List<Assignment> assignments() { return assignments; }
}
