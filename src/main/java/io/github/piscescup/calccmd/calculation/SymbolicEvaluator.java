package io.github.piscescup.calccmd.calculation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.matheclipse.core.basic.Config;
import org.matheclipse.core.eval.EvalEngine;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.expression.ApfloatNum;
import org.matheclipse.core.expression.S;
import org.matheclipse.core.interfaces.IAST;
import org.matheclipse.core.interfaces.IASTAppendable;
import org.matheclipse.core.interfaces.IExpr;
import org.matheclipse.core.interfaces.ISymbol;
import org.matheclipse.core.reflection.system.Integrate;

import static io.github.piscescup.calccmd.calculation.ExpressionLexer.Kind;
import static io.github.piscescup.calccmd.calculation.ExpressionLexer.Token;

/** Iterative AST construction; only explicitly allowed calculator operations reach Symja. */
final class SymbolicEvaluator {
    private static final ScheduledThreadPoolExecutor TIMEOUTS = new ScheduledThreadPoolExecutor(1, task -> {
        Thread thread = new Thread(task, "calc-commands-timeout");
        thread.setDaemon(true);
        return thread;
    });

    static {
        TIMEOUTS.setRemoveOnCancelPolicy(true);
    }

    private static final class TimeBudget implements Runnable {
        private Thread worker = Thread.currentThread();
        private volatile boolean expired;
        @Override public synchronized void run() {
            if (worker != null) {
                expired = true;
                worker.interrupt();
            }
        }
        synchronized void close() {
            worker = null;
        }
    }

    private record Node(IExpr value, int depth, boolean owned) {}

    private static final class OperatorFrame {
        final String operator;
        final FunctionDefinition function;
        final int offset;
        int commas;
        OperatorFrame(String operator, FunctionDefinition function, int offset) {
            this.operator = operator;
            this.function = function;
            this.offset = offset;
        }
    }

    private final EvalEngine engine;
    private long deadline;
    private boolean complexEnabled;

    SymbolicEvaluator() {
        F.initSymja();
        try {
            F.await();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw CalcCommandsException.error("calculation_timeout");
        }
        Config.MAX_AST_SIZE = Math.min(Config.MAX_AST_SIZE, 8192);
        Config.MAX_BIT_LENGTH = Math.min(Config.MAX_BIT_LENGTH, 65536);
        Config.MAX_OUTPUT_SIZE = Math.min(Config.MAX_OUTPUT_SIZE, CalculatorEngine.MAX_OUTPUT_LENGTH);
        Config.MAX_PRECISION_APFLOAT = Math.min(Config.MAX_PRECISION_APFLOAT, CalculatorEngine.MAX_PRECISION);
        engine = new EvalEngine(true);
        engine.setRecursionLimit(256);
        engine.setIterationLimit(10000);
        engine.setQuietMode(true);
    }

    void warmUp() {
        awaitIntegrationRules();
        // Also load the normal expression construction and numeric formatting paths.
        evaluate(() -> build("sin(2*pi)+cos(2*pi)+sqrt(2)", null, Map.of()).value(), false);
    }

