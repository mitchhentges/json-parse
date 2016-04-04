package ca.fuzzlesoft;

/**
 * @author mitch
 * @since 4/4/16
 */
public class Tester {
    public static void main(String[] args) {
        String toParse = "{\"a\":true, \"b\":\"yes\", \"c\":\"totally\"}";
        int iterations = 100000;
        JsonParse parse = new JsonParse();

        long start = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            parse.map(toParse);
        }

        System.out.println("JsonParse done|" + (System.currentTimeMillis() - start));
    }
}
