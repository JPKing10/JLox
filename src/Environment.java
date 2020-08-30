import java.util.HashMap;
import java.util.Map;

/**
 * Where Lox variables are stored.
 *
 * Supports scoping: environments chain together.
 *
 * Map the string variable name to the object it refers to.
 */

public class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    /**
     * Create an environment without an enclosing parent environment.
     */
    public Environment() {
        enclosing = null;
    }

    /**
     * Create an environment with an enclosing parent environment.
     */
    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

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
        } else if (enclosing != null) {
            return enclosing.get(name);
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
        } else if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "' on line " + name.line + ".");

    }

}
