package io.github.piscescup.fabricmc.calculation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

public class CalculatorEngineValidationTest {
    private CalculatorEngine engine;

    @BeforeEach
    public void createEngine() {
        engine = new CalculatorEngine();
    }

    @Test
    public void validatesEveryCalculationCommandShape() {
        CalcRequest expression = engine.validate(CalcRequest.Operation.EXPRESSION, "1+2*3");
        assertEquals("1+2*3", expression.expressionSource());

        CalcRequest expand = engine.validate(CalcRequest.Operation.EXPAND, "(x+1)^3");
        assertEquals(CalcRequest.Operation.EXPAND, expand.operation());

        CalcRequest factor = engine.validate(CalcRequest.Operation.FACTOR, "x^2-1");
        assertEquals(CalcRequest.Operation.FACTOR, factor.operation());

        DiffRequest diff = (DiffRequest) engine.validate(CalcRequest.Operation.DIFF, "x^2*y x,y");
        assertEquals(List.of("x", "y"), diff.variables());
        assertEquals("x^2*y", diff.expressionSource());

        IntegrateRequest integrate = (IntegrateRequest) engine.validate(
            CalcRequest.Operation.INTEGRATE, "sin(x) x 0 pi");
        assertEquals("x", integrate.variable());
        assertEquals("0", integrate.lowerBoundSource());
        assertEquals("pi", integrate.upperBoundSource());

        SolveRequest solve = (SolveRequest) engine.validate(
            CalcRequest.Operation.SOLVE, "2*x+y=10;x+y=7 x,y");
        assertEquals(List.of("x", "y"), solve.variables());
        assertEquals(2, solve.equations().size());
    }

    @Test
    public void reportsCommandSpecificInvalidInput() {
        assertError(CalcRequest.Operation.EXPRESSION, "sin(", "expected_operand");
        assertError(CalcRequest.Operation.EXPRESSION, "unknown(1)", "unknown_name");
        assertError(CalcRequest.Operation.SOLVE, "x+1 x", "expected_equation");
        assertError(CalcRequest.Operation.SOLVE, "x=1 x,x", "duplicate_variable");
        assertError(CalcRequest.Operation.DIFF, "x^2", "symbolic_arguments");
        assertError(CalcRequest.Operation.INTEGRATE, "x x,y 0 1", "single_variable");
    }

    @Test
    public void completionIncludesFunctionsConstantsAndSymbolicVariables() {
        ExpressionValidator.Completion function = engine.complete(CalcRequest.Operation.EXPRESSION, "sq");
        assertTrue(function.candidates().contains("sqrt("));

        ExpressionValidator.Completion constant = engine.complete(CalcRequest.Operation.EXPRESSION, "p");
        assertTrue(constant.candidates().contains("pi"));

        ExpressionValidator.Completion variable = engine.complete(CalcRequest.Operation.DIFF, "x+");
        assertTrue(variable.candidates().contains("x"));
        assertTrue(engine.complete(CalcRequest.Operation.EXPRESSION, "").candidates().contains("I"));
        assertTrue(engine.complete(CalcRequest.Operation.EXPRESSION, "i").candidates().contains("I"));
        engine.validate(CalcRequest.Operation.EXPRESSION, "I+i^2");
        engine.validate(CalcRequest.Operation.SOLVE, "x=2+3*I x");
        assertError(CalcRequest.Operation.SOLVE, "x=1 I", "invalid_variable");
        assertTrue(engine.complete(CalcRequest.Operation.DIFF, "x^2 x --at x=").candidates().contains("I"));
    }

    @Test
    public void validatesNewFunctionsAndRejectsWrongArgumentCounts() {
        for (String function : List.of("ceil", "floor", "round", "sum", "ceildiv", "gcd", "lcm", "hypot", "rad", "deg")) {
            assertTrue(engine.functionNames().contains(function), function);
            assertTrue(engine.complete(CalcRequest.Operation.EXPRESSION, function).candidates().contains(function + "("), function);
        }
        engine.validate(CalcRequest.Operation.EXPRESSION, "ROUND(3.14,2)+HYPOT(3,4)+sum(1,2)");
        for (String input : List.of("ceildiv(1)", "gcd(1)", "lcm()", "hypot(1)", "rad(1,2)", "round(1,2,3)")) {
            assertError(CalcRequest.Operation.EXPRESSION, input, "argument_count");
        }
    }

