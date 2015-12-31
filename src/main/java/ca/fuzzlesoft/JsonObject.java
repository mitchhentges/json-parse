package ca.fuzzlesoft;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mitch
 * @since 12/30/15
 */
public class JsonObject implements Container {

    private final Map<String, Object> map = new HashMap<>();

    @Override
    public void push(Object object) {

    }

    @Override
    public Object internal() {
        return null;
    }
}
