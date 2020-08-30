import java.util.List;

/**
 * Visitor that can compute values for different expressions in the AST.
 */

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment environment = new Environment(); // Store vars here

    /**
     * Interprets the program.
     *
     * @param statements The list of statements to interpret.
     */
    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    /**
     * Helper method to intrepret statement.
     *
     * @param statement The statement to interpret.
     */
    private void execute(Stmt statement) {
        statement.accept(this);
    }

    /**
     * Visit expression statements by evaluating statement expression.
     */
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    /**
     * Evaluates expression and prints value.
     */
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }


    /**
     * Create new variable and evaluate initializer if it exists.
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object val = null;
        if (stmt.initializer != null) {
            val = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, val);
        return null;
    }

    /**
     * Visit assignment.
     */
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        if (expr.operator.type == TokenType.PLUS) {
            // Can add numbers or strings
            if (left instanceof Double && right instanceof Double) {
                return (double)left + (double)right;
            } else if (left instanceof String && right instanceof String) {
                return (String)left + (String)right;
            } else {
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            }
        }

        checkNumberOperands(expr.operator, left, right);

        switch (expr.operator.type) {
            // Comparisons
            case GREATER:
                return (double)left > (double)right;
            case GREATER_EQUAL:
                return (double)left >= (double)right;
            case LESS:
                return (double) left < (double)right;
            case LESS_EQUAL:
                return (double) left <= (double)right;
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
            // Operations
            case MINUS:
                return (double)left - (double)right;
            case SLASH:
                return (double)left / (double)right;
            case STAR:
                return (double)left * (double)right;
        }

        return null;
    }


    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                return -(double)right;
            default: return null;
        }
    }

    /**
     * Returns value stored in environment for variable name.
     */
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    /**
     * Evaluate subexpressions.
     */
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    /**
     * Returns the truthy value of object: false if nil or false, else true.
     */
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    /**
     * Returns true if two objects are equal.
     */
    private boolean isEqual(Object a, Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }

    /**
     * Throws runtime error if operand not number.
     */
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    /**
     * Throws runtime error if operands not numbers.
     */
    private void checkNumberOperands(Token operator, Object... operands) {
        for (Object operand : operands) {
            if (!(operand instanceof Double)) {
                throw new RuntimeError(operator, "Operands must be numbers.");
            }
        }
    }

    /**
     * Converts any Lox value to a string.
     */
    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                // Java double toString includes decimal, but for integers we cut this off
                text = text.substring(0, text.length() - 2);
            }
            return text;
        } else {
            return object.toString();
        }
    }

}
