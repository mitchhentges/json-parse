package ca.fuzzlesoft;

import java.util.Stack;

/**
 * @author mitch
 * @since 30/12/15
 */
public class JsonParseException extends RuntimeException {

    private final String message;

    public JsonParseException(String message) {
        this.message = message;
    }

    public JsonParseException(Stack<String> propertyStack, String message) {
        String jsonTrace = "";
        for (int i = 0; i < propertyStack.size(); i++)
            jsonTrace += propertyStack.get(i) + (i != propertyStack.size() - 1 ? "." : "");
        jsonTrace = jsonTrace.equals("") ? "<root>" : jsonTrace;

        this.message = jsonTrace + ": " + message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
