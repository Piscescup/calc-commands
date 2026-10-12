package io.github.piscescup.fabricmc.calculation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class CalculatorEngineEvaluationTest {
    private static CalculatorEngine engine;

    @BeforeAll
    public static void initializeEngine() {
        engine = new CalculatorEngine();
        engine.warmUp();
    }

    @Test
    public void evaluatesExactNumericExpressionsAndApproximations() {
        assertEquals("7", evaluate(CalcRequest.Operation.EXPRESSION, "1+2*3", null).exact());
        assertEquals("1/2", evaluate(CalcRequest.Operation.EXPRESSION, "1/3+1/6", null).exact());
        assertEquals("0", evaluate(CalcRequest.Operation.EXPRESSION, "sin(100*pi)", null).exact());

        CalculatorEngine.Result squareRoot = evaluate(CalcRequest.Operation.EXPRESSION, "sqrt(2)", null);
        assertEquals("Sqrt(2)", squareRoot.exact());
        assertTrue(squareRoot.approximate().startsWith("1.41421356237"));

        CalculatorEngine.Result numeric = evaluate(CalcRequest.Operation.EXPRESSION, "n(sqrt(2),20)", null);
        assertNull(numeric.approximate());
        assertTrue(numeric.exact().startsWith("1.41421356237"));
    }

    @Test
    public void reusesTheExactPreviousAnswer() {
        CalculatorEngine.Result first = evaluate(CalcRequest.Operation.EXPRESSION, "1/3", null);
        CalculatorEngine.Result second = evaluate(CalcRequest.Operation.EXPRESSION, "ans*6", first);
        assertEquals("2", second.exact());
    }

    @Test
    public void evaluatesEverySymbolicCommand() {
        assertEquals("1+3*x+3*x^2+x^3", evaluate(CalcRequest.Operation.EXPAND, "(x+1)^3", null).exact());
        assertEquals("(-1+x)*(1+x)", evaluate(CalcRequest.Operation.FACTOR, "x^2-1", null).exact());
        assertEquals("3*x^2", evaluate(CalcRequest.Operation.DIFF, "x^3 x", null).exact());
        assertEquals("9", evaluate(CalcRequest.Operation.INTEGRATE, "x^2 x 0 3", null).exact());

        String solutions = evaluate(CalcRequest.Operation.SOLVE, "x^2-4=0 x", null).exact();
        assertTrue(solutions.contains("x->-2"));
        assertTrue(solutions.contains("x->2"));
    }

    @Test
    public void evaluationErrorsDoNotProduceResults() {
        assertEvaluationError("1/0", "division_by_zero");
        assertEvaluationError("sqrt(-1)", "undefined_result");
    }

    private static CalculatorEngine.Result evaluate(
            CalcRequest.Operation operation, String source, CalculatorEngine.Result answer) {
        return engine.evaluate(engine.validate(operation, source), answer);
    }

    private static void assertEvaluationError(String source, String key) {
        try {
            evaluate(CalcRequest.Operation.EXPRESSION, source, null);
            fail("Expected evaluation error " + key);
        } catch (CalcCommandsException error) {
            assertEquals(key, error.key());
        }
    }
}
