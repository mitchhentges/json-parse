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
     * Parses jsonString according to what the outermost structure is
     * @param jsonString parsed
     * @return the contents of jsonString
     */
    @SuppressWarnings("ConstantConditions")
    public static Object parse(String jsonString) {
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

        int i = 0;
        while (i <= end) {
            char current = jsonString.charAt(i);
            switch (currentType) {
                case NAME:
                    // Fast-forward to destination, which is an ending quote
                    do {
                        i = jsonString.indexOf('"', i + 1);
                    } while (jsonString.charAt(i - 1) == '\\');

                    propertyName = jsonString.substring(fieldStart, i);
                    typeStack.pop();
                    typeStack.push(Type.HEURISTIC);
                    currentType = Type.HEURISTIC;
                    expectingColon = true;
                    i++;
                    break;
                case STRING:
                    // Fast-forward to end of string value, which is a '"' character
                    do {
                        i = jsonString.indexOf('"', i + 1);
                    } while (jsonString.charAt(i - 1) == '\\');

                    output = jsonString.substring(fieldStart, i);
                    typeStack.pop();
                    currentType = Type.DONE;
                    current = jsonString.charAt(i++);
                    break;
                case OBJECT_STRING: {
                    // Fast-forward to end of string value, which is a '"' character
                    do {
                        i = jsonString.indexOf('"', i + 1);
                    } while (jsonString.charAt(i - 1) == '\\');

                    Object value = jsonString.substring(fieldStart, i);
                    ((Map<String, Object>) currentContainer).put(propertyName, value);
                    expectingComma = true;

                    typeStack.pop();
                    currentType = Type.OBJECT;
                    current = jsonString.charAt(i++);
                    break;
                }
                case ARRAY_STRING: {
                    // Fast-forward to end of string value, which is a '"' character
                    do {
                        i = jsonString.indexOf('"', i + 1);
                    } while (jsonString.charAt(i - 1) == '\\');

                    Object value = jsonString.substring(fieldStart, i);
                    ((List<Object>) currentContainer).add(value);
                    expectingComma = true;

                    typeStack.pop();
                    currentType = Type.ARRAY;
                    current = jsonString.charAt(i++);
                    break;
                }
                case NUMBER: {
                    boolean withDecimal = false;
                    while (!Constants.isWhitespace(current) && i++ < end) {
                        if (!withDecimal && current == '.' || current == 'e' || current == 'E') {
                            withDecimal = true;
                        }
                        current = jsonString.charAt(i);
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
                        containerStack.push(currentContainer);
                        throw new JsonParseException(propertyNameStack, containerStack, "\"" + valueString
                                + "\" expected to be a number, but wasn't");
                    }

                    output = value;
                    typeStack.pop();
                    currentType = Type.DONE;
                    break;
                }
                case OBJECT_NUMBER: {
                    boolean withDecimal = false;
                    while (current != ',' && current != '}' && !Constants.isWhitespace(current) && i++ < end) {
                        if (!withDecimal && current == '.' || current == 'e' || current == 'E') {
                            withDecimal = true;
                        }
                        current = jsonString.charAt(i);
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
                        containerStack.push(currentContainer);
                        throw new JsonParseException(propertyNameStack, containerStack, "\"" + valueString
                                + "\" expected to be a number, but wasn't");
                    }

                    ((Map<String, Object>) currentContainer).put(propertyName, value);
                    typeStack.pop();
                    currentType = Type.OBJECT;
                    expectingComma = true;

                    break;
                }
                case ARRAY_NUMBER: {
                    boolean withDecimal = false;
                    while (current != ',' && current != ']' && !Constants.isWhitespace(current) && i++ < end) {
                        if (!withDecimal && current == '.' || current == 'e' || current == 'E') {
                            withDecimal = true;
                        }
                        current = jsonString.charAt(i);
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
                        containerStack.push(currentContainer);
                        throw new JsonParseException(propertyNameStack, containerStack, "\"" + valueString
                                + "\" expected to be a number, but wasn't");
                    }

                    ((List<Object>) currentContainer).add(value);
                    typeStack.pop();
                    currentType = Type.ARRAY;
                    expectingComma = true;

                    break;
                }
                case CONSTANT: {
                    while (!Constants.isWhitespace(current) && i++ < end) {
                        current = jsonString.charAt(i);
                    }

                    String valueString = jsonString.substring(fieldStart, i);
                    switch (valueString) {
                        case "false":
                            output = false;
                            break;
                        case "true":
                            output = true;
                            break;
                        case "null":
                            output = null;
                            break;
                        default:
                            propertyNameStack.push(propertyName);
                            containerStack.push(currentContainer);
                            throw new JsonParseException(propertyNameStack, containerStack, "\"" + valueString
                                    + "\" is not a valid constant. Missing quotes?");
                    }

                    typeStack.pop();
                    currentType = Type.DONE;
                    break;
                }
                case OBJECT_CONSTANT: {
                    while (current != ',' && current != '}' && !Constants.isWhitespace(current) && i++ < end) {
                        current = jsonString.charAt(i);
                    }

                    String valueString = jsonString.substring(fieldStart, i);
                    Object value;
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
                            propertyNameStack.push(propertyName);
                            containerStack.push(currentContainer);
                            throw new JsonParseException(propertyNameStack, containerStack, "\"" + valueString
                                    + "\" is not a valid constant. Missing quotes?");
                    }

                    ((Map<String, Object>) currentContainer).put(propertyName, value);
                    typeStack.pop();
                    currentType = Type.OBJECT;
                    expectingComma = true;

                    break;
                }
                case ARRAY_CONSTANT: {
                    while (current != ',' && current != '}' && current != ']' && !Constants.isWhitespace(current) && i++ < end) {
                        current = jsonString.charAt(i);
                    }

                    String valueString = jsonString.substring(fieldStart, i);
                    Object value;
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
                            propertyNameStack.push(propertyName);
                            containerStack.push(currentContainer);
                            throw new JsonParseException(propertyNameStack, containerStack, "\"" + valueString
                                    + "\" is not a valid constant. Missing quotes?");
                    }

                    ((List<Object>) currentContainer).add(value);
                    typeStack.pop();
                    currentType = Type.ARRAY;
                    expectingComma = true;

                    break;
                }
                case DONE:
                    while (true) {
                        if (!Constants.isWhitespace(current)) {
                            throw new JsonParseException("Unexpected character \"" + current + "\" found after root element");
                        }
                        i++;
                        if (i >= end) {
                            break;
                        }
                        current = jsonString.charAt(i);
                    }

                    return output;
                case INITIAL:
                    while (Constants.isWhitespace(current) && i++ < end) {
                        current = jsonString.charAt(i);
                    }

                    if (current == '"') {
                        typeStack.pop();
                        typeStack.push(Type.STRING);
                        currentType = Type.STRING;
                        fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
                        i++;
                    } else if (current == '{') {
                        typeStack.pop();
                        typeStack.push(Type.OBJECT);
                        currentType = Type.OBJECT;
                        currentContainer = new HashMap<>();
                        i++;
                    } else if (current == '[') {
                        typeStack.pop();
                        typeStack.push(Type.ARRAY);
                        currentType = Type.ARRAY;
                        currentContainer = new ArrayList<>();
                        propertyName = "[]";
                        i++;
                    } else if (Constants.isLetter(current)) {
                        // Assume parsing a constant ("null", "true", "false", etc)
                        typeStack.pop();
                        typeStack.push(Type.CONSTANT);
                        currentType = Type.CONSTANT;
                        fieldStart = i;
                    } else if (Constants.isNumberStart(current)) {
                        typeStack.pop();
                        typeStack.push(Type.NUMBER);
                        currentType = Type.NUMBER;
                        fieldStart = i;
                    } else {
                        throw new JsonParseException(propertyNameStack, containerStack,
                                "Unexpected character \"" + current + "\" instead of root value");
                    }
                    break;
                case HEURISTIC:
                    while (Constants.isWhitespace(current) && i++ < end) {
                        current = jsonString.charAt(i);
                    }

                    if (current != ':' && expectingColon) {
                        propertyNameStack.push(propertyName);
                        containerStack.push(currentContainer);
                        throw new JsonParseException(propertyNameStack, containerStack,
                                "wasn't followed by a colon");
                    }

                    if (current == ':') {
                        if (expectingColon) {
                            expectingColon = false;
                            i++;
                        } else {
                            throw new JsonParseException(propertyNameStack, containerStack,
                                    "wasn't followed by too many colons");
                        }
                    } else if (current == '"') {
                        typeStack.pop();
                        typeStack.push(Type.OBJECT_STRING);
                        currentType = Type.OBJECT_STRING;
                        fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
                        i++;
                    } else if (current == '{') {
                        typeStack.pop();
                        typeStack.push(Type.OBJECT);
                        currentType = Type.OBJECT;
                        propertyNameStack.push(propertyName);
                        containerStack.push(currentContainer);
                        currentContainer = new HashMap<>();
                        i++;
                    } else if (current == '[') {
                        typeStack.pop();
                        typeStack.push(Type.ARRAY);
                        currentType = Type.ARRAY;
                        propertyNameStack.push(propertyName);
                        containerStack.push(currentContainer);
                        currentContainer = new ArrayList<>();
                        propertyName = "[]";
                        i++;
                    } else if (Constants.isLetter(current)) {
                        // Assume parsing a constant ("null", "true", "false", etc)
                        typeStack.pop();
                        typeStack.push(Type.OBJECT_CONSTANT);
                        currentType = Type.OBJECT_CONSTANT;
                        fieldStart = i;
                    } else if (Constants.isNumberStart(current)) {
                        // Is a number
                        typeStack.pop();
                        typeStack.push(Type.OBJECT_NUMBER);
                        currentType = Type.OBJECT_NUMBER;
                        fieldStart = i;
                    } else {
                        throw new JsonParseException(propertyNameStack, containerStack,
                                "unexpected character \"" + current + "\" instead of object value");
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
                            propertyNameStack.push(propertyName);
                            containerStack.push(currentContainer);
                            throw new JsonParseException(propertyNameStack, containerStack,
                                    "followed by too many commas");
                        }
                    } else if (current == '"') {
                        if (expectingComma) {
                            propertyNameStack.push(propertyName);
                            containerStack.push(currentContainer);
                            throw new JsonParseException(propertyNameStack, containerStack,
                                    "wasn't followed by a comma");
                        }

                        typeStack.push(Type.NAME);
                        currentType = Type.NAME;
                        fieldStart = i + 1; // Don't start with `current`, as it is the beginning quotation
                        i++;
                    } else if (current == '}') {
                        typeStack.pop();
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
                            currentType = typeStack.peek();
                        } else {
                            output = currentContainer;
                            currentType = Type.DONE;
                        }

                        i++;
                    } else if (!Constants.isWhitespace(current)) {
                        throw new JsonParseException(propertyNameStack, containerStack,
                                "unexpected character '" + current + "' where a property name is expected. Missing quotes?");
                    }
                    break;
                case ARRAY:
                    while (Constants.isWhitespace(current) && i++ < end) {
                        current = jsonString.charAt(i);
                    }

                    if (current != ',' && current != ']' && expectingComma) {
                        propertyNameStack.push(propertyName);
                        containerStack.push(currentContainer);
                        throw new JsonParseException(propertyNameStack, containerStack,
                                "wasn't preceded by a comma");
                    }

                    if (current == ',') {
                        if (expectingComma) {
                            expectingComma = false;
                            i++;
                        } else {
                            propertyNameStack.push(propertyName);
                            containerStack.push(currentContainer);
                            throw new JsonParseException(propertyNameStack, containerStack,
                                    "preceded by too many commas");
                        }
                    } else if (current == '"') {
                        typeStack.push(Type.ARRAY_STRING);
                        currentType = Type.ARRAY_STRING;
                        fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
                        // TODO remove fieldStart
                        i++;
                    } else if (current == '{') {
                        typeStack.push(Type.OBJECT);
                        currentType = Type.OBJECT;
                        propertyNameStack.push(propertyName);
                        containerStack.push(currentContainer);
                        currentContainer = new HashMap<>();
                        i++;
                    } else if (current == '[') {
                        typeStack.push(Type.ARRAY);
                        currentType = Type.ARRAY;
                        propertyNameStack.push(propertyName);
                        containerStack.push(currentContainer);
                        currentContainer = new ArrayList<>();
                        i++;
                    } else if (current == ']') {
                        typeStack.pop();
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
                            currentType = typeStack.peek();
                        } else {
                            output = currentContainer;
                            currentType = Type.DONE;
                        }

                        i++;
                    } else if (Constants.isLetter(current)) {
                        // Assume parsing a constant ("null", "true", "false", etc)
                        typeStack.push(Type.ARRAY_CONSTANT);
                        currentType = Type.ARRAY_CONSTANT;
                        fieldStart = i;
                    } else if (Constants.isNumberStart(current)) {
                        // Is a number
                        typeStack.push(Type.ARRAY_NUMBER);
                        currentType = Type.ARRAY_NUMBER;
                        fieldStart = i;
                    } else {
                        propertyNameStack.push(propertyName);
                        containerStack.push(currentContainer);
                        throw new JsonParseException("Unexpected character \"" + current + "\" instead of array value");
                    }
                    break;
            }
        }

        if (output == null) {
            throw new JsonParseException("Provided string did not contain a value");
        }

        return output;
    }

    enum Type {
        INITIAL,
        DONE,
        ARRAY,
        OBJECT,
        HEURISTIC,
        NAME,
        STRING,
        NUMBER,
        CONSTANT,
        OBJECT_STRING,
        OBJECT_NUMBER,
        OBJECT_CONSTANT,
        ARRAY_STRING,
        ARRAY_NUMBER,
        ARRAY_CONSTANT,
    }
}