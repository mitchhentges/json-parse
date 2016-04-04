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
    /**
     * Converts jsonString into a {@link Map}
     * @param jsonString parsed
     * @return the contents of the jsonString
     */
    public Map<String, Object> map(String jsonString) {
        return (Map<String, Object>) parse(jsonString, Type.OBJECT);
    }

    /**
     * Converts jsonString into a {@link List}
     * @param jsonString parsed
     * @return the contents of the jsonString
     */
    public List<Object> list(String jsonString) {
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
        typeStack.push(type);
        Type currentType = type;

        boolean expectingComma = false, expectingColon = false;
        int fieldStart = 0, offset, endOffset;
        String propertyName;

        Object currentContainer;
        if (type == Type.OBJECT) {
            offset = jsonString.indexOf('{');
            endOffset = jsonString.lastIndexOf('}');
            currentContainer = new HashMap<>();
            propertyName = ""; // Will be set whenever property name is entered
        } else if (type == Type.ARRAY) {
            offset = jsonString.indexOf('[');
            endOffset = jsonString.lastIndexOf(']');
            currentContainer = new ArrayList<>();
            propertyName = "[]";
        } else {
            throw new JsonParseException("Can't parse a structure that isn't an OBJECT or ARRAY");
        }

        if (offset == -1 || endOffset == -1) {
            throw new JsonParseException("Json string didn't contain an " + type);
        }

        char current;
        int i;
        for (i = offset + 1; i < endOffset; i++) {
            current = jsonString.charAt(i);

            // Have to check if in a value string/name first. If so, ignore any special
            // characters (commas, colons, object literals, etc.)
            if (currentType == Type.STRING) {
                if (current == '"') {
                    if (i - 1 >= 0 && jsonString.charAt(i - 1) == '\\') {
                        continue; //This quotation is escaped
                    }

                    Object value = jsonString.substring(fieldStart, i);
                    if (currentContainer instanceof Map) {
                        ((Map<String, Object>) currentContainer).put(propertyName, value);
                    } else {
                        ((List<Object>) currentContainer).add(value);
                    }
                    expectingComma = true;
                    typeStack.pop();
                    currentType = typeStack.peek();
                }

                continue;
            }

            if (currentType == Type.NAME) {
                if (current == '"') {
                    if (i - 1 >= 0 && jsonString.charAt(i - 1) == '\\') {
                        continue; //This quotation is escaped
                    }

                    propertyName = jsonString.substring(fieldStart, i);
                    typeStack.pop();
                    typeStack.push(Type.HEURISTIC);
                    currentType = Type.HEURISTIC;
                    expectingColon = true;
                }

                continue;
            }

            // Check ending literals next, because they can act in place of commas or whitespace in terminating a value
            if (current == '}' || current == ']') {
                if (checkValueTermination(propertyNameStack, currentContainer, jsonString, fieldStart, i, currentType, propertyName)) typeStack.pop();
                if (containerStack.isEmpty()) throw new JsonParseException("Too many closing tags");
                Object upperContainer = containerStack.pop();
                String parentName = propertyNameStack.pop();
                if (upperContainer instanceof Map) {
                    ((Map<String, Object>) upperContainer).put(parentName, currentContainer);
                } else {
                    ((List<Object>) upperContainer).add(currentContainer);
                }
                currentContainer = upperContainer;
                expectingComma = true;
                typeStack.pop();
                currentType = typeStack.peek();
                continue;
            }

            if (Constants.isWhitespace(current)) {
                if (!checkValueTermination(propertyNameStack, currentContainer, jsonString, fieldStart, i, currentType, propertyName)) continue;

                expectingComma = true;
                typeStack.pop();
                currentType = typeStack.peek();
                continue;
            }

            if (current == ',') {
                expectingComma = false;
                if (!checkValueTermination(propertyNameStack, currentContainer, jsonString, fieldStart, i, currentType, propertyName)) continue;

                typeStack.pop();
                currentType = typeStack.peek();
                continue;
            } else if (expectingComma) {
                propertyNameStack.push(propertyName);
                throw new JsonParseException(propertyNameStack, "wasn't followed by a comma");
            }

            if (expectingColon) {
                if (current == ':') {
                    expectingColon = false;
                    continue;
                }

                propertyNameStack.push(propertyName);
                throw new JsonParseException(propertyNameStack, "\"" + propertyName + "\" wasn't followed by a colon");
            }

            if (currentType == Type.HEURISTIC) {
                if (Constants.isNumber(current)) {
                    typeStack.pop();
                    typeStack.push(Type.NUMBER);
                    currentType = Type.NUMBER;
                    fieldStart = i;
                } else if (current == '"') {
                    typeStack.pop();
                    typeStack.push(Type.STRING);
                    currentType = Type.STRING;
                    fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
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
                } else {
                    // Assume parsing a constant ("null", "true", "false", etc)
                    typeStack.pop();
                    typeStack.push(Type.CONSTANT);
                    currentType = Type.CONSTANT;
                    fieldStart = i;
                }

                continue;
            }

            if (currentType == Type.ARRAY) {
                if (Constants.isNumber(current)) {
                    typeStack.push(Type.NUMBER);
                    currentType = Type.NUMBER;
                    fieldStart = i;
                } else if (current == '"') {
                    typeStack.push(Type.STRING);
                    currentType = Type.STRING;
                    fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
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
                } else {
                    typeStack.push(Type.CONSTANT);
                    currentType = Type.CONSTANT;
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
                }

                throw new JsonParseException(propertyNameStack,
                        "unexpected character '" + current + "' where a property name is expected");
            }
        }

        checkValueTermination(propertyNameStack, currentContainer, jsonString, fieldStart, i, currentType, propertyName);
        return currentContainer;
    }

    /**
     * Handles the potential completion of a "section", returning true if a section was just completed (e.g. this is
     * called on the space after a number, completing the number)
     * @return true if section termination just occurred, false otherwise
     */
    private static boolean checkValueTermination(Stack<String> propertyNameStack, Object currentContainer, String jsonString, int fieldStart, int fieldEnd, Type currentType, String propertyName) {
        String valueString = jsonString.substring(fieldStart, fieldEnd);
        Object value;
        if (currentType == Type.NUMBER) {
            try {
                value = Long.valueOf(valueString);
            } catch (NumberFormatException e) {
                //Perhaps the number is a decimal
                try {
                    value = Double.valueOf(valueString);
                } catch (NumberFormatException f) {
                    //Nope, not a decimal, invalid number
                    propertyNameStack.push(propertyName);
                    throw new JsonParseException(propertyNameStack, "\"" + valueString
                            + "\" expected to be a number, but wasn't");
                }
            }
        } else if (currentType == Type.CONSTANT) {
            if (valueString.equals("false")) {
                value = false;
            } else if (valueString.equals("true")) {
                value = true;
            } else if (valueString.equals("null")) {
                value = null;
            } else {
                propertyNameStack.push(propertyName);
                throw new JsonParseException(propertyNameStack, "\"" + valueString
                        + "\" is not a valid constant. Maybe missing quotes?");
            }
        } else {
            return false;
        }

        if (currentContainer instanceof Map) {
            ((Map<String, Object>) currentContainer).put(propertyName, value);
        } else {
            ((List<Object>) currentContainer).add(value);
        }

        return true;
    }

    enum Type {
        NAME,
        HEURISTIC,
        NUMBER,
        STRING,
        CONSTANT,
        OBJECT,
        ARRAY
    }
}