package com.example.lancelot.rust

object RustBridge {
    init {
        System.loadLibrary("runix") // sin "lib" ni ".so"
    }
    external fun initLogger()
    external fun helloRust() : String
    external fun tokenizeCode(code: String, languageName: String) : Array<Token>
    external fun executeCode(code: String, languageName: String, input: String) : String
}