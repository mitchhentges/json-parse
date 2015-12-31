package ca.fuzzlesoft;

import java.util.*;

/**
* @author mitch
* @since 30/12/15
*/
public class JsonParse {
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String NULL = "null";

    private Stack<String> propertyNameStack;
    private Stack<Type> typeStack;
    private Stack<Object> containerStack;
    private Object currentContainer;
    private Type currentType;

    private boolean escaping = false;
    private boolean expectingComma = false;
    private boolean expectingColon = false;
    private int symbolOffset = 0;

    private boolean matchesTrue = true;
    private boolean matchesFalse = true;
    private String propertyName = "";
    private String propertyValue = "";

    public Map<String, Object> map(String jsonString) {
        jsonString = ContainingStrip.OBJECT.strip(jsonString);
        return (Map<String, Object>) parse(jsonString, Type.OBJECT);
    }

    public List<Object> list(String jsonString) {
        jsonString = ContainingStrip.ARRAY.strip(jsonString);
        return (List<Object>) parse(jsonString, Type.ARRAY);
    }

    private Object parse(String jsonString, Type type) {
        propertyNameStack = new Stack<>();
        containerStack = new Stack<>();
        typeStack = new Stack<>();
        typeStack.push(type);
        currentType = type;

        if (type == Type.OBJECT) {
            currentContainer = new HashMap<>();
        } else {
            currentContainer = new ArrayList<>();
        }

        char current;
        for (int i = 0; i < jsonString.length(); i++) {
            current = jsonString.charAt(i);

            if (currentType == Type.NAME) {
                if (current == '"') {
                    typeStack.pop();
                    typeStack.push(Type.HEURISTIC);
                    currentType = Type.HEURISTIC;
                    expectingColon = true;
                } else {
                    propertyName += current;
                }

                continue;
            }

            if (currentType == Type.STRING) {
                if (current == '"' && !escaping) {
                    put(propertyValue);
                    expectingComma = true;
                    typeStack.pop();
                    currentType = typeStack.peek();
                } else if (current == '\\') {
                    if (!escaping) escaping = true;
                    propertyValue += current;
                } else {
                    propertyValue += current;
                }

                continue;
            }

            if (Constants.is(Constants.WHITE, current)) {
                if (!checkValueTermination()) continue;

                expectingComma = true;
                typeStack.pop();
                currentType = typeStack.peek();
                continue;
            }

            if (current == '}') {
                endContainer();
                continue;
            }

            if (current == ']') {
                endContainer();
                continue;
            }

            if (current == ',') {
                expectingComma = false;
                if (!checkValueTermination()) continue;

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
                    propertyValue = String.valueOf(current);
                } else if (current == '"') {
                    typeStack.pop();
                    typeStack.push(Type.STRING);
                    currentType = Type.STRING;
                    propertyValue = ""; // Don't start with `current`, as it is delimiter: '"'
                } else if (current == 't' || current == 'f') {
                    typeStack.pop();
                    typeStack.push(Type.BOOLEAN);
                    currentType = Type.BOOLEAN;
                    propertyValue = String.valueOf(current);
                    symbolOffset = 1; // Already read first character of symbol, next character will be offset 1
                    matchesTrue = true;
                    matchesFalse = true;
                } else if (current == 'n') {
                    typeStack.pop();
                    typeStack.push(Type.NULL);
                    currentType = Type.NULL;
                    propertyValue = String.valueOf(current);
                    symbolOffset = 1; // Already read first character of symbol, next character will be offset 1
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
                    propertyValue = String.valueOf(current);
                } else if (current == '"') {
                    typeStack.push(Type.STRING);
                    currentType = Type.STRING;
                    propertyValue = ""; // Don't start with `current`, as it is delimiter: '"'
                } else if (current == 't' || current == 'f') {
                    typeStack.push(Type.BOOLEAN);
                    currentType = Type.BOOLEAN;
                    propertyValue = String.valueOf(current);
                    symbolOffset = 1; // Already read first character of symbol, next character will be offset 1
                    matchesTrue = true;
                    matchesFalse = true;
                } else if (current == 'n') {
                    typeStack.push(Type.NULL);
                    currentType = Type.NULL;
                    propertyValue = String.valueOf(current);
                    symbolOffset = 1; // Already read first character of symbol, next character will be offset 1
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
                    propertyName = "";
                    continue;
                }

                throw new JsonParseException("Object contained unexpected character");
            }

            if (currentType == Type.NUMBER) {
                if (!Constants.is(Constants.NUMBERS, current)) {
                    throw new JsonParseException("Unexpected character in number property value");
                }

                propertyValue += current;
                continue;
            }

            if (currentType == Type.BOOLEAN) {
                if (matchesTrue && (symbolOffset >= TRUE.length() || current != TRUE.charAt(symbolOffset))) {
                    matchesTrue = false;
                }
                if (matchesFalse && (symbolOffset >= FALSE.length() || current != FALSE.charAt(symbolOffset))) {
                    matchesFalse = false;
                }

                if (!matchesTrue && !matchesFalse) {
                    throw new JsonParseException("Unexpected property value: " + propertyValue);
                }

                propertyValue += current;
                symbolOffset++;
                continue;
            }

            if (currentType == Type.NULL) {
                if (symbolOffset >= NULL.length() || current != NULL.charAt(symbolOffset)) {
                    throw new JsonParseException("Unexpected property value: " + propertyValue);
                }

                propertyValue += current;
                symbolOffset++;
            }
        }

        checkValueTermination();
        return currentContainer;
    }

    /**
     * Handles the potential completion of a "section", returning true if a section was just completed (e.g. this is
     * called on the space after a number, completing the number)
     * @return true if section termination just occurred
     */
    private boolean checkValueTermination() {
        if (currentType == Type.NUMBER) {
            try {
                put(Long.valueOf(propertyValue));
            } catch (NumberFormatException e) {
                //Perhaps the number is a decimal
                try {
                    put(Double.valueOf(propertyValue));
                } catch (NumberFormatException f) {
                    //Nope, not a decimal, invalid number
                    throw new JsonParseException("Can't parse number for property: " + propertyName);
                }
            }
        } else if (currentType == Type.BOOLEAN) {
            put(Boolean.valueOf(propertyValue));
        } else if (currentType == Type.NULL) {
            put(null);
        } else {
            return false;
        }
        return true;
    }

    private void put(Object value) {
        if (currentContainer instanceof Map) {
            ((Map<String, Object>) currentContainer).put(propertyName, value);
        } else {
            ((List<Object>) currentContainer).add(value);
        }
    }

    private void endContainer() {
        if (checkValueTermination()) typeStack.pop();
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