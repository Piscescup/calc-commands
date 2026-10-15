package io.github.piscescup.calccmd.presentation;

import io.github.piscescup.calccmd.calculation.CalcRequest;
import io.github.piscescup.calccmd.calculation.CalculatorEngine;
import io.github.piscescup.calccmd.config.Options;
import io.github.piscescup.calccmd.config.TemplateConfig;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IExpr;
import org.matheclipse.core.interfaces.IInteger;
import org.matheclipse.core.interfaces.IRational;

/** Bounded inventory-style presentation, independent of configuration files and Minecraft. */
public final class QuantityFormatter {
    private static final int MAX_LENGTH = CalculatorEngine.MAX_OUTPUT_LENGTH;
    private static final int SHULKER_STACKS = 27;

    public record Settings(boolean enabled, Set<CalcRequest.Operation> scope,
            Options.FormatMode mode, String template, int stackSize) {
        public Settings {
            scope = Set.copyOf(scope);
            java.util.Objects.requireNonNull(mode, "mode");
            template = new TemplateConfig().parse(template);
            if (stackSize < 1) throw new IllegalArgumentException("Stack size must be positive");
        }

        private boolean appliesTo(CalcRequest.Operation operation) {
            return enabled && scope.contains(operation);
        }
    }

    private QuantityFormatter() {}

    public static Optional<String> format(CalcRequest.Operation operation, IExpr value, Settings settings) {
        if (!settings.appliesTo(operation) || !(value instanceof IRational rational)) return Optional.empty();
        // Bound conversion to text as well as the arithmetic used for presentation.
        if (rational.toBigNumerator().bitLength() > MAX_LENGTH * 4
                || rational.toBigDenominator().bitLength() > MAX_LENGTH * 4) return Optional.empty();
        return format(rational.toString(), rational, false, settings);
    }

    public static Optional<String> formatApproximate(CalcRequest.Operation operation, String source, Settings settings) {
        if (!settings.appliesTo(operation) || source.length() > MAX_LENGTH) return Optional.empty();
        try {
            BigDecimal decimal = new BigDecimal(source).stripTrailingZeros();
            int scale = decimal.scale();
            // A short exponent can describe billions of digits. Check before allocating 10^scale.
            if (decimal.signum() < 0 || Math.abs((long) scale) > MAX_LENGTH
                    || (long) decimal.precision() - scale > MAX_LENGTH) return Optional.empty();
            BigInteger numerator = decimal.unscaledValue();
            IRational value = scale < 0
                ? F.ZZ(numerator.multiply(BigInteger.TEN.pow(-scale)))
                : F.QQ(numerator, BigInteger.TEN.pow(scale)).normalize();
            return format(source, value, true, settings);
        } catch (IllegalArgumentException | ArithmeticException error) {
            return Optional.empty();
        }
    }

    private static Optional<String> format(String source, IRational value, boolean decimalOutput, Settings settings) {
        value = value.normalize();
        if (source.length() > MAX_LENGTH || value.isNegative() || !settings.mode().allows(value)) return Optional.empty();
        String[] literals = settings.template().split("\\{\\}", -1);
        IRational[] quantities;
        if (literals.length == 2) {
            quantities = new IRational[] {value};
        } else {
            IInteger[] stacks = value.floor().divideAndRemainder(F.ZZ(settings.stackSize()));
            IRational items = value.fractionalPart().add(stacks[1]).normalize();
            if (literals.length == 3) {
                quantities = new IRational[] {stacks[0], items};
            } else {
                IInteger[] boxes = stacks[0].divideAndRemainder(F.ZZ(SHULKER_STACKS));
                quantities = new IRational[] {boxes[0], boxes[1], items};
            }
        }
        StringBuilder output = new StringBuilder(source).append('=');
        for (int index = 0; index < quantities.length; index++) {
            output.append(literals[index]).append(quantityText(quantities[index], decimalOutput));
        }
        output.append(literals[literals.length - 1]).append(" at Stack = ").append(settings.stackSize());
        return output.length() <= MAX_LENGTH ? Optional.of(output.toString()) : Optional.empty();
    }

    private static String quantityText(IRational value, boolean decimalOutput) {
        value = value.normalize();
        if (value.isInteger()) return value.toString();
        if (decimalOutput) {
            return new BigDecimal(value.toBigNumerator()).divide(new BigDecimal(value.toBigDenominator()))
                .stripTrailingZeros().toPlainString();
        }
        IRational fraction = value.fractionalPart().normalize();
        return value.floor().isZero() ? fraction.toString() : "(" + value.floor() + "+" + fraction + ")";
    }
}
