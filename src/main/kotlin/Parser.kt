import java.lang.RuntimeException

class Parser(
    private val tokens: List<Token>,
) {
    private companion object {
        class ParseError : RuntimeException()
    }

    private var current = 0

    fun parse(): List<Stmt> {
        val statements = ArrayList<Stmt>()
        while (!isAtEnd()) {
            statements.add(declaration())
        }
        return statements
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = or()
        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()
            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            } else if (expr is Expr.Get) {
                return Expr.Set(expr.obj, expr.name, value)
            }
            error(equals, "invalid assignment target.")
        }
        return expr
    }

    private fun or(): Expr {
        var expr = and()
        while (match(TokenType.OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun and(): Expr {
        var expr = equality()
        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun declaration(): Stmt {
        try {
            if (match(TokenType.CLASS)) {
                return classDeclaration()
            }
            if (match(TokenType.FUN)) {
                return function("function")
            }
            if (match(TokenType.VAR)) {
                return varDeclaration()
            }
            return statement()
        } catch (error: ParseError) {
            synchronize()
            throw ParseError()
        }
    }

    private fun classDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect class name.")
        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.")

        val methods = ArrayList<Stmt.Function>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.")
        return Stmt.Class(name, methods)
    }

    private fun statement(): Stmt {
        if (match(TokenType.FOR)) {
            return forStatement()
        }
        if (match(TokenType.IF)) {
            return ifStatement()
        }
        if (match(TokenType.PRINT)) {
            return printStatement()
        }
        if (match(TokenType.RETURN)) {
            return returnStatement()
        }
        if (match(TokenType.WHILE)) {
            return whileStatement()
        }
        if (match(TokenType.LEFT_BRACE)) {
            return Stmt.Block(block())
        }
        return expressionStatement()
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after for.")
        val initializer = if (match(TokenType.SEMICOLON)) {
            null
        } else if (match(TokenType.VAR)) {
            varDeclaration()
        } else {
            expressionStatement()
        }
        // initializerはStmtなので;まで含めてパースされるので直後のconsumeは必要ない、他はExpr
        val condition = if (!check(TokenType.SEMICOLON)) {
            expression()
        } else {
            Expr.Literal(true)
        }
        consume(TokenType.SEMICOLON, "Expect ';' after for condition.")
        val increment = if (!check(TokenType.RIGHT_PAREN)) {
            expression()
        } else {
            null
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")
        var body = statement()

        // forをwhileにデシュガリング
        if (increment != null) {
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        }
        body = Stmt.While(condition, body)
        if (initializer != null) {
            body = Stmt.Block(listOf(initializer, body))
        }

        return body
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after if.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.")
        val thenBranch = statement()
        val elseBranch = if (match(TokenType.ELSE)) {
            statement()
        } else {
            null
        }
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (!check(TokenType.SEMICOLON)) {
            expression()
        } else {
            null
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
        val initializer = if (match(TokenType.EQUAL)) {
            expression()
        } else {
            null
        }
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement()
        return Stmt.While(condition, body)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Stmt.Expression(expr)
    }

    private fun function(kind: String): Stmt.Function {
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters = ArrayList<Token>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")
        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()
        return Stmt.Function(name, parameters, body)
    }

    private fun block(): List<Stmt> {
        val statements = ArrayList<Stmt>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration())
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun equality(): Expr {
        var expr = comparison()
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun comparison(): Expr {
        var expr = term()
        while (match(TokenType.GRATER, TokenType.GRATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun term(): Expr {
        var expr = factor()
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun factor(): Expr {
        var expr = unary()
        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }
        return call()
    }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            expr = if (match(TokenType.LEFT_PAREN)) {
                finishCall(expr)
            } else if (match(TokenType.DOT)) {
                val name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.")
                Expr.Get(expr, name)
            } else {
                break
            }
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = ArrayList<Expr>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Can't have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }
        val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")
        return Expr.Call(callee, paren, arguments)
    }

    private fun primary(): Expr {
        if (match(TokenType.FALSE)) {
            return Expr.Literal(false)
        }
        if (match(TokenType.TRUE)) {
            return Expr.Literal(true)
        }
        if (match(TokenType.NIL)) {
            return Expr.Literal(null)
        }
        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return Expr.Literal(previous().literal)
        }
        if (match(TokenType.THIS)) {
            return Expr.This(previous())
        }
        if (match(TokenType.IDENTIFIER)) {
            return Expr.Variable(previous())
        }
        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }
        throw error(peek(), "Expect expression.")
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) {
            return advance()
        }
        throw error(peek(), message)
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) {
            return false
        }
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) {
            current++
        }
        return previous()
    }

    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.EOF
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun error(token: Token, message: String): ParseError {
        Lox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) {
                return
            }
            when (peek().type) {
                TokenType.CLASS,
                TokenType.FUN,
                TokenType.IF,
                TokenType.PRINT,
                TokenType.RETURN,
                TokenType.VAR,
                TokenType.WHILE -> return

                else -> advance()
            }
        }
    }
}