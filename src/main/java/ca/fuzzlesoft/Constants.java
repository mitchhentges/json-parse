package ca.fuzzlesoft;

/**
 * @author mitch
 * @since 30/12/15
 */
public class Constants {
    public static final char[] WHITE = new char[] {' ', '\n', '\t'};
    public static final char[] NUMBERS =
            new char[] {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '+', 'e', 'E', '.'};

    public static boolean is(char[] pool, char c) {
        for (char poolChar : pool) {
            if (poolChar == c) return true;
        }
        return false;
    }
}