    CalculatorEngine.Result evaluate(CalcRequest request, CalculatorEngine.Result answer, boolean complex) {
        complexEnabled = complex;
        if (request.operation() == CalcRequest.Operation.INTEGRATE) awaitIntegrationRules();
        return evaluate(() -> {
            Map<String, IExpr> symbols = symbols(answer);
            for (String name : request.symbols()) symbols.computeIfAbsent(name, F::Dummy);
            return switch (request) {
                case ExpressionRequest expression -> switch (expression.operation()) {
                    case EXPRESSION -> build(expression.expression(), answer, symbols);
                    case EXPAND -> F.Expand(build(expression.expression(), answer, symbols));
                    case FACTOR -> F.Factor(build(expression.expression(), answer, symbols));
                    case SIMPLIFY -> F.Simplify(build(expression.expression(), answer, symbols));
                    default -> throw CalcCommandsException.error("evaluation_failed");
                };
                case DiffRequest diff -> {
                    IASTAppendable derivative = F.ast(S.D, diff.variables().size() + 1);
                    derivative.append(build(diff.expression(), answer, symbols));
                    for (String variable : diff.variables()) derivative.append(symbols.get(variable));
                    if (diff.assignments().isEmpty()) yield derivative;
                    IExpr differentiated = calculate(derivative);
                    IASTAppendable rules = F.ast(S.List, diff.assignments().size());
                    for (DiffRequest.Assignment assignment : diff.assignments()) {
                        IExpr point = finitePoint(assignment.value(), answer, complex);
                        rules.append(F.Rule(symbols.get(assignment.variable()), point));
                    }
                    IASTAppendable substitute = F.ast(S.ReplaceAll, 2);
                    substitute.append(differentiated);
                    substitute.append(rules);
                    yield substitute;
                }
                case IntegrateRequest integral -> {
                    if (!integral.definite()) {
                        yield F.Integrate(build(integral.expression(), answer, symbols), symbols.get(integral.variable()));
                    }
                    IExpr from = calculate(build(integral.lowerBound(), answer, Map.of()));
                    IExpr to = calculate(build(integral.upperBound(), answer, Map.of()));
                    if (!calculate(F.N(from)).isReal() || !calculate(F.N(to)).isReal()) {
                        throw CalcCommandsException.error("invalid_bounds");
                    }
                    yield F.Integrate(build(integral.expression(), answer, symbols),
                        F.List(symbols.get(integral.variable()), from, to));
                }
                case LimitRequest limit -> {
                    IExpr point = SymbolicRequestParser.infinity(limit.pointSource())
                        ? (limit.pointSource().startsWith("-") ? F.CNInfinity : F.CInfinity)
                        : finitePoint(limit.point(), answer, false);
                    IExpr direction = switch (limit.direction()) {
                        case BOTH -> S.Reals;
                        case LEFT -> F.C1;
                        case RIGHT -> F.CN1;
                    };
                    IASTAppendable expression = F.ast(S.Limit, 3);
                    expression.append(build(limit.expression(), answer, symbols));
                    expression.append(F.Rule(symbols.get(limit.variable()), point));
                    expression.append(F.Rule(S.Direction, direction));
                    yield expression;
                }
                case SolveRequest solveRequest -> {
                    IASTAppendable equations = F.ast(S.List, solveRequest.equations().size());
                    for (var equation : solveRequest.equations()) {
                        equations.append(F.Equal(build(equation.left(), answer, symbols),
                            build(equation.right(), answer, symbols)));
                    }
                    IASTAppendable variables = F.ast(S.List, solveRequest.variables().size());
                    for (String variable : solveRequest.variables()) variables.append(symbols.get(variable));
                    IASTAppendable solve = F.ast(S.Solve, 3);
                    solve.append(equations);
                    solve.append(variables);
                    solve.append(complex ? S.Complexes : S.Reals);
                    yield solve;
                }
            };
        }, request.operation() != CalcRequest.Operation.EXPRESSION
            || answer != null && !symbols(answer).isEmpty(), request.operation() == CalcRequest.Operation.LIMIT, complex);
    }

    private IExpr finitePoint(CalcRequest.Part point, CalculatorEngine.Result answer, boolean complex) {
        IExpr value = calculate(build(point, answer, Map.of()));
        IExpr decimal = calculate(F.N(value));
        if (!(decimal.isReal() || complex && decimal.isNumber())) {
            throw CalcCommandsException.at("invalid_point", point.offset());
        }
        return value;
    }

    /** The library initializes Rubi on another thread; that one-time wait is not user calculation. */
    private void awaitIntegrationRules() {
        TimeBudget budget = new TimeBudget();
        var timeout = TIMEOUTS.schedule(budget, 30, TimeUnit.SECONDS);
        try {
            if (!(S.Integrate.getEvaluator() instanceof Integrate integration)) throw CalcCommandsException.error("symbolic_failed");
            integration.await();
            if (budget.expired) throw CalcCommandsException.error("calculation_timeout");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw CalcCommandsException.error("calculation_timeout");
        } finally {
            budget.close();
            timeout.cancel(false);
            if (budget.expired) Thread.interrupted();
        }
    }

    private CalculatorEngine.Result evaluate(Supplier<IExpr> input, boolean symbolic) {
        return evaluate(input, symbolic, false, false);
    }

