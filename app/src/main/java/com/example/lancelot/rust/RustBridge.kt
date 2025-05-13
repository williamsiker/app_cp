package com.example.lancelot.rust

import java.util.concurrent.CompletableFuture

object RustBridge {
    init {
        System.loadLibrary("runix") // sin "lib" ni ".so"
    }
    external fun initLogger()
    external fun helloRust() : String
    external fun parseIncremental(code: String, languageName: String, tree_ptr: Long) : Long
    external fun tokenizeCode(code: String, languageName: String, tree_ptr: Long) : Array<Token>
    external fun executeCode(code: String, languageName: String, input: String) : String
    external fun freeTree(tree_ptr: Long)
    
    external fun ktFuture(
        code: String,
        languageName: String,
        input: String
    ) : CompletableFuture<String>
}
