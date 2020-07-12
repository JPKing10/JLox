import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Lox is a scripting language described in Crafting Interpreters.
 *
 * Here we handle user interaction, such as loading a script or an interactive interpreter.
 */

public class Lox {
    /* Interpreter used in REPL session */
    private static final Interpreter interpreter = new Interpreter();

    /* Error flags */
    private static boolean hadError = false; // If error occurs carry on scanning to find more errors but don't exec
    private static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (1 < args.length) { // unexpected arg length
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    /**
     * Read script and execute directly from a source file.
     *
     * @param filepath path to source file
     * @throws IOException
     */
    private static void runFile(String filepath) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filepath));
        run(new String(bytes, Charset.defaultCharset()));

        // Error in source
        if (hadError) {
            // Scan/parsing
            System.exit(65);
        } else if (hadRuntimeError) {
            // Interpretation
            System.exit(70);
        }
    }

    /**
     * Start interactive prompt (no source file given).
     *
     * Prompt reads lines from stdio and runs them in the interpreter.
     *
     * @throws IOException
     */
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while(true) {
            System.out.print("> ");
            run(reader.readLine());
            hadError = false; // Reset error flag so interactive prompt can continue exec
        }
    }

    /**
     * Core interpreter function.
     *
     * Scan source, parse tokens, print AST.
     *
     * @param source source string to be executed by the interpreter
     */
    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();

        // Stop on error
        if (hadError) return;

        interpreter.interpret(expression);
    }


    /**
     * Error during source code scan.
     *
     * @param line Line of being interpreted when error occurred
     * @param message Description of the type of error
     */
    static void error(int line, String message) {
        report(line, "", message);
    }

    /**
     * Error during parsing tokens.
     *
     * @param token Current token when error raised
     * @param errorMessage Error message
     */
    static void error(Token token, String errorMessage) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", errorMessage);
        } else {
            report(token.line, " at '" + token.lexeme + "'", errorMessage);
        }
    }

    /**
     * Error during interpretation (like type error).
     *
     * @param error RuntimeError error object with token and message
     */
    static void runtimeError(RuntimeError error) {
        report(error.token.line, "", error.getMessage());
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message) {
        System.err.println( "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}
