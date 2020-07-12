/**
 * Visitor that can compute values for different expressions in the AST.
 */

public class Interpreter implements Expr.Visitor<Object> {
    /**
     * Interprets the provided expression.
     *
     * @param expression The expression to interpret.
     */
    public void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
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
