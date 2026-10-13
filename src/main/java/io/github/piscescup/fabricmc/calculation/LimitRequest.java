package io.github.piscescup.fabricmc.calculation;

import java.util.Set;

/**
 * A real limit with an explicit approach point and optional direction.
 */
public final class LimitRequest extends CalcRequest {
    public enum Direction {
        BOTH,
        LEFT,
        RIGHT
    }

    private final String variable;
    private final Part point;
    private final Direction direction;

    LimitRequest(
        String source, Part expression, Set<String> symbols,
        String variable, Part point, Direction direction
    ) {
        super(Operation.LIMIT, source, expression, symbols);
        this.variable = variable;
        this.point = point;
        this.direction = direction;
    }

    public String variable() {
        return variable;
    }

    public String pointSource() {
        return point.text();
    }

    public Direction direction() {
        return direction;
    }

    Part point() {
        return point;
    }
}
