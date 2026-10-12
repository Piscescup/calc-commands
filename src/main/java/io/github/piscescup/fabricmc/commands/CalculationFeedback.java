package io.github.piscescup.fabricmc.commands;

import io.github.piscescup.fabricmc.calculation.CalculatorEngine;
import io.github.piscescup.fabricmc.calculation.CalcRequest;
import io.github.piscescup.fabricmc.calculation.DiffRequest;
import io.github.piscescup.fabricmc.calculation.IntegrateRequest;
import io.github.piscescup.fabricmc.calculation.SolveRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.matheclipse.core.expression.S;
import org.matheclipse.core.interfaces.IAST;
import org.matheclipse.core.interfaces.IExpr;

/** Chat presentation only: mathematical values and ans keep their original exact representations. */
final class CalculationFeedback {
    private static final Pattern NUMBER = Pattern.compile("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[Ee][+-]?[0-9]+)?");
    private static final Pattern NAME = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");
    private static final Map<String, String> FUNCTIONS = Map.ofEntries(
        Map.entry("Sin", "sin"), Map.entry("Cos", "cos"), Map.entry("Tan", "tan"),
        Map.entry("Sinh", "sinh"), Map.entry("Cosh", "cosh"), Map.entry("Tanh", "tanh"),
        Map.entry("ArcSin", "asin"), Map.entry("ArcCos", "acos"), Map.entry("Sqrt", "sqrt"),
        Map.entry("Abs", "abs"), Map.entry("Exp", "exp"));

    private CalculationFeedback() {}

    static Component format(CalcRequest request, CalculatorEngine.Result result) {
        MutableComponent message = header(request.operation().command());
        String variables = switch (request) {
            case SolveRequest solve -> String.join(", ", solve.variables());
            case DiffRequest diff -> String.join(", ", diff.variables());
            case IntegrateRequest integral -> integral.variable();
            default -> "";
        };
        if (!variables.isEmpty()) {
            field(message, "variables_label", variables, ChatFormatting.WHITE);
        }
        String inputLabel = switch (request.operation()) {
            case SOLVE -> "equations_label";
            case DIFF, INTEGRATE -> "function_label";
            default -> "expression_label";
        };
        field(message, inputLabel, pretty(request.expressionSource()), ChatFormatting.GRAY);
        if (request instanceof DiffRequest diff && diff.variables().size() > 1) {
            field(message, "derivative_order_label", String.join(" → ", diff.variables()), ChatFormatting.WHITE);
        }
        if (request instanceof IntegrateRequest integral) {
            field(message, "from_label", pretty(integral.lowerBoundSource()), ChatFormatting.WHITE);
            field(message, "to_label", pretty(integral.upperBoundSource()), ChatFormatting.WHITE);
        }
        if (request instanceof SolveRequest && result.value() instanceof IAST solutions
                && solutions.isList()) {
            solutions(message, solutions);
        } else {
            String resultLabel = switch (request.operation()) {
                case DIFF -> "derivative_label";
                case INTEGRATE -> "integral_label";
                default -> "result_label";
            };
            result(message, result, resultLabel);
        }
        return message;
    }

    static Component error(Component error) {
        return Component.empty().append(prefix()).append(" ")
            .append(error.copy().withStyle(ChatFormatting.RED));
    }

    private static MutableComponent prefix() {
        return Component.literal("[Calc Commands]").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    }

    private static MutableComponent header(String operation) {
        return Component.empty().append(prefix()).append(" ")
            .append(Component.translatable("commands.calc.feedback." + operation).withStyle(ChatFormatting.WHITE));
    }

    private static void field(MutableComponent message, String label, String value, ChatFormatting color) {
        message.append("\n  ")
            .append(Component.translatable("commands.calc.feedback." + label).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(value).withStyle(color));
    }

    private static void result(MutableComponent message, CalculatorEngine.Result result, String label) {
        field(message, label, pretty(result.exact()), ChatFormatting.GREEN);
        if (result.approximate() != null) {
            field(message, "approximate_label", pretty(result.approximate()), ChatFormatting.YELLOW);
        }
    }

    private static void solutions(MutableComponent message, IAST solutions) {
        message.append("\n  ").append(Component.translatable("commands.calc.feedback.solutions_label")
            .withStyle(ChatFormatting.GRAY));
        if (solutions.size() == 1) {
            message.append("\n    ").append(Component.translatable("commands.calc.feedback.no_solutions")
                .withStyle(ChatFormatting.YELLOW));
            return;
        }
        for (int i = 1; i < solutions.size(); i++) {
            IExpr solution = solutions.get(i);
            message.append("\n    ");
            if (solutions.size() > 2) {
                message.append(Component.translatable("commands.calc.feedback.solution", i).withStyle(ChatFormatting.GRAY));
            }
            if (solution instanceof IAST assignments && assignments.isList()) {
                if (assignments.size() == 1) {
                    message.append(Component.translatable("commands.calc.feedback.all_reals").withStyle(ChatFormatting.GREEN));
                    continue;
                }
                List<String> values = new ArrayList<>();
                for (int j = 1; j < assignments.size(); j++) {
                    IExpr assignment = assignments.get(j);
                    if (assignment instanceof IAST rule && rule.head().equals(S.Rule) && rule.size() == 3) {
                        values.add(pretty(rule.arg1().toString()) + " = " + pretty(rule.arg2().toString()));
                    } else {
                        values.add(pretty(assignment.toString()));
                    }
                }
                message.append(Component.literal(String.join(",  ", values)).withStyle(ChatFormatting.GREEN));
            } else {
                message.append(Component.literal(pretty(solution.toString())).withStyle(ChatFormatting.GREEN));
            }
        }
    }

    /** A display scanner preserves unary signs, fractions, parentheses and scientific exponents. */
    private static String pretty(String source) {
        StringBuilder out = new StringBuilder();
        var number = NUMBER.matcher(source);
        var name = NAME.matcher(source);
        boolean operand = false;
        for (int i = 0; i < source.length();) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (number.region(i, source.length()).lookingAt()) {
                out.append(number.group());
                i = number.end();
                operand = true;
                continue;
            }
            if (name.region(i, source.length()).lookingAt()) {
                String token = name.group();
                out.append(token.equalsIgnoreCase("pi") ? "π" : FUNCTIONS.getOrDefault(token, token));
                i = name.end();
                operand = true;
                continue;
            }
            if (source.startsWith("->", i)) {
                out.append(" = ");
                i += 2;
                operand = false;
                continue;
            }
            i++;
            switch (c) {
                case '+', '-' -> {
                    String sign = c == '-' ? "−" : "+";
                    out.append(operand ? " " + sign + " " : sign);
                    operand = false;
                }
                case '*' -> { out.append(" × "); operand = false; }
                case '%' -> { out.append(" % "); operand = false; }
                case '=' -> {
                    out.append(" = ");
                    if (i < source.length() && source.charAt(i) == '=') i++;
                    operand = false;
                }
                case ',' -> { out.append(", "); operand = false; }
                case ';' -> { out.append(";  "); operand = false; }
                case '/', '^', '(' -> { out.append(c); operand = false; }
                case ')' -> { out.append(c); operand = true; }
                default -> { out.append(c); operand = false; }
            }
        }
        return out.toString().strip();
    }
}
