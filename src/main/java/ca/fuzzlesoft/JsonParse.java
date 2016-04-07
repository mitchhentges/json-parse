package ca.fuzzlesoft;

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
     * Parses jsonString according to what the outermost structure is
     * @param jsonString parsed
     * @return the contents of jsonString
     */
    @SuppressWarnings("ConstantConditions")
    public static Object parse(String jsonString) {
        Stack<State> stateStack = new Stack<>();
        Type currentType;

        boolean done = false, expectingComma = false, expectingColon = false;
        int fieldStart = 0, end = jsonString.length() - 1, i = 0;
        String propertyName = null;
        Object currentContainer = null;
        Object output = null;
        Object value;
        char current;

        while (Constants.isWhitespace((current = jsonString.charAt(i)))) {
            if (i >= end) {
                throw new JsonParseException("Provided string did not contain a value");
            }
            i++;
        }

        if (current == '{') {
            currentType = Type.OBJECT;
            currentContainer = new HashMap<>();
            i++;
        } else if (current == '[') {
            currentType = Type.ARRAY;
            currentContainer = new ArrayList<>();
            propertyName = "[]";
            i++;
        } else if (current == '"') {
            currentType = Type.STRING;
            fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
            i++;
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

        while (!done && i <= end) {
            current = jsonString.charAt(i);
            switch (currentType) {
                case NAME:
                    // Fast-forward to destination, which is an ending quote
                    do {
                        i = jsonString.indexOf('"', i + 1);
                    } while (jsonString.charAt(i - 1) == '\\');

                    propertyName = jsonString.substring(fieldStart, i);
                    currentType = Type.HEURISTIC;
                    expectingColon = true;
                    i++;
                    break;
                case STRING:
                    // Fast-forward to end of string value, which is a '"' character
                    do {
                        i = jsonString.indexOf('"', i + 1);
                    } while (jsonString.charAt(i - 1) == '\\');

                    value = jsonString.substring(fieldStart, i);
                    if (currentContainer == null) {
                        output = value;
                        done = true;
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
                    while (Constants.isNumber(current) && i++ < end) {
                        if (!withDecimal && current == '.' || current == 'e' || current == 'E') {
                            withDecimal = true;
                        }
                        current = jsonString.charAt(i);
                    }

                    String valueString = jsonString.substring(fieldStart, i);
                    try {
                        if (withDecimal) {
                            value = Double.valueOf(valueString);
                        } else {
                            value = Long.valueOf(valueString);
                        }
                    } catch (NumberFormatException e) {
                        /*if (currentContainer != null) {
                            propertyNameStack.push(propertyName);
                            containerStack.push(currentContainer);
                        }*/
                        throw new JsonParseException(stateStack, "\"" + valueString +
                                "\" expected to be a number, but wasn't");
                    }

                    if (currentContainer == null) {
                        output = value;
                        done = true;
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
                            stateStack.push(new State(propertyName, currentContainer, Type.ARRAY));
                            throw new JsonParseException(stateStack, "\"" + valueString
                                    + "\" is not a valid constant. Missing quotes?");
                    }

                    if (currentContainer == null) {
                        output = value;
                        done = true;
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
                        stateStack.push(new State(propertyName, currentContainer, Type.ARRAY));
                        throw new JsonParseException(stateStack, "wasn't followed by a colon");
                    }

                    if (current == ':') {
                        if (expectingColon) {
                            expectingColon = false;
                            i++;
                        } else {
                            throw new JsonParseException(stateStack, "wasn't followed by too many colons");
                        }
                    } else if (current == '"') {
                        currentType = Type.STRING;
                        fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
                        i++;
                    } else if (current == '{') {
                        stateStack.push(new State(propertyName, currentContainer, Type.OBJECT));
                        currentType = Type.OBJECT;
                        currentContainer = new HashMap<>();
                        i++;
                    } else if (current == '[') {
                        stateStack.push(new State(propertyName, currentContainer, Type.OBJECT));
                        currentType = Type.ARRAY;
                        currentContainer = new ArrayList<>();
                        propertyName = "[]";
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
                            stateStack.push(new State(propertyName, currentContainer, Type.ARRAY));
                            throw new JsonParseException(stateStack, "followed by too many commas");
                        }
                    } else if (current == '"') {
                        if (expectingComma) {
                            stateStack.push(new State(propertyName, currentContainer, Type.ARRAY));
                            throw new JsonParseException(stateStack, "wasn't followed by a comma");
                        }

                        currentType = Type.NAME;
                        fieldStart = i + 1; // Don't start with `current`, as it is the beginning quotation
                        i++;
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
                            output = currentContainer;
                            done = true;
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
                        stateStack.push(new State(propertyName, currentContainer, Type.ARRAY));
                        throw new JsonParseException(stateStack, "wasn't preceded by a comma");
                    }

                    if (current == ',') {
                        if (expectingComma) {
                            expectingComma = false;
                            i++;
                        } else {
                            stateStack.push(new State(propertyName, currentContainer, Type.ARRAY));
                            throw new JsonParseException(stateStack, "preceded by too many commas");
                        }
                    } else if (current == '"') {
                        currentType = Type.STRING;
                        fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
                        i++;
                    } else if (current == '{') {
                        stateStack.push(new State(propertyName, currentContainer, Type.ARRAY));
                        currentType = Type.OBJECT;
                        currentContainer = new HashMap<>();
                        i++;
                    } else if (current == '[') {
                        stateStack.push(new State(propertyName, currentContainer, Type.ARRAY));
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
                            output = currentContainer;
                            done = true;
                        }
                    } else if (Constants.isLetter(current)) {
                        // Assume parsing a constant ("null", "true", "false", etc)
                        currentType = Type.CONSTANT;
                        fieldStart = i;
                    } else if (Constants.isNumberStart(current)) {
                        // Is a number
                        currentType = Type.NUMBER;
                        fieldStart = i;
                    } else {
                        stateStack.push(new State(propertyName, currentContainer, Type.ARRAY));
                        throw new JsonParseException("Unexpected character \"" + current + "\" instead of array value");
                    }
                    break;
            }
        }

        if (!done) {
            throw new JsonParseException("Root element wasn't terminated correctly (Missing ']' or '}'?)");
        }

        while (++i <= end) {
            current = jsonString.charAt(i);
            if (!Constants.isWhitespace(current)) {
                throw new JsonParseException("Unexpected character \"" + current + "\" found after root element");
            }
        }

        return output;
    }

    enum Type {
        ARRAY,
        OBJECT,
        HEURISTIC,
        NAME,
        STRING,
        NUMBER,
        CONSTANT
    }

    static class State {
        final String propertyName;
        final Object container;
        final Type type;

        public State(String propertyName, Object container, Type type) {
            this.propertyName = propertyName;
            this.container = container;
            this.type = type;
        }
    }
}