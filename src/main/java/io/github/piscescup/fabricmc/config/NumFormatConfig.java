package io.github.piscescup.fabricmc.config;

import io.github.piscescup.fabricmc.References;
import io.github.piscescup.fabricmc.calculation.CalcRequest;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IExpr;
import org.matheclipse.core.interfaces.IRational;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Number-format settings and presentation using the three predefined templates. */
public final class NumFormatConfig {
    public static final String ENABLED_KEY = "enabled";
    public static final String SCOPE_KEY = "scope";
    public static final String FORMAT_KEY = "format";
    public static final String TEMPLATE_KEY = "template";
    public static final String STACK_KEY = "stack";

    public static final int DEFAULT_STACK_SIZE = 64;
    private static final Set<CalcRequest.Operation> ALLOWED_OPERATIONS = Set.of(
        CalcRequest.Operation.EXPRESSION, CalcRequest.Operation.INTEGRATE,
        CalcRequest.Operation.DIFF, CalcRequest.Operation.LIMIT, CalcRequest.Operation.SOLVE
    );

    public static final BooleanConfig ENABLED_CONFIG = new BooleanConfig(false);

    public static final IntConfig STACK_CONFIG = new IntConfig(DEFAULT_STACK_SIZE, 1);

    public static final EnumSetConfig<CalcRequest.Operation> SCOPE_CONFIG =
        new EnumSetConfig<>(ALLOWED_OPERATIONS, ALLOWED_OPERATIONS);

    public static final OptionsConfig<Options.FormatMode> FORMAT_CONFIG = new OptionsConfig<>(
        Options.FormatMode.INTEGER, Options.FormatMode::fromValue, Options.FormatMode.values());

    public static final OptionsConfig<Options.TemplateOption> TEMPLATE_CONFIG = new OptionsConfig<>(
        Options.TemplateOption.SB_STACK_ITEM_TEMPLATE, Options.TemplateOption::fromValue, Options.TemplateOption.values());

    private static final ConfigStore STORE = new ConfigStore(FabricLoader.getInstance().getConfigDir()
        .resolve(References.MOD_ID)
        .resolve("config").resolve("num_fmt_cfg.json")
    )
        .register(ENABLED_KEY, ENABLED_CONFIG)
        .register(SCOPE_KEY, SCOPE_CONFIG)
        .register(FORMAT_KEY, FORMAT_CONFIG)
        .register(TEMPLATE_KEY, TEMPLATE_CONFIG)
        .register(STACK_KEY, STACK_CONFIG);

    static {
        STORE.load();
    }

    private NumFormatConfig() {}

    public static ConfigStore store() {
        return STORE;
    }

    public static void initialize() {
        STORE.initialize();
    }

    public static boolean enabled() {
        return STORE.get(ENABLED_CONFIG);
    }

    public static Set<CalcRequest.Operation> scope() {
        return STORE.get(SCOPE_CONFIG);
    }

    public static Options.FormatMode formatMode() {
        return STORE.get(FORMAT_CONFIG);
    }

    public static String template() {
        return templateText(STORE.get(TEMPLATE_CONFIG).getValue());
    }

    public static int stackSize() {
        return STORE.get(STACK_CONFIG);
    }

    public static void setEnabled(boolean enabled) throws IOException {
        STORE.set(ENABLED_CONFIG, enabled);
    }

    public static void setScope(CalcRequest.Operation operation, boolean included) throws IOException {
        if (operation == null || !ALLOWED_OPERATIONS.contains(operation)) {
            throw new IllegalArgumentException("Unsupported number-format scope: " + operation);
        }
        synchronized (STORE) {
            EnumSet<CalcRequest.Operation> selected = EnumSet.noneOf(CalcRequest.Operation.class);
            selected.addAll(scope());
            if (included) selected.add(operation);
            else selected.remove(operation);
            STORE.set(SCOPE_CONFIG, selected);
        }
    }

    public static void setTemplate(String template, int stackSize) throws IOException {
        setTemplate(TEMPLATE_CONFIG.parse(template), stackSize);
    }

    public static void setTemplate(Options.TemplateOption template, int stackSize) throws IOException {
        TEMPLATE_CONFIG.validate(template);
        STORE.update(Map.of(TEMPLATE_KEY, template.getValue(), STACK_KEY, Integer.toString(stackSize)));
    }

    public static void setFormatMode(Options.FormatMode mode) throws IOException {
        STORE.set(FORMAT_CONFIG, mode);
    }

    public static void reset() throws IOException {
        STORE.reset();
    }

    public static void resetScope() throws IOException {
        STORE.reset(SCOPE_KEY);
    }

    public static void resetFormatMode() throws IOException {
        STORE.reset(FORMAT_KEY);
    }

    public static void resetTemplate() throws IOException {
        STORE.reset(TEMPLATE_KEY, STACK_KEY);
    }

    /** Only finite non-negative rational values participate in quantity formatting. */
    public static Optional<String> format(CalcRequest.Operation operation, IExpr value) {
        if (!(value instanceof IRational rational)) return Optional.empty();
        return format(operation, value.toString(), rational, false);
    }

    public static Optional<String> formatApproximate(CalcRequest.Operation operation, String source) {
        try {
            BigDecimal decimal = new BigDecimal(source);
            BigInteger numerator = decimal.unscaledValue();
            int scale = decimal.scale();
            IRational value = scale < 0
                ? F.ZZ(numerator.multiply(BigInteger.TEN.pow(-scale)))
                : F.QQ(numerator, BigInteger.TEN.pow(scale)).normalize();
            return format(operation, source, value, true);
        } catch (IllegalArgumentException | ArithmeticException error) {
            return Optional.empty();
        }
    }

    private static Optional<String> format(CalcRequest.Operation operation, String source,
            IRational value, boolean decimalOutput) {
        value = value.normalize();
        synchronized (STORE) {
            if (value.isNegative() || !enabled() || !scope().contains(operation) || !formatMode().allows(value)) {
                return Optional.empty();
            }
            Options.TemplateOption selected = STORE.get(TEMPLATE_CONFIG);
            Object[] arguments = selected.format(value, stackSize());
            for (int index = 0; index < arguments.length - 1; index++) {
                IRational number = ((IRational) arguments[index]).normalize();
                arguments[index] = quantityText(number, decimalOutput);
            }
            return Optional.of(source + "="
                + ParameterizedMessage.format(templateText(selected.getValue()), arguments));
        }
    }

    private static String quantityText(IRational number, boolean decimalOutput) {
        if (number.isInteger()) return number.toString();
        IRational fraction = number.fractionalPart().normalize();
        String fractionText = decimalOutput
            ? new BigDecimal(fraction.toBigNumerator()).divide(new BigDecimal(fraction.toBigDenominator()))
                .stripTrailingZeros().toPlainString()
            : fraction.toString();
        return number.floor().isZero() ? fractionText : "(" + number.floor() + "+" + fractionText + ")";
    }

    private static String templateText(String value) {
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1).strip();
        }
        return value;
    }
}
