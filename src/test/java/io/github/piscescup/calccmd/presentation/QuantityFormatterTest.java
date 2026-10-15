package io.github.piscescup.calccmd.presentation;

import io.github.piscescup.calccmd.calculation.CalcRequest.Operation;
import io.github.piscescup.calccmd.config.Options;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.matheclipse.core.expression.F;

import static org.junit.jupiter.api.Assertions.*;

public class QuantityFormatterTest {
    private QuantityFormatter.Settings settings(boolean enabled, Options.FormatMode mode, String template, int stack) {
        return new QuantityFormatter.Settings(enabled, Set.of(Operation.EXPRESSION), mode, template, stack);
    }

    @Test
    public void enormousScientificExponentsFallBackWithoutExpansion() {
        var settings = settings(true, Options.FormatMode.ALL, "item", 64);
        assertTimeout(Duration.ofSeconds(1), () -> {
            for (String value : new String[] {"8.002981770660972533E434294481", "1E-434294481",
                    "1E2147483647", "1E-2147483647", "NaN", "Infinity"}) {
                assertTrue(QuantityFormatter.formatApproximate(Operation.EXPRESSION, value, settings).isEmpty(), value);
            }
        });
    }

    @Test
    public void disabledAndExcludedOperationsSkipConversion() {
        var disabled = settings(false, Options.FormatMode.ALL, "item", 64);
        assertTrue(QuantityFormatter.formatApproximate(Operation.EXPRESSION, null, disabled).isEmpty());
        var enabled = settings(true, Options.FormatMode.ALL, "item", 64);
        assertTrue(QuantityFormatter.formatApproximate(Operation.FACTOR, null, enabled).isEmpty());
    }

    @Test
    public void decomposesAllTemplateSizesAndStackSizes() {
        assertEquals("1795=1 S.B.(s) + 1 Stack(s) + 3 Item(s) at Stack = 64",
            QuantityFormatter.format(Operation.EXPRESSION, F.ZZ(1795),
                settings(true, Options.FormatMode.INTEGER, "shulker-stack-item", 64)).orElseThrow());
        assertEquals("35=2组 + 3件 at Stack = 16", QuantityFormatter.format(Operation.EXPRESSION, F.ZZ(35),
            settings(true, Options.FormatMode.ALL, "{}组 + {}件", 16)).orElseThrow());
        assertEquals("35=35 Item(s) at Stack = 1", QuantityFormatter.format(Operation.EXPRESSION, F.ZZ(35),
            settings(true, Options.FormatMode.ALL, "item", 1)).orElseThrow());
    }

    @Test
    public void preservesExactFractionsAndCombinesApproximateRemainders() {
        var settings = settings(true, Options.FormatMode.ALL, "stack-item", 64);
        assertEquals("327803/2500=2 Stack(s) + (3+303/2500) Item(s) at Stack = 64",
            QuantityFormatter.format(Operation.EXPRESSION, F.QQ(327803, 2500), settings).orElseThrow());
        assertEquals("131.1212=2 Stack(s) + 3.1212 Item(s) at Stack = 64",
            QuantityFormatter.formatApproximate(Operation.EXPRESSION, "131.1212", settings).orElseThrow());
        assertTrue(QuantityFormatter.formatApproximate(Operation.EXPRESSION, "1E-6", settings).isPresent());
    }

    @Test
    public void enforcesValueModesAndOutputLimits() {
        var integers = settings(true, Options.FormatMode.INTEGER, "item", 64);
        var fractions = settings(true, Options.FormatMode.DOUBLE, "item", 64);
        assertTrue(QuantityFormatter.format(Operation.EXPRESSION, F.C1D2, integers).isEmpty());
        assertTrue(QuantityFormatter.format(Operation.EXPRESSION, F.C1, fractions).isEmpty());
        assertTrue(QuantityFormatter.format(Operation.EXPRESSION, F.CN1, integers).isEmpty());
        assertTrue(QuantityFormatter.format(Operation.EXPRESSION, F.CI, integers).isEmpty());
        assertTrue(QuantityFormatter.formatApproximate(Operation.EXPRESSION, "1E4095", integers).isEmpty());
    }
}
