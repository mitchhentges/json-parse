package ca.fuzzlesoft;

import java.util.*;

/**
* @author mitch
* @since 30/12/15
*/
public class JsonParse {
    private Stack<String> propertyNameStack;
    private Stack<Type> typeStack;
    private Stack<Object> containerStack;
    private Object currentContainer;
    private Type currentType;
    private String jsonString;

    private boolean expectingComma = false;
    private boolean expectingColon = false;
    private int fieldStart = 0;
    private int i = 0;

    private String propertyName = "";

    public Map<String, Object> map(String jsonString) {
        jsonString = ContainingStrip.OBJECT.strip(jsonString);
        return (Map<String, Object>) parse(jsonString, Type.OBJECT);
    }

    public List<Object> list(String jsonString) {
        jsonString = ContainingStrip.ARRAY.strip(jsonString);
        return (List<Object>) parse(jsonString, Type.ARRAY);
    }

    private Object parse(String jsonString, Type type) {
        this.jsonString = jsonString;
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

                    put(jsonString.substring(fieldStart, i));
                    expectingComma = true;
                    typeStack.pop();
                    currentType = typeStack.peek();
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

        checkValueTermination();
        return currentContainer;
    }

    /**
     * Handles the potential completion of a "section", returning true if a section was just completed (e.g. this is
     * called on the space after a number, completing the number)
     * @return true if section termination just occurred
     */
    private boolean checkValueTermination() {
        String substring = jsonString.substring(fieldStart, i);
        if (currentType == Type.NUMBER) {
            try {
                put(Long.valueOf(substring));
            } catch (NumberFormatException e) {
                //Perhaps the number is a decimal
                try {
                    put(Double.valueOf(substring));
                } catch (NumberFormatException f) {
                    //Nope, not a decimal, invalid number
                    throw new JsonParseException("Can't parse number for: " + propertyName);
                }
            }
        } else if (currentType == Type.BOOLEAN) {
            boolean value = Boolean.valueOf(substring);

            //If boolean isn't parsable, will get "false"
            if (!value && !substring.equals("false")) {
                throw new JsonParseException(propertyName + "| Unable to parse value. Perhaps it needs quotes?");
            }
            put(Boolean.valueOf(substring));
        } else if (currentType == Type.NULL) {
            if (!substring.equals("null")) {
                throw new JsonParseException(propertyName + "| Unable to parse value. Perhaps it needs quotes?");
            }
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