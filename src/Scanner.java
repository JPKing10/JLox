import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans Lox source to generate tokens which can be fed to the parser.
 */

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    // Reserved keywords
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", TokenType.AND);
        keywords.put("class", TokenType.CLASS);
        keywords.put("else", TokenType.ELSE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("for", TokenType.FOR);
        keywords.put("fun", TokenType.FUN);
        keywords.put("if", TokenType.IF);
        keywords.put("nil", TokenType.NIL);
        keywords.put("or", TokenType.OR);
        keywords.put("print", TokenType.PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super", TokenType.SUPER);
        keywords.put("this", TokenType.THIS);
        keywords.put("true", TokenType.TRUE);
        keywords.put("var", TokenType.VAR);
        keywords.put("while", TokenType.WHILE);
    }

    public Scanner(String source) {
        this.source = source;
    }

    /**
     * Processes source code into tokens until end of source reached.
     *
     * Returns a list of scanned tokens.
     */
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // at beginning of next lexeme
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    /**
     * Returns true if all of the source has been scanned.
     */
    private boolean isAtEnd() {
        return source.length() <= current;
    }

    /**
     * Scan a single token.
     */
    private void scanToken() {
        char c = advance();
        switch (c) {
            // Single tokens
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;

            // Multi tokens
            case '!': addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG); break;
            case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
            case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
            case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
            case '/':
                if (match('/')) {
                    // Comment to end of line
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else if (match('*')) {
                    // Multiline comment
                    multilineComment();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;

            // whitespace and newlines
            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n': line++; break;

            case '"': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    /**
     * Scan identifiers.
     */
    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }

        // Check if identifier reserved keyword
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);

        if (type == null) {
            type = TokenType.IDENTIFIER;
        }

        addToken(type);
    }

    /**
     * Scan string literals.
     */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        // Unterminated string so error
        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // The closing "
        advance();

        // Trim the surrounding quotes
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    /**
     * Scan number literals.
     */
    private void number() {
        while (isDigit(peek())) {
            advance();
        }

        // Look for decimal points
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume
            advance();

            while (isDigit(peek())) {
                advance();
            }
        }

        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /**
     * Scan multiline comments.
     *
     * Multiline comments can be nested (unlike Java where /* is ignored if inside comment).
     */
    private void multilineComment() {
        int depth = 1; // Allow nested comments

        while (0 < depth) {
            // Unterminated multiline comment so error
            if (isAtEnd()) {
                Lox.error(line, "Unterminated multiline comment.");
                return;
            }

            char c = advance();

            switch (c) {
                case '*':
                    if (match('/')) {
                        depth--;
                    }
                    break;
                case '/':
                    if (match('*')) {
                        depth++;
                    }
                    break;
                case '\n':
                    line++;
                    break;
            }

            /*
            if (peek() == '*' && peekNext() == '/') {
                // End of current comment nest
                advance();
                advance();
                depth--;
            } else if (peek() == '/' && peekNext() == '*') {
                // Begin nested multiline comment
                advance();
                advance();
                depth++;
            } else if (peek() == '\n') {
                line++;
                advance();
            } else {
                advance();
            }
             */
        }
    }

    /**
     * Returns true if character is alphabetical.
     */
    private boolean isAlpha(char c) {
        return ('a' <= c && c <= 'z') ||
                ('A' <= c && c <= 'Z') ||
                c == '_';
    }

    /**
     * Returns true if character is alphabetical or numerical.
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    /**
     * Returns true if character is a digit.
     *
     * @param c character to check
     */
    private boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    /**
     * Matches multi-character tokens.
     *
     * @param expected given another token, this is the next expected char
     * @return true if this is the expected multichar token lox understands
     */
    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }

        if (source.charAt(current) != expected) {
            return false;
        }

        current++;
        return true;
    }

    /**
     * Lookahead looks at the current unconsumed character.
     *
     * @return the current char
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * Lookahead looks at the next unconsumed character.
     *
     * @return the next char
     */
    private char peekNext() {
        if (source.length() <= current + 1) return '\0';
        return source.charAt(current + 1);
    }

    /**
     * Returns the next char and advances the counter.
     */
    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    /**
     * Create token with no literal.
     *
     * @param type the token type
     */
    private void addToken (TokenType type) {
        addToken(type, null);
    }

    /**
     * Create new token.
     *
     * @param type the token type
     * @param literal the token literal
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

}
