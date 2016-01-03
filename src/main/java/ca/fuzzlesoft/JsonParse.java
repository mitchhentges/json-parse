package ca.fuzzlesoft;

import java.util.*;

/**
* @author mitch
* @since 30/12/15
*/
@SuppressWarnings("unchecked") //Because of reusing `currentContainer` for both maps and lists
public class JsonParse {
    public Map<String, Object> map(String jsonString) {
        jsonString = ContainingStrip.OBJECT.strip(jsonString);
        return (Map<String, Object>) parse(jsonString, Type.OBJECT);
    }

    public List<Object> list(String jsonString) {
        jsonString = ContainingStrip.ARRAY.strip(jsonString);
        return (List<Object>) parse(jsonString, Type.ARRAY);
    }

    public static Object parse(String jsonString, Type type) {
        Stack<String> propertyNameStack = new Stack<>();
        Stack<Object> containerStack = new Stack<>();
        Stack<Type> typeStack = new Stack<>();
        typeStack.push(type);
        Type currentType = type;

        boolean expectingComma = false, expectingColon = false;
        int fieldStart = 0;
        String propertyName = "";

        Object currentContainer;
        if (type == Type.OBJECT) {
            currentContainer = new HashMap<>();
        } else {
            currentContainer = new ArrayList<>();
        }

        char current;
        int i;
        for (i = 0; i < jsonString.length(); i++) {
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
                if (checkValueTermination(currentContainer, jsonString, fieldStart, i, currentType, propertyName)) typeStack.pop();
                Object upperContainer = containerStack.pop();
                if (upperContainer instanceof Map) {
                    ((Map<String, Object>) upperContainer).put(propertyNameStack.pop(), currentContainer);
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
                if (!checkValueTermination(currentContainer, jsonString, fieldStart, i, currentType, propertyName)) continue;

                expectingComma = true;
                typeStack.pop();
                currentType = typeStack.peek();
                continue;
            }

            if (current == ',') {
                expectingComma = false;
                if (!checkValueTermination(currentContainer, jsonString, fieldStart, i, currentType, propertyName)) continue;

                typeStack.pop();
                currentType = typeStack.peek();
                continue;
            } else if (expectingComma) {
                throw new JsonParseException("Properties weren't divided by commas: " + jsonString.substring(i - 20, i + 20));
            }

            if (expectingColon) {
                if (current == ':') {
                    expectingColon = false;
                    continue;
                }

                throw new JsonParseException("Expecting colon");
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
                } else {
                    throw new JsonParseException("Object property's value could not be parsed");
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
                    containerStack.push(currentContainer);
                    currentContainer = new HashMap<>();
                } else if (current == '[') {
                    typeStack.push(Type.ARRAY);
                    currentType = Type.ARRAY;
                    containerStack.push(currentContainer);
                    currentContainer = new ArrayList<>();
                } else {
                    throw new JsonParseException("Object property's value could not be parsed");
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

                throw new JsonParseException("Object contained unexpected character");
            }
        }

        checkValueTermination(currentContainer, jsonString, fieldStart, i, currentType, propertyName);
        return currentContainer;
    }

    /**
     * Handles the potential completion of a "section", returning true if a section was just completed (e.g. this is
     * called on the space after a number, completing the number)
     * @return true if section termination just occurred
     */
    private static boolean checkValueTermination(Object currentContainer, String jsonString, int fieldStart, int fieldEnd, Type currentType, String propertyName) {
        String substring = jsonString.substring(fieldStart, fieldEnd);
        Object value = null;
        if (currentType == Type.NUMBER) {
            try {
                value = Long.valueOf(substring);
            } catch (NumberFormatException e) {
                //Perhaps the number is a decimal
                try {
                    value = Double.valueOf(substring);
                } catch (NumberFormatException f) {
                    //Nope, not a decimal, invalid number
                    throw new JsonParseException("Can't parse number for: " + propertyName);
                }
            }
        } else if (currentType == Type.BOOLEAN) {
            boolean bool = Boolean.valueOf(substring);

            //If boolean isn't parsable, will get "false"
            if (!bool && !substring.equals("false")) {
                throw new JsonParseException(propertyName + "| Unable to parse value. Perhaps it needs quotes?");
            }
            value = bool;
        } else if (currentType == Type.NULL) {
            if (!substring.equals("null")) {
                throw new JsonParseException(propertyName + "| Unable to parse value. Perhaps it needs quotes?");
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