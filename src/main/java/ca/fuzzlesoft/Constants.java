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

    public static boolean isLetter(char c) {
        return c >= 'a' && c <= 'z';
    }
}
