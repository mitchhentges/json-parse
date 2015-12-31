package ca.fuzzlesoft;

/**
 * @author mitch
 * @since 12/30/15
 */
public interface Container {
    /**
     * Adds the value onto the object. It might be a key, or just a value - it's up to the implementor to decide at
     * runtime.
     * @param object pushed onto container
     */
    void push(Object object);

    /**
     * Returns the internal map or list which is holding the data.
     * @return internal data structure
     */
    Object internal();
}
