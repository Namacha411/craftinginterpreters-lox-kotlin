enum class TokenType {
    // 記号1個のトークン
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    COMMA,
    DOT,
    MINUS,
    PLUS,
    SEMICOLON,
    SLASH,
    STAR,

    // 記号1個または記号2個のトークン
    BANG,
    BANG_EQUAL,
    EQUAL,
    EQUAL_EQUAL,
    GRATER,
    GRATER_EQUAL,
    LESS,
    LESS_EQUAL,

    // リテラル
    IDENTIFIER,
    STRING,
    NUMBER,

    // キーワード
    AND,
    CLASS,
    ELSE,
    FALSE,
    FUN,
    FOR,
    IF,
    NIL,
    OR,
    PRINT,
    RETURN,
    SUPER,
    THIS,
    TRUE,
    VAR,
    WHILE,

    EOF
}