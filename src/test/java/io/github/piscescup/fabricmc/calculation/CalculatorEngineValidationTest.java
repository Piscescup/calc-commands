package io.github.piscescup.fabricmc.calculation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    }

    @Test
    public void integrateSyntaxUsesMathematicalParameterNames() {
        assertEquals(
            "/calc integrate <integrand> <variable> <lower-limit> <upper-limit>",
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