    @Test
    public void parsesNewSymbolicCommandsAndNamedSubstitutions() {
        engine.validate(CalcRequest.Operation.SIMPLIFY, "sin(x)^2+cos(x)^2");
        LimitRequest limit = (LimitRequest) engine.validate(CalcRequest.Operation.LIMIT, "sin(x)/x x 0 left");
        assertEquals(LimitRequest.Direction.LEFT, limit.direction());
        assertEquals("0", limit.pointSource());
        engine.validate(CalcRequest.Operation.LIMIT, "1/x x -inf");
        DiffRequest diff = (DiffRequest) engine.validate(CalcRequest.Operation.DIFF,
            "x^2*y x --at x=max(1,2),y=(pi / 2)");
        assertEquals(List.of("x=max(1,2)", "y=(pi / 2)"), diff.pointSources());
        IntegrateRequest integral = (IntegrateRequest) engine.validate(CalcRequest.Operation.INTEGRATE,
            "x^2 x --internal 0 3");
        assertTrue(integral.definite());
        assertEquals("x^2", integral.expressionSource());
        assertEquals("x", integral.variable());
        assertFalse(((IntegrateRequest) engine.validate(CalcRequest.Operation.INTEGRATE, "x + y + z x")).definite());
        assertFalse(((IntegrateRequest) engine.validate(CalcRequest.Operation.INTEGRATE, "x+ y +z x")).definite());
    }

    @Test
    public void reportsMalformedOptionsAndSubstitutionValuesAtTheirSourcePositions() {
        assertError(CalcRequest.Operation.INTEGRATE, "x^2 x --internal 0", "symbolic_arguments");
        assertError(CalcRequest.Operation.INTEGRATE, "x^2 --internal 0 3", "symbolic_arguments");
        assertError(CalcRequest.Operation.DIFF, "x^2 x --at", "symbolic_arguments");
        assertError(CalcRequest.Operation.DIFF, "x^2 x --at 2", "invalid_assignment");
        assertError(CalcRequest.Operation.DIFF, "x^2 x --at x=2,x=3", "duplicate_assignment");
        assertError(CalcRequest.Operation.DIFF, "x^2 x --at pi=2", "invalid_variable");
        assertError(CalcRequest.Operation.DIFF, "x^2 x --at x=y", "unknown_name");
        assertError(CalcRequest.Operation.DIFF, "x^2 x --at x=2,", "invalid_assignment");
        assertError(CalcRequest.Operation.LIMIT, "x x,y 0", "single_variable");
        assertError(CalcRequest.Operation.LIMIT, "x x y", "unknown_name");
        try {
            engine.validate(CalcRequest.Operation.DIFF, "x^2 x --at x=unknown");
            fail("Expected an unknown substitution constant error");
        } catch (CalcCommandsException error) {
            assertEquals(13, error.offset());
        }
    }

    @Test
    public void completesOptionsNamedAssignmentsNumericValuesAndLimitDirections() {
        assertTrue(engine.complete(CalcRequest.Operation.LIMIT, "sq").candidates().contains("sqrt("));
        assertTrue(engine.complete(CalcRequest.Operation.DIFF, "x + ").candidates().contains("sin("));
        assertTrue(engine.complete(CalcRequest.Operation.DIFF, "x^2 x --a").candidates().contains("--at "));
        assertTrue(engine.complete(CalcRequest.Operation.INTEGRATE, "x^2 x ").candidates().contains("--internal "));
        assertTrue(engine.complete(CalcRequest.Operation.DIFF, "x^2*y x --at ").candidates().containsAll(List.of("x=", "y=")));
        ExpressionValidator.Completion substitution = engine.complete(CalcRequest.Operation.DIFF, "x^2 x --at x=sq");
        assertEquals(13, substitution.offset());
        assertTrue(substitution.candidates().contains("sqrt("));
        assertTrue(engine.complete(CalcRequest.Operation.DIFF, "x^2*y x --at x=max(1,2),y=p").candidates().contains("pi"));
        assertTrue(engine.complete(CalcRequest.Operation.INTEGRATE, "x^2 x --internal 0 p").candidates().contains("pi"));
        assertTrue(engine.complete(CalcRequest.Operation.INTEGRATE, "x^2 x --internal 0 ").candidates().contains("sqrt("));
        assertTrue(engine.complete(CalcRequest.Operation.LIMIT, "1/x x in").candidates().contains("inf"));
        assertTrue(engine.complete(CalcRequest.Operation.LIMIT, "1/x x -in").candidates().contains("-inf"));
        assertTrue(engine.complete(CalcRequest.Operation.LIMIT, "sin(x)/x x 0 r").candidates().contains("right"));
        assertTrue(engine.complete(CalcRequest.Operation.LIMIT, "sin(x)/x x 0 ").candidates().contains("both"));
    }

    @Test
    public void integrateSyntaxUsesMathematicalParameterNames() {
        assertEquals(
            "/calc integrate <integrand> <variable> [--internal <lower-limit> <upper-limit>]",
            CalcRequest.Operation.INTEGRATE.syntax());
    }

    private void assertError(CalcRequest.Operation operation, String source, String key) {
        try {
            engine.validate(operation, source);
            fail("Expected " + key + " for: " + source);
        } catch (CalcCommandsException error) {
            assertEquals(key, error.key());
            assertTrue(error.offset() >= 0);
        }
    }
}
