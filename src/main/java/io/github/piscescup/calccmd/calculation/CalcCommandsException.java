package io.github.piscescup.calccmd.calculation;

/**
 * A localized calculator diagnostic; offsets are UTF-16 positions in command input.
 */
public final class CalcCommandsException
    extends IllegalArgumentException
{
    private final String key;
    private final int offset;
    private final boolean incomplete;
    private final Object[] arguments;

    private CalcCommandsException(String key, int offset, boolean incomplete, Object... arguments) {
        super(key);
        this.key = key;
        this.offset = offset;
        this.incomplete = incomplete;
        this.arguments = arguments.clone();
    }

    /**
     * Evaluation errors without a specific input position.
     */
    public static CalcCommandsException error(String key, Object... arguments) {
        return at(key, 0, arguments);
    }

    /**
     * Invalid input at a specific position.
     */
    public static CalcCommandsException at(String key, int offset, Object... arguments) {
        return diagnostic(key, offset, false, arguments);
    }

    /**
     * Input which needs more characters, for example sin(.
     */
    static CalcCommandsException incomplete(String key, int offset, Object... arguments) {
        return diagnostic(key, offset, true, arguments);
    }

    /**
     * Use when completeness is determined while parsing.
     */
    static CalcCommandsException diagnostic(String key, int offset, boolean incomplete, Object... arguments) {
        return new CalcCommandsException(key, offset, incomplete, arguments);
    }

    /**
     * Translate an expression-local error position into the original command input.
     */
    static CalcCommandsException shifted(CalcCommandsException error, int offset) {
        return diagnostic(error.key, offset + error.offset, error.incomplete, error.arguments);
    }

    public String key() {
        return key;
    }

    public int offset() {
        return offset;
    }

    public Object[] arguments() {
        return arguments.clone();
    }
}
