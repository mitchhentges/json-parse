package ca.fuzzlesoft;

import java.util.List;
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

    public JsonParseException(Stack<String> propertyStack, Stack<Object> containerStack, String message) {
        String jsonTrace = "";
        for (int i = 0; i < propertyStack.size(); i++) {
            String name = propertyStack.get(i);
            if (name.equals("[]")) {
                // Fill in array index
                List<Object> list = (List<Object>) containerStack.get(i);
                name = String.format("[%d]", list.size());
            }
            jsonTrace += name + (i != propertyStack.size() - 1 ? "." : "");
        }

        jsonTrace = jsonTrace.equals("") ? "<root>" : jsonTrace;

        this.message = jsonTrace + ": " + message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