    private CalculatorEngine.Result evaluate(Supplier<IExpr> input, boolean symbolic, boolean allowInfinity, boolean complex) {
        EvalEngine.set(engine);
        engine.init();
        deadline = System.currentTimeMillis() + 2000;
        engine.setTimeConstrainedMillis(-1);
        engine.setTimeConstrainedMillis(deadline);
        TimeBudget budget = new TimeBudget();
        var timeout = TIMEOUTS.schedule(budget, 2000, TimeUnit.MILLISECONDS);
        try {
            IExpr value = calculate(input.get());
            checkTree(value);
            if (allowInfinity && (value.isInfinity() || value.isNegativeInfinity())) {
                return new CalculatorEngine.Result(value, text(value), null);
            }
            checkResult(value, complex);
            if (value.isDirectedInfinity()) throw CalcCommandsException.error("division_by_zero");
            if (symbolic && unresolved(value)) throw CalcCommandsException.error("symbolic_failed");
            if (value.isList()) {
                if (!symbolic) throw CalcCommandsException.error("undefined_result");
                return new CalculatorEngine.Result(value, text(value), null);
            }
            IExpr decimal = calculate(F.N(value, F.ZZ(30)));
            if (!(decimal.isReal() || complex && decimal.isNumber())) {
                if (!symbolic || value.isNumber()) throw CalcCommandsException.error("undefined_result");
                return new CalculatorEngine.Result(value, text(value), null);
            }
            String exact = text(value);
            String approximate = value.isInteger() || value.isInexactNumber() ? null : text(decimal);
            return new CalculatorEngine.Result(value, exact, approximate);
        } catch (CalcCommandsException error) {
            throw error;
        } catch (StackOverflowError error) {
            // Library internals may still encounter a recursion path beyond their configured limit.
            throw CalcCommandsException.error("too_complex");
        } catch (RuntimeException error) {
            if (budget.expired || System.currentTimeMillis() >= deadline) throw CalcCommandsException.error("calculation_timeout");
            throw CalcCommandsException.error("too_complex");
        } finally {
            // Synchronize with the watchdog so a late interrupt cannot reach the next calculation.
            budget.close();
            timeout.cancel(false);
            if (budget.expired) Thread.interrupted();
            engine.setTimeConstrainedMillis(-1);
            EvalEngine.remove();
        }
    }

    private IExpr build(CalcRequest.Part input, CalculatorEngine.Result answer, Map<String, IExpr> symbols) {
        try {
            return build(input.text(), answer, symbols).value();
        } catch (CalcCommandsException error) {
            throw CalcCommandsException.shifted(error, input.offset());
        }
    }

    /** Reuse local symbols from ans so differentiating a previous expression uses the same identities. */
    private static Map<String, IExpr> symbols(CalculatorEngine.Result answer) {
        Map<String, IExpr> symbols = new HashMap<>();
        if (answer == null) return symbols;
        Deque<IExpr> pending = new ArrayDeque<>();
        pending.push(answer.value());
        while (!pending.isEmpty()) {
            IExpr value = pending.pop();
            if (value instanceof ISymbol symbol && !symbol.isBuiltInSymbol()) {
                symbols.put(symbol.getSymbolName(), symbol);
            } else if (value instanceof IAST ast) {
                for (int i = 1; i < ast.size(); i++) pending.push(ast.get(i));
            }
        }
        return symbols;
    }

    private static boolean unresolved(IExpr expression) {
        Deque<IExpr> pending = new ArrayDeque<>();
        pending.push(expression);
        while (!pending.isEmpty()) {
            if (pending.pop() instanceof IAST ast) {
                if (ast.head().equals(S.Solve) || ast.head().equals(S.Integrate)
                        || ast.head().equals(S.D) || ast.head().equals(S.Limit) || ast.head().equals(S.Simplify)) return true;
                for (int i = 1; i < ast.size(); i++) pending.push(ast.get(i));
            }
        }
        return false;
    }

    private static void checkResult(IExpr expression, boolean complex) {
        Deque<IExpr> pending = new ArrayDeque<>();
        pending.push(expression);
        while (!pending.isEmpty()) {
            IExpr value = pending.pop();
            if (value.isIndeterminate()) throw CalcCommandsException.error("undefined_result");
            if (value.isDirectedInfinity()) throw CalcCommandsException.error("undefined_result");
            if (!complex && value.isNumber() && !value.isReal()) {
                throw CalcCommandsException.error("complex_disabled");
            }
            if (value instanceof IAST ast) {
                for (int i = 1; i < ast.size(); i++) pending.push(ast.get(i));
            }
        }
    }

