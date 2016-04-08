package ca.fuzzlesoft;

import ca.fuzzlesoft.JsonParse.State;

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

    public JsonParseException(Stack<State> stateStack, String message) {
        String jsonTrace = "";
        for (int i = 0; i < stateStack.size(); i++) {
            String name = stateStack.get(i).propertyName;
            if (name == null) {
                // Fill in array index
                List<Object> list = (List<Object>) stateStack.get(i).container;
                name = String.format("[%d]", list.size());
            }
            jsonTrace += name + (i != stateStack.size() - 1 ? "." : "");
        }

        jsonTrace = jsonTrace.equals("") ? "<root>" : jsonTrace;

        this.message = jsonTrace + ": " + message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
