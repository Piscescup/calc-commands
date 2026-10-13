package io.github.piscescup.fabricmc.commands;

import io.github.piscescup.fabricmc.calculation.CalcRequest;
import io.github.piscescup.fabricmc.calculation.CalculatorEngine;
import io.github.piscescup.fabricmc.config.NumFormatConfig;
import java.util.Set;

import io.github.piscescup.fabricmc.config.Options;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matheclipse.core.expression.F;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CalculationFeedbackTest {
    private static CalculatorEngine engine;
    private boolean previousEnabled;
    private int previousStackSize;
    private Set<CalcRequest.Operation> previousScope;
    private Options.FormatMode previousMode;
    private Options.TemplateOption previousTemplate;

    @BeforeAll
    public static void initializeEngine() {
        engine = new CalculatorEngine();
        engine.warmUp();
    }

    @BeforeEach
    public void enableFormattingInMemory() {
        // Direct configuration assignment changes memory only; no player files are written.
        previousEnabled = NumFormatConfig.enabled();
        previousStackSize = NumFormatConfig.stackSize();
        previousScope = NumFormatConfig.scope();
        previousMode = NumFormatConfig.formatMode();
        previousTemplate = NumFormatConfig.TEMPLATE_CONFIG.value();
        NumFormatConfig.ENABLED_CONFIG.setValue(true);
        NumFormatConfig.STACK_CONFIG.setValue(64);
        NumFormatConfig.SCOPE_CONFIG.setValue(Set.of(
            CalcRequest.Operation.EXPRESSION, CalcRequest.Operation.INTEGRATE,
            CalcRequest.Operation.DIFF, CalcRequest.Operation.LIMIT, CalcRequest.Operation.SOLVE));
        NumFormatConfig.FORMAT_CONFIG.setValue(Options.FormatMode.ALL);
        NumFormatConfig.TEMPLATE_CONFIG.setValue(Options.TemplateOption.STACK_ITEM_TEMPLATE);
    }

    @AfterEach
    public void restoreFormatting() {
        NumFormatConfig.ENABLED_CONFIG.setValue(previousEnabled);
        NumFormatConfig.STACK_CONFIG.setValue(previousStackSize);
        NumFormatConfig.SCOPE_CONFIG.setValue(previousScope);
        NumFormatConfig.FORMAT_CONFIG.setValue(previousMode);
        NumFormatConfig.TEMPLATE_CONFIG.setValue(previousTemplate);
    }

    @Test
    public void formatsNumericDerivativeAndLimitResultsAndPreservesMathematicalValues() {
        for (CalcRequest request : new CalcRequest[] {
            engine.validate(CalcRequest.Operation.DIFF, "x^2 x --at x=64"),
            engine.validate(CalcRequest.Operation.LIMIT, "x x 128")
        }) {
            CalculatorEngine.Result result = engine.evaluate(request, null);
            String before = result.value().toString();
            assertTrue(CalculationFeedback.format(request, result).getString()
                .contains("128=2 Stack(s) + 0 Item(s) at Stack = 64"));
            assertEquals("128", result.exact());
            assertEquals(before, result.value().toString());
        }
        assertTrue(format(CalcRequest.Operation.DIFF, "sqrt(2)*x x").contains(" at Stack = 64"));
        assertFalse(format(CalcRequest.Operation.DIFF, "x^2 x").contains(" at Stack = 64"));
        assertFalse(format(CalcRequest.Operation.DIFF, "-128*x x").contains(" at Stack = 64"));
        assertFalse(format(CalcRequest.Operation.LIMIT, "1/x x 0 right").contains(" at Stack = 64"));
        assertTrue(NumFormatConfig.format(CalcRequest.Operation.FACTOR, F.ZZ(128)).isEmpty());
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
    }

    @Test
    public void keepsNoSolutionsAndUnrestrictedSolutionsOnIndentedLines() {
        String empty = format(CalcRequest.Operation.SOLVE, "x^2+1=0 x");
        CalcRequest request = engine.validate(CalcRequest.Operation.SOLVE, "x=x x");
        var allValues = F.List(F.List());
        String unrestricted = CalculationFeedback.format(request,
            new CalculatorEngine.Result(allValues, "{{}}", null)).getString();
        assertTrue(empty.substring(empty.lastIndexOf('\n') + 1).startsWith("    "));
        assertTrue(unrestricted.substring(unrestricted.lastIndexOf('\n') + 1).startsWith("    "));
        assertFalse(empty.contains(" at Stack = 64"));
        assertFalse(unrestricted.contains(" at Stack = 64"));
    }

    private static String format(CalcRequest.Operation operation, String source) {
        CalcRequest request = engine.validate(operation, source);
        return CalculationFeedback.format(request, engine.evaluate(request, null)).getString();
    }

    @Test
    public void displaysComplexSolutionsWithIWhileFormattingRealAssignments() {
        CalcRequest request = engine.validate(CalcRequest.Operation.SOLVE, "x=I;y=128 x,y");
        CalculatorEngine.Result result = engine.evaluate(request, null, true);
        String text = CalculationFeedback.format(request, result).getString();
        assertTrue(text.contains("\n    x = I\n    y = 128=2 Stack(s) + 0 Item(s) at Stack = 64"));
        assertTrue(text.contains("I"));
        assertEquals("{{x->I,y->128}}", result.exact());
    }
}