    private Node build(String source, CalculatorEngine.Result answer, Map<String, IExpr> symbols) {
        ExpressionLexer lexer = new ExpressionLexer(source);
        Deque<Node> values = new ArrayDeque<>();
        Deque<OperatorFrame> operators = new ArrayDeque<>();
        FunctionDefinition pendingFunction = null;
        boolean expectOperand = true;
        Token previous = null;
        for (Token token = lexer.next(); token.kind() != Kind.END; token = lexer.next()) {
            checkTime();
            switch (token.kind()) {
                case NUMBER -> {
                    values.push(new Node(number(token.text()), 0, false));
                    expectOperand = false;
                }
                case NAME -> {
                    IExpr constant = switch (token.text()) {
                        case "pi" -> S.Pi;
                        case "e" -> S.E;
                        case "i" -> F.CI;
                        case "ans" -> answer == null ? F.C0 : answer.value();
                        default -> symbols.get(token.text());
                    };
                    if (constant == null) {
                        pendingFunction = FunctionDefinition.FUNCTIONS.get(token.text());
                        if (pendingFunction == null) {
                            throw CalcCommandsException.at("unknown_name", token.start(), token.text());
                        }
                    } else {
                        values.push(new Node(constant, checkTree(constant), false));
                        expectOperand = false;
                    }
                }
                case OPEN -> {
                    operators.push(new OperatorFrame("(", pendingFunction, token.start()));
                    pendingFunction = null;
                    expectOperand = true;
                }
                case COMMA -> {
                    reduceToOpen(operators, values);
                    operators.peek().commas++;
                    expectOperand = true;
                }
                case CLOSE -> {
                    reduceToOpen(operators, values);
                    OperatorFrame frame = operators.pop();
                    if (frame.function != null) {
                        int count = previous.kind() == Kind.OPEN ? 0 : frame.commas + 1;
                        Node[] arguments = new Node[count];
                        for (int i = count - 1; i >= 0; i--) arguments[i] = values.pop();
                        values.push(function(frame.function, arguments));
                    }
                    expectOperand = false;
                }
                case OPERATOR -> {
                    String operator = expectOperand ? "u" + token.text() : token.text();
                    if (!expectOperand) {
                        while (!operators.isEmpty() && !operators.peek().operator.equals("(")
                                && (precedence(operators.peek().operator) > precedence(operator)
                                || precedence(operators.peek().operator) == precedence(operator) && !operator.equals("^"))) {
                            reduce(operators.pop(), values);
                        }
                    }
                    operators.push(new OperatorFrame(operator, null, token.start()));
                    expectOperand = true;
                }
                default -> throw new IllegalStateException("Unexpected token");
            }
            previous = token;
        }
        while (!operators.isEmpty()) reduce(operators.pop(), values);
        if (values.size() != 1) throw CalcCommandsException.error("evaluation_failed");
        return values.pop();
    }

    private void reduceToOpen(Deque<OperatorFrame> operators, Deque<Node> values) {
        while (!operators.peek().operator.equals("(")) reduce(operators.pop(), values);
    }

    private void reduce(OperatorFrame frame, Deque<Node> values) {
        Node right = values.pop();
        if (frame.operator.equals("u+")) {
            values.push(right);
            return;
        }
        if (frame.operator.equals("u-")) {
            values.push(node(F.Times(F.CN1, right.value()), right.depth() + 1));
            return;
        }
        Node left = values.pop();
        int depth = Math.max(left.depth(), right.depth()) + 1;
        Node result = switch (frame.operator) {
            case "+" -> associative(S.Plus, left, right);
            case "-" -> associative(S.Plus, left, node(F.Times(F.CN1, right.value()), right.depth() + 1));
            case "*" -> associative(S.Times, left, right);
            case "/", "%" -> {
                IExpr divisor = calculate(right.value());
                if (divisor.isZero()) throw CalcCommandsException.at("division_by_zero", frame.offset);
                if (frame.operator.equals("/")) yield node(F.Divide(left.value(), divisor), depth + 1);
                // Remainder truncates toward zero, preserving the existing negative-operand behavior.
                IExpr dividend = calculate(left.value());
                yield node(F.Subtract(dividend, F.Times(F.IntegerPart(F.Divide(dividend, divisor)), divisor)),
                    Math.max(checkTree(dividend), checkTree(divisor)) + 4);
            }
            case "^" -> node(F.Power(left.value(), right.value()), depth);
            default -> throw new IllegalStateException("Unknown operator");
        };
        values.push(result);
    }

