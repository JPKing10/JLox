import java.util.ArrayList;
import java.util.List;

/**
 * Parse tokens from scanner to build abstract syntax tree.
 */

public class Parser {
    /* Unchecked parse exception */
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0; // next token to be parsed

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Parse statements until end of input.
     */
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    /**
     * Parse statement.
     */
    private Stmt statement() {
        if (match(TokenType.PRINT)) {
            return printStatement();
        }

        return expressionStatement();
    }

    /**
     * Evaluate print statements.
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after value.");
        return new Stmt.Print(value);
    }

    /**
     * Evaluate variable declaration.
     */
    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expected variable name.");

        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    /**
     * Evaluate expression statements.
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after value.");
        return new Stmt.Expression(expr);
    }

    /**
     * Evaluate assignments. Parse lhs, if '=' found parse rhs and wrap in assignment expression tree node.
     */
    private Expr assignment() {
        Expr expr = equality();

        if (match(TokenType.EQUAL)) {
            Token lhs = previous();
            Expr rhs = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, rhs);
            }

            error(lhs, "Invalid assignment target.");
        }

        return expr;
    }

    /**
     * When we have expression first thing to check for is assignment.
     * <p>
     * We work down the operator rules for our parser in a systematic way to ensure proper order of operations, avoiding
     * ambiguity.
     */
    private Expr expression() {
        return assignment();
    }

    /**
     * Parse variable declaration.
     */
    private Stmt declaration() {
        try {
            if (match(TokenType.VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    /**
     * Parse equality:
     * <p>
     * equality -> comparison (( "!=" | "==") comparison)*;
     */
    private Expr equality() {
        Expr expr = comparison();

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parse comparison:
     * <p>
     * comparison -> addition ((">" | ">=" | "<" | "<=") addition)*;
     */
    private Expr comparison() {
        Expr expr = addition();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = addition();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parse addition:
     * <p>
     * addition -> multiplication (("-" | "+") multiplication)*;
     */
    private Expr addition() {
        Expr expr = multiplication();

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = multiplication();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parse multiplication:
     * <p>
     * multiplication -> unary (("/" | "*") unary)*;
     */
    private Expr multiplication() {
        Expr expr = unary();

        while (match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parse unary:
     * <p>
     * unary -> ("!" | "-") unary | primary;
     */
    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        } else {
            return primary();
        }
    }

    /**
     * Parse primary:
     * <p>
     * primary -> NUMBER | STRING | "false" | "true" | "nil" | "(" expression ")";
     */
    private Expr primary() {
        if (match(TokenType.NUMBER, TokenType.STRING)) {
            Token literal = previous();
            return new Expr.Literal(literal.literal);
        } else if (match(TokenType.FALSE)) {
            return new Expr.Literal(false);
        } else if (match(TokenType.TRUE)) {
            return new Expr.Literal(true);
        } else if (match(TokenType.NIL)) {
            return new Expr.Literal(null);
        } else if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        } else if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.");
            return new Expr.Grouping(expr);
        } else {
            // No token to start expression found
            throw error(peek(), "Expected expression.");
        }
    }


    /**
     * Returns true if current token matches supplied token.
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    /**
     * Check if current token matches supplied token.
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }

        return peek().type == type;
    }

    /**
     * Returns the current token and consumes it.
     */
    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }

        return previous();
    }

    /**
     * Verify the current token has the specified type. If not, throw exception.
     */
    private Token consume(TokenType type, String errorMessage) {
        if (check(type)) {
            return advance();
        }

        throw error(peek(), errorMessage);
    }

    /**
     * Returns the current token (without consuming).
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Returns the previous token (without consuming).
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * Returns true if last token reached (EOF).
     */
    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    /**
     * Raise a parse error and inform Lox driver.
     * <p>
     * Return throwable so calling method can decide if we need to throw to unwind the parser.
     */
    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    /**
     * Panic mode recovery: synchronize on next valid statement.
     * <p>
     * Current implementation might have an error with skipping tokens? Example:
     * var x = (1+2
     * var y = 4
     */
    private void synchronize() {
        advance(); // Problem? See comment

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
