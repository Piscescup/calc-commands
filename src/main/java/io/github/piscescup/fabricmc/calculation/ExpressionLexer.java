package io.github.piscescup.fabricmc.calculation;

import java.util.Locale;
import java.math.BigDecimal;

/**
 * An iterative, bounded scanner shared by validation and symbolic expression construction.
 */
final class ExpressionLexer {
    enum Kind {
        NUMBER,
        NAME,
        OPERATOR,
        OPEN,
        CLOSE,
        COMMA,
        END
    }

    record Token(
        Kind kind,
        String text,
        int start,
        int end
    ) {
    }

    private final String source;
    private int cursor;
    private int count;

    ExpressionLexer(String source) {
        if (source.length() > CalculatorEngine.MAX_LENGTH) {
            throw CalcCommandsException.at("too_long", CalculatorEngine.MAX_LENGTH, CalculatorEngine.MAX_LENGTH);
        }
        this.source = source;
    }

    Token next() {
        while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) cursor++;
        int start = cursor;
        if (cursor == source.length()) return new Token(Kind.END, "", cursor, cursor);
        if (++count > CalculatorEngine.MAX_TOKENS) {
            throw CalcCommandsException.at("too_many_tokens", start, CalculatorEngine.MAX_TOKENS);
        }
        char c = source.charAt(cursor++);
        if (isDigit(c) || c == '.') {
            boolean digits = isDigit(c);
            while (cursor < source.length() && isDigit(source.charAt(cursor))) {
                cursor++;
                digits = true;
            }
            if (c != '.' && cursor < source.length() && source.charAt(cursor) == '.') {
                cursor++;
                while (cursor < source.length() && isDigit(source.charAt(cursor))) {
                    cursor++;
                    digits = true;
                }
            }
            if (!digits) throw CalcCommandsException.at("invalid_number", start, ".");
            if (cursor < source.length() && (source.charAt(cursor) == 'e' || source.charAt(cursor) == 'E')) {
                cursor++;
                if (cursor < source.length() && (source.charAt(cursor) == '+' || source.charAt(cursor) == '-')) {
                    cursor++;
                }
                int exponentStart = cursor;
                while (cursor < source.length() && isDigit(source.charAt(cursor))) cursor++;
                if (cursor == exponentStart) {
                    throw CalcCommandsException.diagnostic("expected_exponent", cursor, cursor == source.length());
                }
            }
            String text = source.substring(start, cursor);
            try {
                BigDecimal number = new BigDecimal(text);
                if (Math.abs((long) number.scale()) > CalculatorEngine.MAX_NUMBER_EXPONENT) {
                    throw CalcCommandsException.at("invalid_number", start, text);
                }
            } catch (NumberFormatException error) {
                throw CalcCommandsException.at("invalid_number", start, text);
            }
            return new Token(Kind.NUMBER, text, start, cursor);
        }
        if (isNameStart(c)) {
            while (cursor < source.length() && (isNameStart(source.charAt(cursor)) || isDigit(source.charAt(cursor))))
                cursor++;
            return new Token(
                Kind.NAME, source.substring(start, cursor)
                .toLowerCase(Locale.ROOT), start, cursor
            );
        }
        Kind kind = switch (c) {
            case '+', '-', '*', '/', '%', '^' -> Kind.OPERATOR;
            case '(' -> Kind.OPEN;
            case ')' -> Kind.CLOSE;
            case ',' -> Kind.COMMA;
            default -> throw CalcCommandsException.at("unexpected_character", start, String.valueOf(c));
        };
        return new Token(kind, String.valueOf(c), start, cursor);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isNameStart(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }
}