    private Node associative(IExpr head, Node left, Node right) {
        // Long addition/multiplication chains stay flat; never mutate an expression taken from ans.
        if (left.owned() && left.value() instanceof IASTAppendable ast && ast.head().equals(head)) {
            ast.append(right.value());
            return node(ast, Math.max(left.depth(), right.depth() + 1));
        }
        IASTAppendable ast = F.ast(head, 2);
        ast.append(left.value());
        ast.append(right.value());
        return node(ast, Math.max(left.depth(), right.depth()) + 1);
    }

    private Node function(FunctionDefinition function, Node[] arguments) {
        IExpr[] args = new IExpr[arguments.length];
        int depth = 1;
        for (int i = 0; i < args.length; i++) {
            args[i] = arguments[i].value();
            depth = Math.max(depth, arguments[i].depth() + 1);
        }
        IExpr value;
        if (function == FunctionDefinition.SIN) {
            value = F.Sin(args[0]);
        } else if (function == FunctionDefinition.COS) {
            value = F.Cos(args[0]);
        } else if (function == FunctionDefinition.TAN) {
            value = F.Tan(args[0]);
        } else if (function == FunctionDefinition.ASIN) {
            value = F.ArcSin(args[0]);
        } else if (function == FunctionDefinition.ACOS) {
            value = F.ArcCos(args[0]);
        } else if (function == FunctionDefinition.ATAN) {
            value = F.ArcTan(args[0]);
        } else if (function == FunctionDefinition.ATAN2) {
            value = F.ArcTan(args[1], args[0]); // atan2(y, x)
        } else if (function == FunctionDefinition.SINH) {
            value = F.Sinh(args[0]);
        } else if (function == FunctionDefinition.COSH) {
            value = F.Cosh(args[0]);
        } else if (function == FunctionDefinition.TANH) {
            value = F.Tanh(args[0]);
        } else if (function == FunctionDefinition.SQRT) {
            value = F.Sqrt(args[0]);
        } else if (function == FunctionDefinition.ABS) {
            value = F.Abs(args[0]);
        } else if (function == FunctionDefinition.CEIL) {
            value = F.Ceiling(args[0]);
        } else if (function == FunctionDefinition.FLOOR) {
            value = F.Floor(args[0]);
        } else if (function == FunctionDefinition.ROUND) {
            IExpr digits = args.length == 1 ? F.C0 : calculate(args[1]);
            int places = digits.toIntDefault(Integer.MIN_VALUE);
            if (!digits.isInteger() || places < -CalculatorEngine.MAX_PRECISION
                    || places > CalculatorEngine.MAX_PRECISION) {
                throw CalcCommandsException.at("invalid_round_digits", 0,
                    CalculatorEngine.MAX_PRECISION, CalculatorEngine.MAX_PRECISION);
            }
            IExpr scale = F.Power(F.C10, digits);
            // Preserve the original tie rule: halfway values round toward positive infinity.
            value = F.Divide(F.Floor(F.Plus(F.Times(args[0], scale), F.C1D2)), scale);
        } else if (function == FunctionDefinition.CEILDIV) {
            IExpr dividend = integerArgument(function, args[0]);
            IExpr divisor = integerArgument(function, args[1]);
            if (divisor.isZero()) throw CalcCommandsException.error("division_by_zero");
            value = F.Ceiling(F.Divide(dividend, divisor));
        } else if (function == FunctionDefinition.GCD || function == FunctionDefinition.LCM) {
            IASTAppendable ast = F.ast(function == FunctionDefinition.GCD ? S.GCD : S.LCM, args.length);
            for (IExpr arg : args) ast.append(integerArgument(function, arg));
            value = ast;
        } else if (function == FunctionDefinition.HYPOT) {
            IASTAppendable squares = F.ast(S.Plus, args.length);
            for (IExpr arg : args) squares.append(F.Power(arg, F.C2));
            value = F.Sqrt(squares);
        } else if (function == FunctionDefinition.RAD) {
            value = F.Divide(F.Times(args[0], S.Pi), F.ZZ(180));
        } else if (function == FunctionDefinition.DEG) {
            value = F.Divide(F.Times(args[0], F.ZZ(180)), S.Pi);
        } else if (function == FunctionDefinition.LN) {
            value = F.Log(args[0]);
        } else if (function == FunctionDefinition.LG) {
            value = F.Log(F.C10, args[0]);
        } else if (function == FunctionDefinition.LOG2) {
            value = F.Log(F.C2, args[0]);
        } else if (function == FunctionDefinition.LOG) {
            IExpr base = calculate(args[0]);
            IExpr argument = calculate(args[1]);
            if (base.isOne() || base.isZero() || argument.isZero()
                    || !complexEnabled && (base.isNegativeResult() || argument.isNegativeResult())) {
                throw CalcCommandsException.at("function_domain", 0, function.name());
            }
            value = F.Log(base, argument); // Preserve log(base, value).
        } else if (function == FunctionDefinition.MIN || function == FunctionDefinition.MAX) {
            IASTAppendable ast = F.ast(function == FunctionDefinition.MIN ? S.Min : S.Max, args.length);
            for (IExpr arg : args) ast.append(arg);
            value = ast;
        } else if (function == FunctionDefinition.SUM) {
            value = F.Plus(args);
        } else if (function == FunctionDefinition.AVG) {
            value = F.Divide(F.Plus(args), F.ZZ(args.length));
        } else if (function == FunctionDefinition.RANDOM) {
            value = F.num(ThreadLocalRandom.current().nextDouble());
        } else if (function == FunctionDefinition.N) {
            IExpr precision = args.length == 1 ? F.ZZ(30) : calculate(args[1]);
            int digits = precision.toIntDefault(-1);
            if (!precision.isInteger() || digits < 1 || digits > CalculatorEngine.MAX_PRECISION) {
                throw CalcCommandsException.at("invalid_precision", 0, CalculatorEngine.MAX_PRECISION);
            }
            value = F.N(args[0], precision);
        } else {
            throw CalcCommandsException.error("evaluation_failed");
        }
        return node(value, Math.max(depth + 2, checkTree(value)));
    }

