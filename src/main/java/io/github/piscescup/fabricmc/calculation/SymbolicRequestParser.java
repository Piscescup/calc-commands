package io.github.piscescup.fabricmc.calculation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import io.github.piscescup.fabricmc.calculation.CalcRequest.Operation;
import io.github.piscescup.fabricmc.calculation.CalcRequest.Part;
import io.github.piscescup.fabricmc.calculation.SolveRequest.Equation;

/** Splits and validates command input without evaluating it or initializing Symja. */
final class SymbolicRequestParser {
    private SymbolicRequestParser() {}

    static CalcRequest parse(Operation operation, String source, CalculatorEngine engine) {
        if (source.length() > CalculatorEngine.MAX_LENGTH) {
            throw CalcCommandsException.at("too_long", CalculatorEngine.MAX_LENGTH, CalculatorEngine.MAX_LENGTH);
        }
        if (operation == Operation.EXPRESSION) {
            // Keep numeric syntax strict and preserve its original error offsets and diagnostics.
            engine.validate(source);
            return new ExpressionRequest(operation, source, new Part(source, 0), Set.of());
        }
        List<Part> words = words(source);
        int suffix = switch (operation) {
            case SOLVE, DIFF -> 1;
            case INTEGRATE -> 3;
            default -> 0;
        };
        if (words.size() <= suffix) {
            throw CalcCommandsException.incomplete("symbolic_arguments", source.length(), operation.syntax());
        }
        int expressionEnd = suffix == 0 ? source.length() : words.get(words.size() - suffix)
            .offset();
        Part expression = part(source, 0, expressionEnd, true);
        List<String> variables = new ArrayList<>();
        if (suffix > 0) {
            Part names = words.get(words.size() - suffix);
            String[] split = names.text()
                .split(",", -1);
            if (split.length > CalculatorEngine.MAX_SYMBOLIC_ITEMS) throw tooMany(names.offset());
            int offset = names.offset();
            for (String item : split) {
                String name = item.toLowerCase(Locale.ROOT);
                if (!variableName(name) || engine.reservedName(name)) {
                    throw CalcCommandsException.diagnostic("invalid_variable", offset, item.isEmpty(), item);
                }
                if (operation == Operation.SOLVE && variables.contains(name)) {
                    throw CalcCommandsException.at("duplicate_variable", offset, name);
                }
                variables.add(name);
                offset += item.length() + 1;
            }
            if (operation == Operation.INTEGRATE && variables.size() != 1) {
                throw CalcCommandsException.at("single_variable", names.offset());
            }
        }

        List<Equation> equations = new ArrayList<>();
        if (operation == Operation.SOLVE) {
            String text = expression.text();
            int start = 0;
            for (int i = 0; i <= text.length(); i++) {
                if (i < text.length() && text.charAt(i) != ';') continue;
                if (equations.size() >= CalculatorEngine.MAX_SYMBOLIC_ITEMS) throw tooMany(expression.offset() + i);
                String equation = text.substring(start, i);
                int equal = equation.indexOf('=');
                int right = equal + 1;
                if (right < equation.length() && equation.charAt(right) == '=') right++;
                if (equal < 0 || equation.indexOf('=', right) >= 0) {
                    throw CalcCommandsException.at("expected_equation", expression.offset() + start);
                }
                equations.add(new Equation(
                    part(source, expression.offset() + start, expression.offset() + start + equal, false),
                    part(source, expression.offset() + start + right, expression.offset() + i, false)
                ));
                start = i + 1;
            }
        }
        List<Part> bounds = operation == Operation.INTEGRATE
            ? List.of(words.get(words.size() - 2), words.getLast()) : List.of();
        Set<String> symbols = new LinkedHashSet<>(variables);
        List<Part> expressions = new ArrayList<>();
        if (operation == Operation.SOLVE) {
            for (Equation equation : equations) {
                expressions.add(equation.left());
                expressions.add(equation.right());
            }
        } else {
            expressions.add(expression);
        }
        int tokens = 0;
        for (Part input : expressions) {
            try {
                ExpressionLexer lexer = new ExpressionLexer(input.text());
                for (var token = lexer.next(); token.kind() != ExpressionLexer.Kind.END; token = lexer.next()) {
                    if (++tokens > CalculatorEngine.MAX_TOKENS) {
                        throw CalcCommandsException.at("too_many_tokens", token.start(), CalculatorEngine.MAX_TOKENS);
                    }
                    if (token.kind() == ExpressionLexer.Kind.NAME && !engine.reservedName(token.text())) {
                        if (!variableName(token.text())) {
                            throw CalcCommandsException.at("invalid_variable", token.start(), token.text());
                        }
                        symbols.add(token.text());
                        if (symbols.size() > CalculatorEngine.MAX_SYMBOLIC_ITEMS) throw tooMany(token.start());
                    }
                }
                engine.validate(input.text(), symbols);
            } catch (CalcCommandsException error) {
                throw CalcCommandsException.shifted(error, input.offset());
            }
        }
        for (Part bound : bounds) {
            try {
                ExpressionLexer lexer = new ExpressionLexer(bound.text());
                for (var token = lexer.next(); token.kind() != ExpressionLexer.Kind.END; token = lexer.next()) {
                    if (++tokens > CalculatorEngine.MAX_TOKENS) {
                        throw CalcCommandsException.at("too_many_tokens", token.start(), CalculatorEngine.MAX_TOKENS);
                    }
                }
                // Bounds use the numeric calculator grammar; free symbols are not accepted here.
                engine.validate(bound.text());
            } catch (CalcCommandsException error) {
                throw CalcCommandsException.shifted(error, bound.offset());
            }
        }
        return switch (operation) {
            case EXPRESSION, EXPAND, FACTOR -> new ExpressionRequest(operation, source, expression, symbols);
            case SOLVE -> new SolveRequest(source, expression, symbols, variables, equations);
            case DIFF -> new DiffRequest(source, expression, symbols, variables);
            case INTEGRATE -> new IntegrateRequest(source, expression, symbols,
                variables.getFirst(), bounds.getFirst(), bounds.getLast());
        };
    }

