package ca.fuzzlesoft;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

/**
 * Parses JSON, converting it into {@link List}s and {@link Map}s. Is thread safe.
 *
 * @author mitch
 * @since 30/12/15
 */
@SuppressWarnings("unchecked") // Because of reusing `currentContainer` for both maps and lists
public class JsonParse {

    private JsonParse() {}

    /**
     * Converts jsonString into a {@link Map}
     * @param jsonString parsed
     * @return the contents of the jsonString
     */
    public static Map<String, Object> map(String jsonString) {
        return (Map<String, Object>) parse(jsonString);
    }

    /**
     * Converts jsonString into a {@link List}
     * @param jsonString parsed
     * @return the contents of the jsonString
     */
    public static List<Object> list(String jsonString) {
        return (List<Object>) parse(jsonString);
    }

    /**
     * Pulls the internal JSON string from jsonString and returns it
     * @param jsonString parsed
     * @return the contents of the jsonString
     */
    public static String string(String jsonString) {
        return (String) parse(jsonString);
    }

    /**
     * Converts jsonString into a {@link Number}, be it an integer or floating-point
     * @param jsonString parsed
     * @return the contents of the jsonString
     */
    public static Number number(String jsonString) {
        return (Number) parse(jsonString);
    }

    /**
     * Converts jsonString into a boolean
     * @param jsonString parsed
     * @return the contents of the jsonString
     */
    public static boolean bool(String jsonString) {
        return (boolean) parse(jsonString);
    }