    private IExpr integerArgument(FunctionDefinition function, IExpr argument) {
        IExpr value = calculate(argument);
        if (!value.isInteger()) {
            throw CalcCommandsException.at("integer_arguments", 0, function.name());
        }
        return value;
    }

    private static IExpr number(String literal) {
        BigDecimal number = new BigDecimal(literal);
        BigInteger unscaled = number.unscaledValue();
        int scale = number.scale();
        return scale <= 0 ? F.ZZ(unscaled.multiply(BigInteger.TEN.pow(-scale)))
            : F.QQ(unscaled, BigInteger.TEN.pow(scale));
    }

    private IExpr calculate(IExpr expression) {
        checkTime();
        // Count the whole tree before entering recursive library code, including repeated subtrees.
        checkTree(expression);
        IExpr result = engine.evaluate(expression);
        checkTime();
        return result;
    }

    private static Node node(IExpr value, int depth) {
        if (depth > CalculatorEngine.MAX_DEPTH) throw CalcCommandsException.error("too_complex");
        return new Node(value, depth, true);
    }

    private static int checkTree(IExpr expression) {
        record Entry(IExpr value, int depth) {}
        Deque<Entry> stack = new ArrayDeque<>();
        stack.push(new Entry(expression, 0));
        int count = 0;
        int maxDepth = 0;
        while (!stack.isEmpty()) {
            Entry entry = stack.pop();
            if (++count > 8192 || entry.depth() > CalculatorEngine.MAX_DEPTH) throw CalcCommandsException.error("too_complex");
            maxDepth = Math.max(maxDepth, entry.depth());
            if (entry.value() instanceof IAST ast) {
                for (int i = 1; i < ast.size(); i++) stack.push(new Entry(ast.get(i), entry.depth() + 1));
            }
        }
        return maxDepth;
    }

    private static String text(IExpr value) {
        String text;
        if (value instanceof ApfloatNum number) {
            var decimal = number.apfloatValue();
            long scale = decimal.scale();
            // Keep normal-sized decimals readable without expanding enormous powers of ten.
            text = decimal.toString(scale >= -5 && scale <= 12).replace('e', 'E');
        } else {
            text = value.toString();
        }
        if (text.length() > CalculatorEngine.MAX_OUTPUT_LENGTH) throw CalcCommandsException.error("too_complex");
        return text;
    }

    private void checkTime() {
        if (Thread.currentThread().isInterrupted() || System.currentTimeMillis() >= deadline) {
            throw CalcCommandsException.error("calculation_timeout");
        }
    }

    private static int precedence(String operator) {
        return switch (operator) {
            case "+", "-" -> 1;
            case "*", "/", "%" -> 2;
            case "u+", "u-" -> 3;
            case "^" -> 4;
            default -> 0;
        };
    }
}
