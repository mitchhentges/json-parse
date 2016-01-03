package ca.fuzzlesoft;

import java.util.*;

/**
* @author mitch
* @since 30/12/15
*/
@SuppressWarnings("unchecked") //Because of reusing `currentContainer` for both maps and lists
public class JsonParse {
    public Map<String, Object> map(String jsonString) {
        return (Map<String, Object>) parse(jsonString, Type.OBJECT);
    }

    public List<Object> list(String jsonString) {
        return (List<Object>) parse(jsonString, Type.ARRAY);
    }

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
        } else {
            offset = jsonString.indexOf('[');
            endOffset = jsonString.lastIndexOf(']');
            currentContainer = new ArrayList<>();
            propertyName = "[]";
        }

        if (offset == -1 || endOffset == -1) {
            throw new JsonParseException("Json string didn't contain an " + type);
        }

        char current;
        int i;
        for (i = offset + 1; i < endOffset; i++) {
            current = jsonString.charAt(i);

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

            if (current == '}' || current == ']') {
                if (checkValueTermination(propertyNameStack, currentContainer, jsonString, fieldStart, i, currentType, propertyName)) typeStack.pop();
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

            if (Constants.is(Constants.WHITE, current)) {
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
                if (Constants.is(Constants.NUMBERS, current)) {
                    typeStack.pop();
                    typeStack.push(Type.NUMBER);
                    currentType = Type.NUMBER;
                    fieldStart = i;
                } else if (current == '"') {
                    typeStack.pop();
                    typeStack.push(Type.STRING);
                    currentType = Type.STRING;
                    fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
                } else if (current == 't' || current == 'f') {
                    typeStack.pop();
                    typeStack.push(Type.BOOLEAN);
                    currentType = Type.BOOLEAN;
                    fieldStart = i;
                } else if (current == 'n') {
                    typeStack.pop();
                    typeStack.push(Type.NULL);
                    currentType = Type.NULL;
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
                } else {
                    propertyNameStack.push(propertyName);
                    throw new JsonParseException(propertyNameStack, "value could not be parsed");
                }

                continue;
            }

            if (currentType == Type.ARRAY) {
                if (Constants.is(Constants.NUMBERS, current)) {
                    typeStack.push(Type.NUMBER);
                    currentType = Type.NUMBER;
                    fieldStart = i;
                } else if (current == '"') {
                    typeStack.push(Type.STRING);
                    currentType = Type.STRING;
                    fieldStart = i + 1; // Don't start with current `i`, as it is delimiter: '"'
                } else if (current == 't' || current == 'f') {
                    typeStack.push(Type.BOOLEAN);
                    currentType = Type.BOOLEAN;
                    fieldStart = i;
                } else if (current == 'n') {
                    typeStack.push(Type.NULL);
                    currentType = Type.NULL;
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
                } else {
                    propertyNameStack.push(propertyName);
                    throw new JsonParseException(propertyNameStack, "value could not be parsed");
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
     * @return true if section termination just occurred
     */
    private static boolean checkValueTermination(Stack<String> propertyNameStack, Object currentContainer, String jsonString, int fieldStart, int fieldEnd, Type currentType, String propertyName) {
        String valueString = jsonString.substring(fieldStart, fieldEnd);
        Object value = null;
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
                    throw new JsonParseException(propertyNameStack, "\"" + valueString + "\" is an invalid value");
                }
            }
        } else if (currentType == Type.BOOLEAN) {
            boolean bool = Boolean.valueOf(valueString);

            //If boolean isn't parsable, will get "false"
            if (!bool && !valueString.equals("false")) {
                propertyNameStack.push(propertyName);
                throw new JsonParseException(propertyNameStack, "\"" + valueString + "\" is an invalid value");
            }
            value = bool;
        } else if (currentType == Type.NULL) {
            if (!valueString.equals("null")) {
                propertyNameStack.push(propertyName);
                throw new JsonParseException(propertyNameStack, "\"" + valueString + "\" is an invalid value");
            }
            //toPut is null by default
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
        BOOLEAN,
        NULL,
        OBJECT,
        ARRAY
    }
}