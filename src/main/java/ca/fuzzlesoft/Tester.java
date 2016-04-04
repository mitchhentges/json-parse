package ca.fuzzlesoft;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

/**
 * @author mitch
 * @since 4/4/16
 */
public class Tester {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Waiting for input");
        scanner.nextLine();
        String toParse = "{\"a\":true, \"b\":\"yes\", \"c\":\"totally\"}";
        int iterations = 5000000;
        ObjectMapper mapper = new ObjectMapper();
        JsonParse parse = new JsonParse();

        long start = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {

            try {
                mapper.readValue(toParse, new TypeReference<Map<String, Object>>(){});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Jackson done|" + (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            parse.map(toParse);
        }

        System.out.println("JsonParse done|" + (System.currentTimeMillis() - start));

        //System.out.println("Waiting for input, again");
        //scanner.nextLine();
    }
}
