class LoxFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment,
    private val isInitializer: Boolean
) : LoxCallable {

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment, isInitializer)
    }

    override fun arity(): Int {
        return declaration.params.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (i in 0..<declaration.params.size) {
            environment.define(declaration.params[i].lexeme, arguments[i])
        }
        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            if (isInitializer) {
                return closure.getAt(0, "this")
            }
            return returnValue.value
        }
        if (isInitializer) return closure.getAt(0, "this")
        return null
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme} >"
    }
}