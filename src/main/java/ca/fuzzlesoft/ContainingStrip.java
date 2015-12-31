package ca.fuzzlesoft;

/**
 * @author mitch
 * @since 30/12/15
 */
public class ContainingStrip {
    public static final ContainingStrip OBJECT = new ContainingStrip('{', '}');
    public static final ContainingStrip ARRAY = new ContainingStrip('[', ']');

    private final char toStripStart;
    private final char toStripEnd;

    public ContainingStrip(char toStripStart, char toStripEnd) {
        this.toStripStart = toStripStart;
        this.toStripEnd = toStripEnd;
    }

    public String strip(String string) {
        int beginning, i = 0;
        char current;

        //reach and remove first {
        while ((current = string.charAt(i++)) != toStripStart) {
            if (i >= string.length() || !Constants.is(Constants.WHITE, current)) {
                throw new JsonParseException("JSON string did not start with " + toStripStart);
            }
        }
        beginning = i;
        i = string.length() - 1;

        // reach and remove last }
        while ((current = string.charAt(i--)) != toStripEnd) {
            if (i < 0 || !Constants.is(Constants.WHITE, current)) {
                throw new JsonParseException("JSON string did not end with " + toStripEnd);
            }
        }
        return string.substring(beginning, i + 1);
    }
}
