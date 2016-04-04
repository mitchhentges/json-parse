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

        //System.out.println("Waiting for input");
        //scanner.nextLine();
        String toParse = "{\"a\":true}";
        int iterations = 1000000;

        long start = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                mapper.readValue(toParse, new TypeReference<Map<String, Object>>(){});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Jackson done|" + (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            JsonParse parse = new JsonParse();
            parse.map("{\"a\":true}");
        }

        System.out.println("JsonParse done" + (System.currentTimeMillis() - start));

        //System.out.println("Waiting for input, again");
        //scanner.nextLine();
    }
}
