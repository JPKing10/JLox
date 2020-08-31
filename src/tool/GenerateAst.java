package tool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Tool to generate classes for abstract syntax tree.
 */

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        String outputDir = args[0];

        // define our syntax
        // name of class : comma separated fields
        // Expression syntax: evaluate to value
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign : Token name, Expr value",
                "Binary : Expr left, Token operator, Expr right",
                "Grouping : Expr expression",
                "Literal : Object value",
                "Logical : Expr left, Token operator, Expr right",
                "Unary : Token operator, Expr right",
                "Variable : Token name"
        ));

        // Statement syntax: Cause side effect
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block: List<Stmt> statements",
                "Expression: Expr expression",
                "Print : Expr expression",
                "If : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Var : Token name, Expr initializer",
                "While : Expr condition, Stmt body"
        ));
    }

    /**
     * Generates Java source representation of AST.
     *
     * @param outputDir directory for source output
     * @param baseName name of source
     * @param types types of expression to define
     * @throws IOException
     */
    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);

        // writer.println("pkg");
        // writer.println();
        writer.println("// Generated AST");
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");
        writer.println("  abstract <R> R accept(Visitor<R> visitor);");

        defineVisitor(writer, baseName, types);

        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        writer.println("}");
        writer.close();
    }


    /**
     * Generate visitor interface.
     *
     * Visitor pattern helps us get the best of both worlds from OOP/Fun programming to retain separation of concerns
     * wrt the expression structures, which will be used at multiple stages of the interpreter, and the methods
     * involved in interpretation of expressions, which will only be used during interpretation.
     */
    public static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("  interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("    R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("  }");
        writer.println();

    }

    /**
     * Generate a specific type as inline subclass.
     */
    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        String[] fields = fieldList.split(", ");

        writer.println("  static class " + className + " extends " + baseName + " {");

        // Instance variables
        for (String field : fields) {
            writer.println("    final " + field + ";");
        }

        writer.println();

        // Constructor
        writer.println("    " + className + "(" + fieldList + ") {");

        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("      this." + name + " = " + name + ";");
        }

        writer.println("    }");

        // Visit method
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit" + className + baseName + "(this);");
        writer.println("    }");


        writer.println("  }");
        writer.println();
    }
}
