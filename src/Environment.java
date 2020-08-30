import java.util.HashMap;
import java.util.Map;

/**
 * Where Lox variables are stored.
 *
 * Map the string variable name to the object it refers to.
 */

public class Environment {
    private final Map<String, Object> values = new HashMap<>();

    /**
     * Define a variable.
     *
     * Updates value if variable with same name already exists.
     */
    public void define(String name, Object value) {
        values.put(name, value);
    }

    /**
     * Returns the value associated with an existing variable name.
     *
     * @throws RuntimeError if unknown variable
     */
    public Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme +"' on line " + name.line + ".");
    }

    /**
     * Assigns a new value to an existing variable.
     *
     * @throws RuntimeError if unknown variable
     */
    public void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "' on line " + name.line + ".");

    }

}
