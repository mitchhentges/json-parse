package ca.fuzzlesoft;

import java.util.*;

/**
 * Parses JSON, converting it into {@link List}s and {@link Map}s. Is thread safe.
 *
 * @author mitch
 * @since 30/12/15
 */
@SuppressWarnings("unchecked") //Because of reusing `currentContainer` for both maps and lists
public class JsonParse {

    private JsonParse() {}

    /**
     * Converts jsonString into a {@link Map}
     * @param jsonString parsed
     * @return the contents of the jsonString
     */
    public static Map<String, Object> map(String jsonString) {
        return (Map<String, Object>) parse(jsonString, Type.OBJECT);
    }

    /**
     * Converts jsonString into a {@link List}
     * @param jsonString parsed
     * @return the contents of the jsonString
     */
    public static List<Object> list(String jsonString) {
        return (List<Object>) parse(jsonString, Type.ARRAY);
    }

    /**
     * Parses jsonString according to what the outermost structure is
     * @param jsonString parsed
     * @param type type of outermost structure, expecting {@link Type#OBJECT} or {@link Type#ARRAY}
     * @return the contents of jsonString
     */
    @SuppressWarnings("ConstantConditions")
    public static Object parse(String jsonString, Type type) {
        Stack<String> propertyNameStack = new Stack<>();
        Stack<Object> containerStack = new Stack<>();
        Stack<Type> typeStack = new Stack<>();
        typeStack.push(Type.INITIAL);
        Type currentType = Type.INITIAL;

        boolean expectingComma = false, expectingColon = false;
        int fieldStart = 0, end = jsonString.length() - 1;
        String propertyName = null;
        Object currentContainer = null;
        Object output = null;

        char current;
        int i;
        for (i = 0; i <= end; i++) {
            current = jsonString.charAt(i);

            // Have to check if in a value string/name first. If so, ignore any special
            // characters (commas, colons, object literals, etc.)
            if (currentType == Type.STRING) {
                // Fast-forward to end of string value, which is a '"' character
                do {
                    i = jsonString.indexOf('"', i + 1);
                } while (jsonString.charAt(i - 1) == '\\');

                Object value = jsonString.substring(fieldStart, i);

                if (currentContainer != null) { // String is not outermost value, is in some container
                    if (currentContainer instanceof Map) {
                        ((Map<String, Object>) currentContainer).put(propertyName, value);
                    } else {
                        ((List<Object>) currentContainer).add(value);
                    }
                    expectingComma = true;
                } else {
                    output = value;
                }

                typeStack.pop();
                currentType = typeStack.peek();
                continue;
            }

            if (currentType == Type.NAME) {
                // Fast-forward to destination, which is an ending quote
                do {
                    i = jsonString.indexOf('"', i + 1);
                } while (jsonString.charAt(i - 1) == '\\');

                propertyName = jsonString.substring(fieldStart, i);
                typeStack.pop();
                typeStack.push(Type.HEURISTIC);
                currentType = Type.HEURISTIC;
                expectingColon = true;

                continue;
            }

            if (currentType == Type.NUMBER) {
                boolean withDecimal = false;
                while (current != ',' && current != '}' && current != ']' && !Constants.isWhitespace(current) && i < end) {
                    if (!withDecimal && current == '.' || current == 'e' || current == 'E') {
                        withDecimal = true;
                    }
                    current = jsonString.charAt(++i);
                }

                String valueString = jsonString.substring(fieldStart, i);
                Object value;
                try {
                    if (withDecimal) {
                        value = Double.valueOf(valueString);
                    } else {
                        value = Long.valueOf(valueString);
                    }
                } catch (NumberFormatException e) {
                    propertyNameStack.push(propertyName);
                    throw new JsonParseException(propertyNameStack, containerStack, "\"" + valueString
                            + "\" expected to be a number, but wasn't");
                }

                if (currentContainer == null) {
                    output = value;
                } else if (currentContainer instanceof Map) {
                    ((Map<String, Object>) currentContainer).put(propertyName, value);
                } else {
                    ((List<Object>) currentContainer).add(value);
                }

                typeStack.pop();
                currentType = typeStack.peek();

                if (Constants.isWhitespace(current)) {
                    expectingComma = true;
                    continue;
                }

                if (current != ']' && current != '}') {
                    continue;
                }
            }

            if (currentType == Type.CONSTANT) {
                while (current != ',' && current != '}' && current != ']' && !Constants.isWhitespace(current) && i < end) {
                    current = jsonString.charAt(++i);
                }

                String valueString = jsonString.substring(fieldStart, i);
                Object value;
                if (valueString.equals("false")) {
                    value = false;
                } else if (valueString.equals("true")) {
                    value = true;
                } else if (valueString.equals("null")) {
                    value = null;
                } else {
                    propertyNameStack.push(propertyName);
                    throw new JsonParseException(propertyNameStack, containerStack, "\"" + valueString
                            + "\" is not a valid constant. Missing quotes?");
                }

                if (currentContainer == null) {
                    output = value;
                } else if (currentContainer instanceof Map) {
                    ((Map<String, Object>) currentContainer).put(propertyName, value);
                } else {
                    ((List<Object>) currentContainer).add(value);
                }

                typeStack.pop();
                currentType = typeStack.peek();

                if (Constants.isWhitespace(current)) {
                    expectingComma = true;
                    continue;
                }

                if (current != ']' && current != '}') {
                    continue;
                }
            }

            if (currentType == Type.INITIAL) {
                if (output != null) {
                    // The "outside" value has been found.
                    // Fast-forward to the end of the string, make sure that there's no extra characters

                    while (i <= end) {
                        current = jsonString.charAt(i++);
                        if (!Constants.isWhitespace(current)) {
                            throw new JsonParseException("Unexpected character \"" + current + "\" found after root element");
                        }
                    }

                    return output;
                }

                if (current == '"') {
                    typeStack.push(Type.STRING);
                    currentType = Type.STRING;
                    fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
                } else if (Constants.isLetter(current)){
                    // Assume parsing a constant ("null", "true", "false", etc)
                    typeStack.push(Type.CONSTANT);
                    currentType = Type.CONSTANT;
                    fieldStart = i;
                } else if (current == '{') {
                    typeStack.push(Type.OBJECT);
                    currentType = Type.OBJECT;
                    currentContainer = new HashMap<>();
                } else if (current == '[') {
                    typeStack.push(Type.ARRAY);
                    currentType = Type.ARRAY;
                    currentContainer = new ArrayList<>();
                    propertyName = "[]";
                } else if (!Constants.isWhitespace(current)) {
                    // Is a number
                    typeStack.push(Type.NUMBER);
                    currentType = Type.NUMBER;
                    fieldStart = i;
                }

                continue;
            }

            if (current == '}' || current == ']') {
                if (!containerStack.isEmpty()) {
                    Object upperContainer = containerStack.pop();
                    String parentName = propertyNameStack.pop();

                    if (upperContainer instanceof Map) {
                        ((Map<String, Object>) upperContainer).put(parentName, currentContainer);
                    } else {
                        ((List<Object>) upperContainer).add(currentContainer);
                    }
                    currentContainer = upperContainer;
                    expectingComma = true;
                } else {
                    output = currentContainer;
                }

                typeStack.pop();
                currentType = typeStack.peek();
                continue;
            }

            if (expectingComma) {
                if (current == ',') {
                    expectingComma = false;
                } else if (!Constants.isWhitespace(current)) {
                    propertyNameStack.push(propertyName);
                    throw new JsonParseException(propertyNameStack, containerStack, "wasn't followed by a comma");
                }

                continue;
            }

            if (expectingColon) {
                if (current == ':') {
                    expectingColon = false;
                } else if (!Constants.isWhitespace(current)) {
                    propertyNameStack.push(propertyName);
                    throw new JsonParseException(propertyNameStack, containerStack,
                            "\"" + propertyName + "\" wasn't followed by a colon");
                }

                continue;
            }

            if (currentType == Type.HEURISTIC) {
                if (current == '"') {
                    typeStack.pop();
                    typeStack.push(Type.STRING);
                    currentType = Type.STRING;
                    fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
                } else if (Constants.isLetter(current)) {
                    // Assume parsing a constant ("null", "true", "false", etc)
                    typeStack.pop();
                    typeStack.push(Type.CONSTANT);
                    currentType = Type.CONSTANT;
                    fieldStart = i;
                } else if (current == '{') {
                    typeStack.pop();
                    typeStack.push(Type.OBJECT);
                    currentType = Type.OBJECT;
                    propertyNameStack.push(propertyName);
                    containerStack.push(currentContainer);
                    currentContainer = new HashMap<>();
                } else if (current == '[') {
                    typeStack.pop();
                    typeStack.push(Type.ARRAY);
                    currentType = Type.ARRAY;
                    propertyNameStack.push(propertyName);
                    containerStack.push(currentContainer);
                    currentContainer = new ArrayList<>();
                    propertyName = "[]";
                } else if (!Constants.isWhitespace(current)) {
                    // Is a number
                    typeStack.pop();
                    typeStack.push(Type.NUMBER);
                    currentType = Type.NUMBER;
                    fieldStart = i;
                }

                continue;
            }

            if (currentType == Type.OBJECT) {
                if (current == '"') {
                    typeStack.push(Type.NAME);
                    currentType = Type.NAME;
                    fieldStart = i + 1; // Don't start with `current`, as it is the beginning quotation
                    continue;
                } else if (Constants.isWhitespace(current)) {
                    continue;
                }

                throw new JsonParseException(propertyNameStack, containerStack,
                        "unexpected character '" + current + "' where a property name is expected. Missing quotes?");
            }

            if (currentType == Type.ARRAY) {
                if (current == '"') {
                    typeStack.push(Type.STRING);
                    currentType = Type.STRING;
                    fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
                } else if (Constants.isLetter(current)){
                    // Assume parsing a constant ("null", "true", "false", etc)
                    typeStack.push(Type.CONSTANT);
                    currentType = Type.CONSTANT;
                    fieldStart = i;
                } else if (current == '{') {
                    typeStack.push(Type.OBJECT);
                    currentType = Type.OBJECT;
                    propertyNameStack.push(propertyName);
                    containerStack.push(currentContainer);
                    currentContainer = new HashMap<>();
                } else if (current == '[') {
                    typeStack.push(Type.ARRAY);
                    currentType = Type.ARRAY;
                    propertyNameStack.push(propertyName);
                    containerStack.push(currentContainer);
                    currentContainer = new ArrayList<>();
                } else if (!Constants.isWhitespace(current)) {
                    // Is a number
                    typeStack.push(Type.NUMBER);
                    currentType = Type.NUMBER;
                    fieldStart = i;
                }
            }
        }

        if (output == null) {
            throw new JsonParseException("Provided string did not contain a value");
        }

        return output;
    }

    enum Type {
        NAME,
        INITIAL,
        HEURISTIC,
        NUMBER,
        STRING,
        CONSTANT,
        OBJECT,
        ARRAY
    }
}