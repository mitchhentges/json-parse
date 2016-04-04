package ca.fuzzlesoft;

/**
 * @author mitch
 * @since 30/12/15
 */
public class Constants {
    public static boolean isWhitespace(char c) {
        return c == ' '
                || c == '\n'
                || c == '\t';
    }

    public static boolean isNumber(char c) {
        return (c >= '0' && c <= '9')
                || c == '.'
                || c == '-'
                || c == 'e'
                || c == 'E'
                || c == '+';
    }
}
