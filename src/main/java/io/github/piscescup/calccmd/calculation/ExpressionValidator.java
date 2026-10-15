package io.github.piscescup.calccmd.calculation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.piscescup.calccmd.calculation.ExpressionLexer.Kind;
import static io.github.piscescup.calccmd.calculation.ExpressionLexer.Token;

/**
 * Checks token order and function frames without evaluating or constructing a recursive tree.
 */
public final class ExpressionValidator {
    public record Completion(int offset, List<String> candidates) {
        public Completion {
            candidates = List.copyOf(candidates);
        }
    }

    private static final class Frame {
        final FunctionDefinition function;
        int commas;

        Frame(FunctionDefinition function) {
            this.function = function;
        }
    }

    private final String source;
    private final Map<String, FunctionDefinition> functions;
    private final ExpressionLexer lexer;
    private final Set<String> symbols;
    private final int contentEnd;
    private final Deque<Frame> frames = new ArrayDeque<>();
    private boolean expectOperand = true;
    private Token previous;
    private FunctionDefinition pendingFunction;
    private int completionOffset = -1;
    private String completionPrefix = "";

    ExpressionValidator(String source, Map<String, FunctionDefinition> functions) {
        this(source, functions, Set.of());
    }

    ExpressionValidator(String source, Map<String, FunctionDefinition> functions, Set<String> symbols) {
        this.source = source;
        this.functions = functions;
        this.symbols = symbols;
        lexer = new ExpressionLexer(source);
        int end = source.length();
        while (end > 0 && Character.isWhitespace(source.charAt(end - 1))) end--;
        contentEnd = end;
    }

    void validate() {
        while (true) {
            Token token = lexer.next();
            if (pendingFunction != null) {
                if (token.kind() != Kind.OPEN) {
                    throw CalcCommandsException.diagnostic(
                        "expected_open", token.start(), token.kind() == Kind.END, pendingFunction.name());
                }
                push(pendingFunction, token);
                pendingFunction = null;
                completionOffset = -1;
            } else if (token.kind() == Kind.END) {
                if (expectOperand) {
                    completionOffset = token.start();
                    completionPrefix = "";
                    throw CalcCommandsException.incomplete("expected_operand", token.start());
                }
                if (!frames.isEmpty()) throw CalcCommandsException.incomplete("expected_close", token.start());
                return;
            } else {
                switch (token.kind()) {
                    case NUMBER -> {
                        requireOperand(token);
                        expectOperand = false;
                        completionOffset = -1;
                    }
                    case NAME -> {
                        requireOperand(token);
                        // Keep the partial name available even when validation fails at end of input.
                        if (token.end() == contentEnd) {
                            completionOffset = token.start();
                            completionPrefix = token.text();
                        }
                        FunctionDefinition function = functions.get(token.text());
                        if (function != null) {
                            pendingFunction = function;
                        } else if ((CalculatorEngine.CONSTANT_NAMES
                            .contains(token.text()) || symbols.contains(token.text()))) {
                            expectOperand = false;
                            if (!token.text().equals("i")) completionOffset = -1;
                        } else {
                            throw CalcCommandsException.at("unknown_name", token.start(), token.text());
                        }
                    }
                    case OPEN -> {
                        requireOperand(token);
                        push(null, token);
                    }
                    case CLOSE -> close(token);
                    case COMMA -> {
                        if (frames.isEmpty() || frames.peek().function == null) {
                            throw CalcCommandsException.at("unexpected_comma", token.start());
                        }
                        if (expectOperand) throw CalcCommandsException.at("expected_operand", token.start());
                        if (++frames.peek().commas >= CalculatorEngine.MAX_ARGUMENTS) {
                            throw CalcCommandsException.at(
                                "too_many_arguments", token.start(), CalculatorEngine.MAX_ARGUMENTS);
                        }
                        expectOperand = true;
                    }
                    case OPERATOR -> {
                        if (expectOperand) {
                            if (!token.text()
                                .equals("+") && !token.text()
                                .equals("-")) {
                                throw CalcCommandsException.at("expected_operand", token.start());
                            }
                        } else {
                            expectOperand = true;
                        }
                        completionOffset = -1;
                    }
                    default -> throw new IllegalStateException("Unexpected token kind");
                }
            }
            previous = token;
        }
    }

    Completion complete() {
        try {
            validate();
        } catch (CalcCommandsException ignored) {
            // Incomplete expressions still carry an operand/name completion position.
        }
        if (completionOffset < 0) return new Completion(source.length(), List.of());
        List<String> candidates = new ArrayList<>();
        for (String constant : CalculatorEngine.CONSTANT_NAMES) {
            if (constant.startsWith(completionPrefix)) candidates.add(constant.equals("i") ? "I" : constant);
        }
        for (String symbol : symbols) {
            if (symbol.startsWith(completionPrefix)) candidates.add(symbol);
        }
        for (FunctionDefinition function : functions.values()) {
            if (function.name().startsWith(completionPrefix)) candidates.add(function.insertion());
        }
        candidates.sort(String::compareTo);
        return new Completion(completionOffset, candidates);
    }

    private void push(FunctionDefinition function, Token token) {
        if (frames.size() >= CalculatorEngine.MAX_DEPTH) {
            throw CalcCommandsException.at("too_deep", token.start(), CalculatorEngine.MAX_DEPTH);
        }
        frames.push(new Frame(function));
        expectOperand = true;
    }

    private void close(Token token) {
        if (frames.isEmpty()) throw CalcCommandsException.at("unexpected_close", token.start());
        Frame frame = frames.peek();
        boolean empty = previous != null && previous.kind() == Kind.OPEN;
        if (expectOperand && !(empty && frame.function != null)) {
            throw CalcCommandsException.at("expected_operand", token.start());
        }
        int count = empty ? 0 : frame.commas + 1;
        if (frame.function != null && (count < frame.function.minimumArguments()
            || count > frame.function.maximumArguments())) {
            throw CalcCommandsException.at(
                "argument_count", token.start(), frame.function.name(), count,
                frame.function.minimumArguments(),
                Math.min(frame.function.maximumArguments(), CalculatorEngine.MAX_ARGUMENTS)
            );
        }
        frames.pop();
        expectOperand = false;
        completionOffset = -1;
    }

    private void requireOperand(Token token) {
        if (!expectOperand) throw CalcCommandsException.at("expected_operator", token.start());
    }
}