    /**
     * Parses jsonString according to what the outermost structure is
     * @param jsonString parsed
     * @return the contents of jsonString
     */
    @SuppressWarnings("ConstantConditions")
    public static Object parse(String jsonString) {
        Stack<State> stateStack = new Stack<>();
        Type currentType;

        boolean expectingComma = false, expectingColon = false;
        int fieldStart = 0, end = jsonString.length() - 1, i = 0;
        String propertyName = null;
        Object currentContainer = null;
        Object value;
        char current;

        try {
            while (Constants.isWhitespace((current = jsonString.charAt(i)))) i++;
        } catch (IndexOutOfBoundsException e) {
            throw new JsonParseException("Provided JSON string did not contain a value");
        }

        if (current == '{') {
            currentType = Type.OBJECT;
            currentContainer = new HashMap<>();
            i++;
        } else if (current == '[') {
            currentType = Type.ARRAY;
            currentContainer = new ArrayList<>();
            propertyName = null;
            i++;
        } else if (current == '"') {
            currentType = Type.STRING;
            fieldStart = i;
        } else if (Constants.isLetter(current)) {
            // Assume parsing a constant ("null", "true", "false", etc)
            currentType = Type.CONSTANT;
            fieldStart = i;
        } else if (Constants.isNumberStart(current)) {
            currentType = Type.NUMBER;
            fieldStart = i;
        } else {
            throw new JsonParseException(stateStack, "Unexpected character \"" + current + "\" instead of root value");
        }

        while (i <= end) {
            current = jsonString.charAt(i);
            switch (currentType) {
                case NAME:
                    try {
                        ExtractedString extracted = extractString(jsonString, i);
                        i = extracted.sourceEnd;
                        propertyName = extracted.str;
                    } catch (StringIndexOutOfBoundsException e) {
                        throw new JsonParseException(stateStack, "String did not have ending quote");
                    }
                    currentType = Type.HEURISTIC;
                    expectingColon = true;
                    i++;
                    break;
                case STRING:
                    try {
                        ExtractedString extracted = extractString(jsonString, i);
                        i = extracted.sourceEnd;
                        value = extracted.str;
                    } catch (StringIndexOutOfBoundsException e) {
                        throw new JsonParseException(stateStack, "String did not have ending quote");
                    }

                    if (currentContainer == null) {
                        return value;
                    } else {
                        expectingComma = true;
                        if (currentContainer instanceof Map) {
                            ((Map<String, Object>) currentContainer).put(propertyName, value);
                            currentType = Type.OBJECT;
                        } else {
                            ((List<Object>) currentContainer).add(value);
                            currentType = Type.ARRAY;

                        }
                    }

                    i++;
                    break;
                case NUMBER: {
                    boolean withDecimal = false;
                    boolean withE = false;
                    do {
                        current = jsonString.charAt(i);
                        if (!withDecimal && current == '.') {
                            withDecimal = true;
                        } else if (!withE && (current == 'e' || current == 'E')) {
                            withE = true;
                        } else if (!Constants.isNumberStart(current) && current != '+') {
                            break;
                        }
                    } while (i++ < end);

                    String valueString = jsonString.substring(fieldStart, i);
                    try {
                        if (withDecimal || withE) {
                            value = Double.valueOf(valueString);
                        } else {
                            value = Long.valueOf(valueString);
                        }
                    } catch (NumberFormatException e) {
                        throw new JsonParseException(stateStack, "\"" + valueString +
                                "\" expected to be a number, but wasn't");
                    }

                    if (currentContainer == null) {
                        return value;
                    } else {
                        expectingComma = true;
                        if (currentContainer instanceof Map) {
                            ((Map<String, Object>) currentContainer).put(propertyName, value);
                            currentType = Type.OBJECT;
                        } else {
                            ((List<Object>) currentContainer).add(value);
                            currentType = Type.ARRAY;
                        }
                    }
                    break;
                }
                case CONSTANT:
                    while (Constants.isLetter(current) && i++ < end) {
                        current = jsonString.charAt(i);
                    }

                    String valueString = jsonString.substring(fieldStart, i);
                    switch (valueString) {
                        case "false":
                            value = false;
                            break;
                        case "true":
                            value = true;
                            break;
                        case "null":
                            value = null;
                            break;
                        default:
                            if (currentContainer instanceof Map) {
                                stateStack.push(new State(propertyName, currentContainer, Type.OBJECT));
                            } else if (currentContainer instanceof List) {
                                stateStack.push(new State(propertyName, currentContainer, Type.ARRAY));
                            }
                            throw new JsonParseException(stateStack, "\"" + valueString
                                    + "\" is not a valid constant. Missing quotes?");
                    }

                    if (currentContainer == null) {
                        return value;
                    } else {
                        expectingComma = true;
                        if (currentContainer instanceof Map) {
                            ((Map<String, Object>) currentContainer).put(propertyName, value);
                            currentType = Type.OBJECT;
                        } else {
                            ((List<Object>) currentContainer).add(value);
                            currentType = Type.ARRAY;

                        }
                    }
                    break;
                case HEURISTIC:
                    while (Constants.isWhitespace(current) && i++ < end) {
                        current = jsonString.charAt(i);
                    }

                    if (current != ':' && expectingColon) {
                        stateStack.push(new State(propertyName, currentContainer, Type.OBJECT));
                        throw new JsonParseException(stateStack, "wasn't followed by a colon");
                    }

                    if (current == ':') {
                        if (expectingColon) {
                            expectingColon = false;
                            i++;
                        } else {
                            stateStack.push(new State(propertyName, currentContainer, Type.OBJECT));
                            throw new JsonParseException(stateStack, "was followed by too many colons");
                        }
                    } else if (current == '"') {
                        currentType = Type.STRING;
                        fieldStart = i;
                    } else if (current == '{') {
                        stateStack.push(new State(propertyName, currentContainer, Type.OBJECT));
                        currentType = Type.OBJECT;
                        currentContainer = new HashMap<>();
                        i++;
                    } else if (current == '[') {
                        stateStack.push(new State(propertyName, currentContainer, Type.OBJECT));
                        currentType = Type.ARRAY;
                        currentContainer = new ArrayList<>();
                        i++;
                    } else if (Constants.isLetter(current)) {
                        // Assume parsing a constant ("null", "true", "false", etc)
                        currentType = Type.CONSTANT;
                        fieldStart = i;
                    } else if (Constants.isNumberStart(current)) {
                        // Is a number
                        currentType = Type.NUMBER;
                        fieldStart = i;
                    } else {
                        throw new JsonParseException(stateStack, "unexpected character \"" + current +
                                "\" instead of object value");
                    }
                    break;
                case OBJECT:
                    while (Constants.isWhitespace(current) && i++ < end) {
                        current = jsonString.charAt(i);
                    }

                    if (current == ',') {
                        if (expectingComma) {
                            expectingComma = false;
                            i++;
                        } else {
                            stateStack.push(new State(propertyName, currentContainer, Type.OBJECT));
                            throw new JsonParseException(stateStack, "followed by too many commas");
                        }
                    } else if (current == '"') {
                        if (expectingComma) {
                            stateStack.push(new State(propertyName, currentContainer, Type.OBJECT));
                            throw new JsonParseException(stateStack, "wasn't followed by a comma");
                        }

                        currentType = Type.NAME;
                        fieldStart = i;
                    } else if (current == '}') {
                        if (!stateStack.isEmpty()) {
                            State upper = stateStack.pop();
                            Object upperContainer = upper.container;
                            String parentName = upper.propertyName;
                            currentType = upper.type;

                            if (upperContainer instanceof Map) {
                                ((Map<String, Object>) upperContainer).put(parentName, currentContainer);
                            } else {
                                ((List<Object>) upperContainer).add(currentContainer);
                            }
                            currentContainer = upperContainer;
                            expectingComma = true;
                            i++;
                        } else {
                            return currentContainer;
                        }
                    } else if (!Constants.isWhitespace(current)) {
                        throw new JsonParseException(stateStack, "unexpected character '" + current +
                                "' where a property name is expected. Missing quotes?");
                    }
                    break;
                case ARRAY:
                    while (Constants.isWhitespace(current) && i++ < end) {
                        current = jsonString.charAt(i);
                    }

                    if (current != ',' && current != ']' && current != '}' && expectingComma) {
                        stateStack.push(new State(null, currentContainer, Type.ARRAY));
                        throw new JsonParseException(stateStack, "wasn't preceded by a comma");
                    }

                    if (current == ',') {
                        if (expectingComma) {
                            expectingComma = false;
                            i++;
                        } else {
                            stateStack.push(new State(null, currentContainer, Type.ARRAY));
                            throw new JsonParseException(stateStack, "preceded by too many commas");
                        }
                    } else if (current == '"') {
                        currentType = Type.STRING;
                        fieldStart = i;
                    } else if (current == '{') {
                        stateStack.push(new State(null, currentContainer, Type.ARRAY));
                        currentType = Type.OBJECT;
                        currentContainer = new HashMap<>();
                        i++;
                    } else if (current == '[') {
                        stateStack.push(new State(null, currentContainer, Type.ARRAY));
                        currentType = Type.ARRAY;
                        currentContainer = new ArrayList<>();
                        i++;
                    } else if (current == ']') {
                        if (!stateStack.isEmpty()) {
                            State upper = stateStack.pop();
                            Object upperContainer = upper.container;
                            String parentName = upper.propertyName;
                            currentType = upper.type;

                            if (upperContainer instanceof Map) {
                                ((Map<String, Object>) upperContainer).put(parentName, currentContainer);
                            } else {
                                ((List<Object>) upperContainer).add(currentContainer);
                            }
                            currentContainer = upperContainer;
                            expectingComma = true;
                            i++;
                        } else {
                            return currentContainer;
                        }
                    } else if (Constants.isLetter(current)) {
                        // Assume parsing a   ("null", "true", "false", etc)
                        currentType = Type.CONSTANT;
                        fieldStart = i;
                    } else if (Constants.isNumberStart(current)) {
                        // Is a number
                        currentType = Type.NUMBER;
                        fieldStart = i;
                    } else {
                        stateStack.push(new State(propertyName, currentContainer, Type.ARRAY));
                        throw new JsonParseException(stateStack, "Unexpected character \"" + current + "\" instead of array value");
                    }
                    break;
            }
        }

        throw new JsonParseException("Root element wasn't terminated correctly (Missing ']' or '}'?)");
    }