    private static boolean variableName(String name) {
        return name.matches("[a-z][a-z0-9_]{0,31}");
    }

    private static CalcCommandsException tooMany(int offset) {
        return CalcCommandsException.at("symbolic_limit", offset, CalculatorEngine.MAX_SYMBOLIC_ITEMS);
    }

    private static Part part(String source, int start, int end, boolean unquote) {
        while (start < end && Character.isWhitespace(source.charAt(start))) start++;
        while (end > start && Character.isWhitespace(source.charAt(end - 1))) end--;
        if (unquote && start < end && source.charAt(start) == '"') {
            if (end - start < 2 || source.charAt(end - 1) != '"') {
                throw CalcCommandsException.incomplete("expected_quote", end);
            }
            start++;
            end--;
        }
        return new Part(source.substring(start, end), start);
    }

    /**
     * Split only at top-level whitespace; parentheses keep bounds such as (pi / 2) together.
     */
    private static List<Part> words(String source) {
        List<Part> parts = new ArrayList<>();
        int start = -1;
        int depth = 0;
        boolean quoted = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c) && depth == 0 && !quoted) {
                if (start >= 0) parts.add(new Part(source.substring(start, i), start));
                start = -1;
                continue;
            }
            if (start < 0) start = i;
            if (c == '"') quoted = !quoted;
            if (!quoted) {
                if (c == '(') depth++;
                if (c == ')') depth = Math.max(0, depth - 1);
            }
        }
        if (start >= 0) parts.add(new Part(source.substring(start), start));
        if (quoted) throw CalcCommandsException.incomplete("expected_quote", source.length());
        if (depth > 0) throw CalcCommandsException.incomplete("expected_close", source.length());
        return parts;
    }
}
