import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox {
    companion object {
        private var hadError = false

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
                runLox(line)
                hadError = false
            }
        }

        private fun runFile(path: String) {
            val bytes = Files.readAllBytes(Paths.get(path))
            runLox(String(bytes, Charset.defaultCharset()))
            if (hadError) {
                exitProcess(65)
            }
        }

        private fun runLox(source: String) {
            val scanner = Scanner(source)
            val tokens = scanner.scanTokens()

            for (token in tokens) {
                println(token)
            }
        }

        fun error(line: Int, message: String) {
            report(line, "", message)
        }

        fun report(line: Int, where: String, message: String) {
            System.err.println("[line $line] Error $where: $message")
            hadError = true
        }
    }
}