    private static ExtractedString extractString(String jsonString, int fieldStart) {
        StringBuilder builder = new StringBuilder();
        while (true) {
            int i = indexOfSpecial(jsonString, fieldStart);
            char c = jsonString.charAt(i);
            if (c == '"') {
                builder.append(jsonString.substring(fieldStart + 1, i));
                ExtractedString val = new ExtractedString();
                val.sourceEnd = i;
                val.str = builder.toString();
                return val;
            } else if (c == '\\') {
                builder.append(jsonString.substring(fieldStart + 1, i));

                c = jsonString.charAt(i + 1);
                switch (c) {
                    case '"':
                        builder.append('\"');
                        break;
                    case '\\':
                        builder.append('\\');
                        break;
                    case '/':
                        builder.append('/');
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'u':
                        builder.append(Character.toChars(
                                Integer.parseInt(jsonString.substring(i + 2, i + 6), 16)));
                        fieldStart = i + 5; // Jump over escape sequence and code point
                        continue;

                }
                fieldStart = i + 1; // Jump over escape sequence
            } else {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    static class State {
        final String propertyName;
        final Object container;
        final Type type;

        State(String propertyName, Object container, Type type) {
            this.propertyName = propertyName;
            this.container = container;
            this.type = type;
        }
    }

    /**
     * Returns the index of either a quotation, or a control character backslash. Skips the first element.
     * !! Do not inline this function, the JVM <3 optimising it, and inlining it slows it down ... somehow.
     * @param str content string to find quote or backslash
     * @param start start index to search
     * @return index of the first quote or backslash found at or after `start`
     */
    private static int indexOfSpecial(String str, int start) {
        while (++start < str.length() && str.charAt(start) != '"' && str.charAt(start) != '\\');
        return start;
    }
    private enum Type {
        ARRAY,
        OBJECT,
        HEURISTIC,
        NAME,
        STRING,
        NUMBER,
        CONSTANT

    }

    private static class ExtractedString {
        int sourceEnd;
        String str;
    }

    public static void main(String[] args) {
        int iterations = Integer.parseInt(args[0]);
        String implementation = args[1];

        String[] toParse = new String[iterations];
        for (int i = 0; i < toParse.length; i++) {
            String uuid = UUID.randomUUID().toString();
            Boolean bool = Math.floor(Math.random() * 2) == 0;
            double rand = Math.random();
            toParse[i] = "{\"a\": " + bool + ", \"uuid\": \"" + uuid + "\", \"rand\": " + rand + "}";
        }

        if ("fasterxml".equals(implementation)) {
            long before = System.currentTimeMillis();
            ObjectMapper mapper = new ObjectMapper();

            for (String parse : toParse) {
                try {
                    mapper.readValue(parse, new TypeReference<Map<String, Object>>(){});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.println(System.currentTimeMillis() - before);
        } else if ("fuzzlesoft".equals(implementation)) {
            long before = System.currentTimeMillis();

            for (String parse : toParse) {
                JsonParse.map(parse);
            }

            System.out.println(System.currentTimeMillis() - before);
        }
    }
}