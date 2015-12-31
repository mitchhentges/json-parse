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
    private Stack<Map<String, Object>> mapStack;
    private Map<String, Object> currentMap;
    private Type currentType;

    private boolean expectingComma = false;
    private boolean expectingColon = false;
    private int symbolOffset = 0;

    private boolean matchesTrue = true;
    private boolean matchesFalse = true;
    private String propertyName = "";
    private String propertyValue = "";

    public Map<String, Object> map(String jsonString) {
        String inner = ContainingStrip.OBJECT.strip(jsonString);
        return internalMap(inner);
    }

    public List<Object> list(String jsonString) {
        return new ArrayList<>();
    }

    private Map<String, Object> internalMap(String jsonString) {
        propertyNameStack = new Stack<>();
        mapStack = new Stack<>();
        currentMap = new HashMap<>();
        typeStack = new Stack<>();
        typeStack.push(Type.OBJECT);
        currentType = Type.OBJECT;

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
                if (current == '"') {
                    currentMap.put(propertyName, propertyValue);
                    expectingComma = true;
                    typeStack.pop();
                    currentType = typeStack.peek();
                } else {
                    propertyValue += current;
                }

                continue;
            }

            if (Constants.is(Constants.WHITE, current)) {
                if (currentType == Type.NUMBER) {
                    try {
                        currentMap.put(propertyName, Long.valueOf(propertyValue));
                    } catch (NumberFormatException e) {
                        //Perhaps the number is a decimal
                        try {
                            currentMap.put(propertyName, Double.valueOf(propertyValue));
                        } catch (NumberFormatException f) {
                            //Nope, not a decimal, invalid number
                            throw new JsonParseException("Can't parse number for property: " + propertyName);
                        }
                    }
                } else if (currentType == Type.BOOLEAN) {
                    currentMap.put(propertyName, Boolean.valueOf(propertyValue));
                } else if (currentType == Type.NULL) {
                    currentMap.put(propertyName, null);
                } else {
                    continue;
                }

                expectingComma = true;
                typeStack.pop();
                currentType = typeStack.peek();
                continue;
            }

            if (current == '}') {
                if (currentType == Type.NUMBER) {
                    try {
                        currentMap.put(propertyName, Long.valueOf(propertyValue));
                    } catch (NumberFormatException e) {
                        //Perhaps the number is a decimal
                        try {
                            currentMap.put(propertyName, Double.valueOf(propertyValue));
                        } catch (NumberFormatException f) {
                            //Nope, not a decimal, invalid number
                            throw new JsonParseException("Can't parse number for property: " + propertyName);
                        }
                    }
                } else if (currentType == Type.BOOLEAN) {
                    currentMap.put(propertyName, Boolean.valueOf(propertyValue));
                } else if (currentType == Type.NULL) {
                    currentMap.put(propertyName, null);
                }

                mapStack.peek().put(propertyNameStack.pop(), currentMap);
                currentMap = mapStack.pop();
                expectingComma = true;
                typeStack.pop();
                currentType = typeStack.peek();
                continue;
            }

            if (current == ',') {
                expectingComma = false;
                if (currentType == Type.NUMBER) {
                    try {
                        currentMap.put(propertyName, Long.valueOf(propertyValue));
                    } catch (NumberFormatException e) {
                        //Perhaps the number is a decimal
                        try {
                            currentMap.put(propertyName, Double.valueOf(propertyValue));
                        } catch (NumberFormatException f) {
                            //Nope, not a decimal, invalid number
                            throw new JsonParseException("Can't parse number for property: " + propertyName);
                        }
                    }
                } else if (currentType == Type.BOOLEAN) {
                    currentMap.put(propertyName, Boolean.valueOf(propertyValue));
                } else if (currentType == Type.NULL) {
                    currentMap.put(propertyName, null);
                } else {
                    continue;
                }

                typeStack.pop();
                currentType = typeStack.peek();
                continue;
            } else if (expectingComma) {
                throw new JsonParseException("Properties weren't divided by commas");
            }

            if (expectingColon) {
                if (current == ':') {
                    expectingColon = false;
                    continue;
                }

                throw new JsonParseException("Expecting colon");
            }

            if (currentType == Type.HEURISTIC) {
                //object, list, string, number, boolean, null
                if (Constants.is(Constants.NUMBERS, current)) {
                    typeStack.pop();
                    typeStack.push(Type.NUMBER);
                    currentType = Type.NUMBER;
                    propertyValue = String.valueOf(current);
                } else if (current == '"') {
                    typeStack.pop();
                    typeStack.push(Type.STRING);
                    currentType = Type.STRING;
                    propertyValue = ""; // Don't use `current`, as it is delimiter: '"'
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
                    mapStack.push(currentMap);
                    currentMap = new HashMap<>();
                } else {
                    throw new JsonParseException("Property value was not of known type");
                }

                continue;
            }

            if (currentType == Type.OBJECT) {
                if (current == '"') {
                    typeStack.push(Type.NAME);
                    currentType = Type.NAME;
                    propertyName = "";
                } else if (current == '}') {
                    mapStack.peek().put(propertyNameStack.pop(), currentMap);
                    currentMap = mapStack.pop();
                    expectingComma = true;
                    typeStack.pop();
                    currentType = typeStack.peek();
                } else {
                    throw new JsonParseException("Object contained unexpected character");
                }

                continue;
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
                continue;
            }
        }

        if (currentType == Type.NUMBER) {
            try {
                currentMap.put(propertyName, Long.valueOf(propertyValue));
            } catch (NumberFormatException e) {
                //Perhaps the number is a decimal
                try {
                    currentMap.put(propertyName, Double.valueOf(propertyValue));
                } catch (NumberFormatException f) {
                    //Nope, not a decimal, invalid number
                    throw new JsonParseException("Can't parse number for property: " + propertyName);
                }
            }
        } else if (currentType == Type.BOOLEAN) {
            currentMap.put(propertyName, Boolean.valueOf(propertyValue));
        } else if (currentType == Type.NULL) {
            currentMap.put(propertyName, null);
        }
        return currentMap;
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