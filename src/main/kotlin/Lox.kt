import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox {
    companion object {
        private val interpreter = Interpreter()
        private var hadError = false
        private var hadRuntimeError = false

        fun main(args: Array<String>) {
            if (args.size > 1) {
                println("Usage: klox [script]")
                exitProcess(64)
            } else if (args.size == 1) {
                runFile(args[0])
            } else {
                runPrompt()
            }
        }

        private fun runPrompt() {
            while (true) {
                print("> ")
                val line = readlnOrNull() ?: break
                run(line)
                hadError = false
            }
        }

        private fun runFile(path: String) {
            val bytes = Files.readAllBytes(Paths.get(path))
            run(String(bytes, Charset.defaultCharset()))
            if (hadError) {
                exitProcess(65)
            }
            if (hadRuntimeError) {
                exitProcess(70)
            }
        }

        private fun run(source: String) {
            val scanner = Scanner(source)
            val tokens = scanner.scanTokens()
            val parser = Parser(tokens)
            val statements = parser.parse()

            if (hadError) {
                return
            }

            interpreter.interpret(statements)
        }

        fun error(line: Int, message: String) {
            report(line, "", message)
        }

        fun error(token: Token, message: String) {
            if (token.type == TokenType.EOF) {
                token.line?.let { report(it, "at end", message) }
            } else {
                token.line?.let { report(it, "at '${token.lexeme}'", message) }
            }
        }

        fun runtimeError(error: RuntimeError) {
            println("${error.message}\n[line ${error.token.line}]")
            hadRuntimeError = true
        }

        private fun report(line: Int, where: String, message: String) {
            System.err.println("[line $line] Error $where: $message")
            hadError = true
        }
    }
}