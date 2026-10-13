package io.github.piscescup.fabricmc.commands;

import io.github.piscescup.fabricmc.calculation.CalcRequest;
import io.github.piscescup.fabricmc.calculation.CalculatorEngine;
import io.github.piscescup.fabricmc.presentation.QuantityFormatter;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;

import io.github.piscescup.fabricmc.config.Options;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.matheclipse.core.expression.F;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CalculationFeedbackTest {
    private static CalculatorEngine engine;
    private static final QuantityFormatter.Settings QUANTITIES = new QuantityFormatter.Settings(true,
        Set.of(CalcRequest.Operation.EXPRESSION, CalcRequest.Operation.INTEGRATE, CalcRequest.Operation.DIFF,
            CalcRequest.Operation.LIMIT, CalcRequest.Operation.SOLVE),
        Options.FormatMode.ALL, Options.TemplateOption.STACK_ITEM_TEMPLATE.template(), 64);
    private static final CalculationFeedback FEEDBACK = new CalculationFeedback(QUANTITIES, Options.PowerOrder.ASCENDING);
    private static final CalculationFeedback MATH = new CalculationFeedback(
        new QuantityFormatter.Settings(false, Set.of(), Options.FormatMode.ALL, "{}", 64), Options.PowerOrder.ASCENDING);

    @BeforeAll
    public static void initializeEngine() {
        engine = new CalculatorEngine();
        engine.warmUp();
    }

    @Test
    public void formatsNumericDerivativeAndLimitResultsAndPreservesMathematicalValues() {
        for (CalcRequest request : new CalcRequest[] {
            engine.validate(CalcRequest.Operation.DIFF, "x^2 x --at x=64"),
            engine.validate(CalcRequest.Operation.LIMIT, "x x 128")
        }) {
            CalculatorEngine.Result result = engine.evaluate(request, null);
            String before = result.value().toString();
            assertTrue(FEEDBACK.render(request, result).getString()
                .contains("128=2 Stack(s) + 0 Item(s) at Stack = 64"));
            assertEquals("128", result.exact());
            assertEquals(before, result.value().toString());
        }
        assertTrue(format(CalcRequest.Operation.DIFF, "sqrt(2)*x x").contains(" at Stack = 64"));
        assertFalse(format(CalcRequest.Operation.DIFF, "x^2 x").contains(" at Stack = 64"));
        assertFalse(format(CalcRequest.Operation.DIFF, "-128*x x").contains(" at Stack = 64"));
        assertFalse(format(CalcRequest.Operation.LIMIT, "1/x x 0 right").contains(" at Stack = 64"));
        assertTrue(QuantityFormatter.format(CalcRequest.Operation.FACTOR, F.ZZ(128), QUANTITIES).isEmpty());
    }

    @Test
    public void putsEachSystemVariableOnItsOwnIndentedLine() {
        String single = format(CalcRequest.Operation.SOLVE, "2*x+y=10;x+y=7 x,y");
        assertTrue(single.contains("\n    x = 3=0 Stack(s) + 3 Item(s) at Stack = 64"));
        assertTrue(single.contains("\n    y = 4=0 Stack(s) + 4 Item(s) at Stack = 64"));

        String multiple = format(CalcRequest.Operation.SOLVE, "x^2-4=0;y=x+1 x,y");
        assertEquals(2, multiple.lines().filter(line -> line.startsWith("      x = ")).count());
        assertEquals(2, multiple.lines().filter(line -> line.startsWith("      y = ")).count());
        assertTrue(multiple.contains("\n      x = −2\n      y = −1"));
        assertTrue(multiple.contains("\n      x = 2=0 Stack(s) + 2 Item(s) at Stack = 64"));

        CalcRequest system = engine.validate(CalcRequest.Operation.SOLVE, "2*x+y=10;x+y=7 x,y");
        Component feedback = MATH.render(system, engine.evaluate(system, null));
        assertTrue(feedback.getString().contains("2 × x + y = 10;  x + y = 7"));
        assertTrue(feedback.getString().contains("\n    x = 3\n    y = 4"));
        assertEquals(List.of("x = 3", "y = 4"), copies(feedback).stream().map(Component::getString).toList());
    }

    @Test
    public void keepsNoSolutionsAndUnrestrictedSolutionsOnIndentedLines() {
        String empty = format(CalcRequest.Operation.SOLVE, "x^2+1=0 x");
        CalcRequest request = engine.validate(CalcRequest.Operation.SOLVE, "x=x x");
        var allValues = F.List(F.List());
        String unrestricted = FEEDBACK.render(request,
            new CalculatorEngine.Result(allValues, "{{}}", null)).getString();
        assertTrue(empty.substring(empty.lastIndexOf('\n') + 1).startsWith("    "));
        assertTrue(unrestricted.substring(unrestricted.lastIndexOf('\n') + 1).startsWith("    "));
        assertFalse(empty.contains(" at Stack = 64"));
        assertFalse(unrestricted.contains(" at Stack = 64"));
    }

    private static String format(CalcRequest.Operation operation, String source) {
        CalcRequest request = engine.validate(operation, source);
        return FEEDBACK.render(request, engine.evaluate(request, null)).getString();
    }

    @Test
    public void hugeApproximateResultsKeepScientificNotation() {
        CalcRequest request = engine.validate(CalcRequest.Operation.EXPRESSION, "n(e^1000000000,30)");
        var result = engine.evaluate(request, null);
        String text = FEEDBACK.render(request, result).getString();
        assertTrue(text.contains("434294481"), text);
        assertFalse(text.contains(" at Stack = "));
        assertTrue(text.length() < 1024);
    }

    @Test
    public void termOrderOnlyChangesPresentation() {
        CalcRequest request = engine.validate(CalcRequest.Operation.EXPAND, "(x+1)^2");
        var result = engine.evaluate(request, null);
        String ascending = FEEDBACK.render(request, result).getString();
        String descending = new CalculationFeedback(QUANTITIES, Options.PowerOrder.DESCENDING)
            .render(request, result).getString();
        assertTrue(ascending.contains("1 + 2 × x + x^2"));
        assertTrue(descending.contains("x^2 + 2 × x + 1"));
        assertEquals("1+2*x+x^2", result.exact());
    }

    @Test
    public void displaysComplexSolutionsWithIWhileFormattingRealAssignments() {
        CalcRequest request = engine.validate(CalcRequest.Operation.SOLVE, "x=I;y=128 x,y");
        CalculatorEngine.Result result = engine.evaluate(request, null, true);
        String text = FEEDBACK.render(request, result).getString();
        assertTrue(text.contains("\n    x = I\n    y = 128=2 Stack(s) + 0 Item(s) at Stack = 64"));
        assertTrue(text.contains("I"));
        assertEquals("{{x->I,y->128}}", result.exact());
    }

    @Test
    public void formattedExactAndApproximateResultsCopyTheirDisplayedText() {
        for (String input : List.of("128", "1/3", "sqrt(2)", "n(131.1212,30)")) {
            CalcRequest request = engine.validate(CalcRequest.Operation.EXPRESSION, input);
            var result = engine.evaluate(request, null);
            Component message = FEEDBACK.render(request, result);
            List<Component> copies = copies(message);
            assertEquals(result.approximate() == null ? 1 : 2, copies.size(), input);
            for (Component part : copies) {
                var event = (ClickEvent.CopyToClipboard) part.getStyle().getClickEvent();
                assertEquals(part.getString(), event.value());
                assertTrue(part.getStyle().isUnderlined());
                assertTrue(part.getStyle().getHoverEvent() instanceof net.minecraft.network.chat.HoverEvent.ShowText);
            }
        }
    }

    @Test
    public void symbolicResultsAndSolutionRowsAreAlsoCopyable() {
        for (CalcRequest request : new CalcRequest[] {
                engine.validate(CalcRequest.Operation.DIFF, "x^3 x"),
                engine.validate(CalcRequest.Operation.SOLVE, "2*x+y=10;x+y=7 x,y")}) {
            var result = engine.evaluate(request, null);
            Component message = FEEDBACK.render(request, result);
            List<Component> copies = copies(message);
            assertEquals(request.operation() == CalcRequest.Operation.SOLVE ? 2 : 1, copies.size());
            for (Component part : copies) {
                assertEquals(part.getString(), ((ClickEvent.CopyToClipboard) part.getStyle().getClickEvent()).value());
                assertTrue(part.getStyle().isUnderlined());
            }
        }
    }

    @Test
    public void copiesTheOriginalCalculationAlongsideItsResultWithoutReplacingAns() {
        CalcRequest sine = engine.validate(CalcRequest.Operation.EXPRESSION, "sin(2*pi)");
        assertEquals("sin(2 × π) = 0", copies(MATH.render(sine, engine.evaluate(sine, null))).getFirst().getString());

        var previous = engine.evaluate(engine.validate(CalcRequest.Operation.EXPRESSION, "7"), null);
        CalcRequest reuse = engine.validate(CalcRequest.Operation.EXPRESSION, "ans*2");
        var result = engine.evaluate(reuse, previous);
        assertEquals("ans × 2 = 14", copies(MATH.render(reuse, result)).getFirst().getString());
        assertEquals("14", result.exact());
        assertEquals("7", previous.exact());

        CalcRequest fraction = engine.validate(CalcRequest.Operation.EXPRESSION, "1/3");
        List<Component> rows = copies(MATH.render(fraction, engine.evaluate(fraction, null)));
        assertEquals("1/3 = 1/3", rows.getFirst().getString());
        assertTrue(rows.get(1).getString().startsWith("1/3 ≈ 0.333"));
        CalcRequest numeric = engine.validate(CalcRequest.Operation.EXPRESSION, "n(pi,20)");
        assertTrue(copies(MATH.render(numeric, engine.evaluate(numeric, null))).getFirst().getString()
            .startsWith("n(π, 20) ≈ 3.14159"));
    }

    @Test
    public void derivativeEquationsShowOrderAndEvaluationPoints() {
        String[][] cases = {
            {"x^3 x", "∂ (x^3) / ∂x = 3 × x^2"},
            {"x^3 x,x", "∂ (x^3) / ∂x∂x = 6 × x"},
            {"x^4 x,x,x", "∂ (x^4) / ∂x∂x∂x = 24 × x"},
            {"x^2*y^3 x,y", "∂ (x^2 × y^3) / ∂y∂x = 6 × x × y^2"},
            {"x^2*y^3 y,x", "∂ (x^2 × y^3) / ∂x∂y = 6 × x × y^2"},
            {"x^2*y x --at x=2,y=3", "∂ (x^2 × y) / ∂x = 12 at [x = 2, y = 3]"},
            {"sin(x*y)+x^2+y^2 x,y --at x=1,y=1", "∂ (sin(x × y) + x^2 + y^2) / ∂y∂x = cos(1) − sin(1) at [x = 1, y = 1]"}
        };
        for (String[] row : cases) {
            CalcRequest request = engine.validate(CalcRequest.Operation.DIFF, row[0]);
            assertEquals(row[1], copies(MATH.render(request, engine.evaluate(request, null))).getFirst().getString());
        }
    }

    @Test
    public void integralEquationsIncludeBoundsOrTheConstantWhileAnsExcludesIt() {
        CalcRequest indefinite = engine.validate(CalcRequest.Operation.INTEGRATE, "x^2 x");
        var antiderivative = engine.evaluate(indefinite, null);
        assertEquals("∫(x^2) dx = x^3/3 + C", copies(MATH.render(indefinite, antiderivative)).getFirst().getString());
        var derivative = engine.evaluate(engine.validate(CalcRequest.Operation.DIFF, "ans x"), antiderivative);
        assertEquals("x^2", derivative.exact());

        CalcRequest definite = engine.validate(CalcRequest.Operation.INTEGRATE, "sin(x) x --internal 0 pi");
        assertEquals("∫(sin(x)) dx = 2  [0 -> π]",
            copies(MATH.render(definite, engine.evaluate(definite, null))).getFirst().getString());
        CalcRequest reversed = engine.validate(CalcRequest.Operation.INTEGRATE, "sin(x) x --internal pi 0");
        assertEquals("∫(sin(x)) dx = −2  [π -> 0]",
            copies(MATH.render(reversed, engine.evaluate(reversed, null))).getFirst().getString());
        CalcRequest example = engine.validate(CalcRequest.Operation.INTEGRATE, "sin(x) x --internal 1 0");
        List<Component> rows = copies(MATH.render(example, engine.evaluate(example, null)));
        assertEquals("∫(sin(x)) dx = −1 + cos(1)  [1 -> 0]", rows.getFirst().getString());
        assertTrue(rows.stream().allMatch(row -> row.getString().endsWith("  [1 -> 0]")));
    }

    @Test
    public void limitEquationsPreserveThePointAndSide() {
        String[][] cases = {
            {"sin(x)/x x 0", "lim[x → (0)] (sin(x)/x) = 1"},
            {"abs(x)/x x 0 left", "lim[x → (0)⁻] (abs(x)/x) = −1"},
            {"abs(x)/x x 0 right", "lim[x → (0)⁺] (abs(x)/x) = 1"},
            {"1/x x inf", "lim[x → (inf)] (1/x) = 0"}
        };
        for (String[] row : cases) {
            CalcRequest request = engine.validate(CalcRequest.Operation.LIMIT, row[0]);
            assertEquals(row[1], copies(MATH.render(request, engine.evaluate(request, null))).getFirst().getString());
        }
    }

    private List<Component> copies(Component root) {
        List<Component> parts = new ArrayList<>();
        parts.add(root);
        for (int index = 0; index < parts.size(); index++) parts.addAll(parts.get(index).getSiblings());
        return parts.stream().filter(part -> part.getStyle().getClickEvent() instanceof ClickEvent.CopyToClipboard).toList();
    }
}
