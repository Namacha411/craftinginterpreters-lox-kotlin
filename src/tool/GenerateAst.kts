import java.io.PrintWriter

fun defineVisitor(pw: PrintWriter, baseName: String, types: List<String>) {
    pw.println("\tinterface Visitor<R> {")
    for (type in types) {
        val typeName = type.split(":")[0].trim()
        pw.println("\t\tfun visit$typeName$baseName(${baseName.lowercase()}: $typeName): R")
    }
    pw.println("\t}")
}

fun defineType(pw: PrintWriter, baseName: String, className: String, fieldList: String) {
    pw.println("\tclass $className(")
    val fields = fieldList.split(",").map { x -> x.trim() }
    for (field in fields) {
        val type = field.split(" ")[0]
        val name = field.split(" ")[1]
        pw.println("\t\tval $name: $type,")
    }
    pw.println("\t) : $baseName() {")
    pw.println("\t\toverride fun <R> accept(visitor: Visitor<R>): R {")
    pw.println("\t\t\treturn visitor.visit$className$baseName(this)")
    pw.println("\t\t}")
    pw.println("\t}")
}

fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path = "$outputDir/$baseName.kt"
    val pw = PrintWriter(path)
    pw.println("abstract class $baseName {")
    defineVisitor(pw, baseName, types)
    for (type in types) {
        val className = type.split(":")[0].trim()
        val fields = type.split(":")[1].trim()
        defineType(pw, baseName, className, fields)
    }
    pw.println("\tabstract fun <R> accept(visitor: Visitor<R>): R")
    pw.println("}")
    pw.close()
}

defineAst(
    "../main/kotlin/",
    "Expr",
    arrayListOf(
        "Assign: Token name, Expr value",
        "Binary: Expr left, Token operator, Expr right",
        "Call: Expr callee, Token paren, List<Expr> arguments",
        "Get: Expr obj, Token name",
        "Grouping: Expr expression",
        "Literal: Any? value",
        "Logical: Expr left, Token operator, Expr right",
        "Set: Expr obj, Token name, Expr value",
        "Super: Token keyword, Token method",
        "This: Token keyword",
        "Unary: Token operator, Expr right",
        "Variable: Token name"
    )
)

defineAst(
    "../main/kotlin/",
    "Stmt",
    arrayListOf(
        "Block: List<Stmt> statements",
        "Class: Token name, Expr.Variable? superclass, List<Function> methods",
        "Expression: Expr expression",
        "Function: Token name, List<Token> params, List<Stmt> body",
        "If: Expr condition, Stmt thenBranch, Stmt? elseBranch",
        "Print: Expr expression",
        "Return: Token keyword, Expr? value",
        "Var: Token name, Expr? initializer",
        "While: Expr condition, Stmt body"
    )
)
