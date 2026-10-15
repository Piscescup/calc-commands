package io.github.piscescup.calccmd.calculation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Map;
import org.matheclipse.core.interfaces.IAST;

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
    public void evaluatesInventoryGeometryAndRoundingFunctionsExactly() {
        Map<String, String> examples = Map.ofEntries(
            Map.entry("ceil(1/3)", "1"), Map.entry("floor(-1/3)", "-1"),
            Map.entry("sum(1,2,3)", "6"), Map.entry("ceildiv(130,64)", "3"),
            Map.entry("ceildiv(-130,64)", "-2"), Map.entry("ceildiv(130,-64)", "-2"),
            Map.entry("ceildiv(-130,-64)", "3"), Map.entry("ceildiv(0,64)", "0"),
            Map.entry("gcd(-12,18,30)", "6"), Map.entry("gcd(0,0)", "0"),
            Map.entry("gcd(6/2,9)", "3"), Map.entry("lcm(-12,18,30)", "180"),
            Map.entry("lcm(0,12)", "0"), Map.entry("hypot(3,4)", "5"),
            Map.entry("hypot(2,-3,6)", "7"), Map.entry("hypot(3/10,4/10)", "1/2"),
            Map.entry("sin(rad(30))", "1/2"), Map.entry("deg(pi/2)", "90"),
            Map.entry("round(3.14159,2)", "157/50"), Map.entry("round(1250,-2)", "1300"),
            Map.entry("round(-1.5)", "-1"), Map.entry("round(-1.25,1)", "-6/5"),
            Map.entry("round(1/8,1+1)", "13/100"), Map.entry("round(2,-1000)", "0"),
            Map.entry("round(2,1000)", "2"));
        examples.forEach((input, expected) ->
            assertEquals(expected, evaluate(CalcRequest.Operation.EXPRESSION, input, null).exact(), input));
        CalculatorEngine.Result angle = evaluate(CalcRequest.Operation.EXPRESSION, "rad(30)", null);
        assertEquals("1/2", evaluate(CalcRequest.Operation.EXPRESSION, "sin(ans)", angle).exact());
        assertEquals("100000000000000000000", evaluate(CalcRequest.Operation.EXPRESSION,
            "gcd(100000000000000000000,300000000000000000000)", null).exact());
    }

    @Test
    public void rejectsInvalidIntegerArgumentsAndRoundingPrecision() {
        for (String input : new String[] {"gcd(1.5,3)", "lcm(2,pi)", "ceildiv(2.5,2)"}) {
            assertEvaluationError(input, "integer_arguments");
        }
        assertEvaluationError("ceildiv(1,0)", "division_by_zero");
        for (String input : new String[] {"round(1,0.5)", "round(1,1001)", "round(1,-1001)",
                "round(1,100000000000000000000)"}) {
            assertEvaluationError(input, "invalid_round_digits");
        }
    }

    @Test
    public void simplifiesAndEvaluatesFiniteDirectionalAndInfiniteLimits() {
        assertEquals("1", evaluate(CalcRequest.Operation.SIMPLIFY, "sin(x)^2+cos(x)^2", null).exact());
        assertEquals("1+x", evaluate(CalcRequest.Operation.SIMPLIFY, "(x^2-1)/(x-1)", null).exact());
        assertEquals("1", evaluate(CalcRequest.Operation.LIMIT, "sin(x)/x x 0", null).exact());
        assertEquals("0", evaluate(CalcRequest.Operation.LIMIT, "1/x x inf", null).exact());
        assertEquals("0", evaluate(CalcRequest.Operation.LIMIT, "1/x x -INFINITY", null).exact());
        assertEquals("-1", evaluate(CalcRequest.Operation.LIMIT, "abs(x)/x x 0 left", null).exact());
        assertEquals("1", evaluate(CalcRequest.Operation.LIMIT, "abs(x)/x x 0 right", null).exact());
        assertEquals("Infinity", evaluate(CalcRequest.Operation.LIMIT, "1/x x 0 right", null).exact());
        assertEquals("-Infinity", evaluate(CalcRequest.Operation.LIMIT, "1/x x 0 left", null).exact());
        assertEvaluationError(CalcRequest.Operation.LIMIT, "abs(x)/x x 0 both", "undefined_result");
        assertEvaluationError(CalcRequest.Operation.LIMIT, "x x sqrt(-1)", "invalid_point");
    }

    @Test
    public void differentiatesBeforeSubstitutingNamedValuesIncludingParameters() {
        assertEquals("12", evaluate(CalcRequest.Operation.DIFF, "x^3 x --at x=2", null).exact());
        assertEquals("12", evaluate(CalcRequest.Operation.DIFF, "x^2*y x --at y=3,x=2", null).exact());
        assertEquals("12", evaluate(CalcRequest.Operation.DIFF, "x^3 x,x --at x=2", null).exact());
        assertEquals("108", evaluate(CalcRequest.Operation.DIFF, "x^2*y^3 x,y --at x=2,y=3", null).exact());
        assertEquals("1/2", evaluate(CalcRequest.Operation.DIFF, "sin(x) x --at x=(pi / 3)", null).exact());
        assertEquals("18", evaluate(CalcRequest.Operation.DIFF, "x^2 x --at x=max(2,9)", null).exact());
        assertEquals("6*x", evaluate(CalcRequest.Operation.DIFF, "x^2*y x --at y=3", null).exact());
        CalculatorEngine.Result first = evaluate(CalcRequest.Operation.DIFF, "x^3 x", null);
        assertEquals("12", evaluate(CalcRequest.Operation.DIFF, "ans x --at x=2", first).exact());
        assertEvaluationError(CalcRequest.Operation.DIFF, "x^2 x --at x=sqrt(-1)", "invalid_point");
    }

    @Test
    public void integratesWithOptionalInternalBoundsAndReusesAntiderivatives() {
        CalculatorEngine.Result primitive = evaluate(CalcRequest.Operation.INTEGRATE, "x^2 x", null);
        assertEquals("x^3/3", primitive.exact());
        assertEquals("x^2", evaluate(CalcRequest.Operation.DIFF, "ans x", primitive).exact());
        assertEquals("9", evaluate(CalcRequest.Operation.INTEGRATE, "x^2 x --internal 0 3", null).exact());
        assertEquals("2", evaluate(CalcRequest.Operation.INTEGRATE, "sin(x) x --internal 0 pi", null).exact());
        assertEquals("1", evaluate(CalcRequest.Operation.INTEGRATE, "cos(x) x --internal 0 (pi / 2)", null).exact());
        assertEquals("-9", evaluate(CalcRequest.Operation.INTEGRATE, "x^2 x --internal 3 0", null).exact());
        assertEquals("-Cos(x)", evaluate(CalcRequest.Operation.INTEGRATE, "sin(x) x", null).exact());
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
        assertEvaluationError("sqrt(-1)", "complex_disabled");
    }

    @Test
    public void complexModeControlsPolynomialAndSystemSolutions() {
        assertEquals("{}", evaluate(CalcRequest.Operation.SOLVE, "x^2+1=0 x", null).exact());
        String quadratic = complex(CalcRequest.Operation.SOLVE, "x^2+1=0 x", null).exact();
        assertTrue(quadratic.contains("x->I"));
        assertTrue(quadratic.contains("x->-I"));
        IAST cubic = (IAST) complex(CalcRequest.Operation.SOLVE, "x^3-1=0 x", null).value();
        assertEquals(4, cubic.size());
        assertEquals(2, ((IAST) evaluate(CalcRequest.Operation.SOLVE, "x^3-1=0 x", null).value()).size());
        assertEquals("{{x->I,y->-I}}", complex(CalcRequest.Operation.SOLVE, "x+y=0;x-y=2*I x,y", null).exact());
        assertEquals("{{x->I}}", complex(CalcRequest.Operation.SOLVE, "x=I x", null).exact());
        assertEquals("{}", evaluate(CalcRequest.Operation.SOLVE, "x=I x", null).exact());
    }

    @Test
    public void imaginaryUnitSupportsExactArithmeticApproximationsAndAnswerReuse() {
        assertEquals("-1", evaluate(CalcRequest.Operation.EXPRESSION, "I^2", null).exact());
        assertEquals("-1", evaluate(CalcRequest.Operation.EXPRESSION, "i*i", null).exact());
        assertEvaluationError("I", "complex_disabled");
        CalculatorEngine.Result imaginary = complex(CalcRequest.Operation.EXPRESSION, "sqrt(-1)", null);
        assertEquals("I", imaginary.exact());
        assertEquals("-1", evaluate(CalcRequest.Operation.EXPRESSION, "ans^2", imaginary).exact());
        assertEquals("I", complex(CalcRequest.Operation.EXPRESSION, "ans", imaginary).exact());
        assertEquals("5", complex(CalcRequest.Operation.EXPRESSION, "abs(3+4*I)", null).exact());
        assertEquals("1", complex(CalcRequest.Operation.EXPRESSION, "(2+3*I)/(2+3*i)", null).exact());
        CalculatorEngine.Result root = complex(CalcRequest.Operation.EXPRESSION, "sqrt(-2)", null);
        assertTrue(root.approximate().contains("1.41421356237"));
        assertTrue(root.approximate().contains("I"));
        assertEquals("1", complex(CalcRequest.Operation.EXPRESSION, "log(2,-1)/(I*pi/ln(2))", null).exact());
    }

    @Test
    public void complexDerivativeSubstitutionsDoNotChangeRealBoundRequirements() {
        CalculatorEngine.Result derivative = complex(CalcRequest.Operation.DIFF, "x^2 x --at x=I", null);
        assertEquals("-4", evaluate(CalcRequest.Operation.EXPRESSION, "ans^2", derivative).exact());
        assertEvaluationError(CalcRequest.Operation.DIFF, "x^2 x --at x=I", "invalid_point");
        try {
            complex(CalcRequest.Operation.LIMIT, "x x I", null);
            fail("Expected a real limit point");
        } catch (CalcCommandsException error) {
            assertEquals("invalid_point", error.key());
        }
        try {
            complex(CalcRequest.Operation.INTEGRATE, "x x --internal 0 I", null);
            fail("Expected real integration bounds");
        } catch (CalcCommandsException error) {
            assertEquals("invalid_bounds", error.key());
        }
    }

    private static CalculatorEngine.Result complex(
            CalcRequest.Operation operation, String source, CalculatorEngine.Result answer) {
        return engine.evaluate(engine.validate(operation, source), answer, true);
    }

    private static CalculatorEngine.Result evaluate(
            CalcRequest.Operation operation, String source, CalculatorEngine.Result answer) {
        return engine.evaluate(engine.validate(operation, source), answer);
    }

    private static void assertEvaluationError(String source, String key) {
        assertEvaluationError(CalcRequest.Operation.EXPRESSION, source, key);
    }

    private static void assertEvaluationError(CalcRequest.Operation operation, String source, String key) {
        try {
            evaluate(operation, source, null);
            fail("Expected evaluation error " + key);
        } catch (CalcCommandsException error) {
            assertEquals(key, error.key());
        }
    }
}
