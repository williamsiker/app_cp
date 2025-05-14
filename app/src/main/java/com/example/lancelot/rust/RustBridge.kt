package com.example.lancelot.rust

import java.util.concurrent.CompletableFuture

object RustBridge {
    init {
        System.loadLibrary("runix") // sin "lib" ni ".so"
    }
    external fun initLogger()
    external fun helloRust() : String
    external fun highlight(
        code: String,
        languageName: String,
        h: String,
        i: String,
        l: String,
        t: String,
        hn: String
    ) : String


    external fun executeCode(code: String, languageName: String, input: String) : String

    external fun ktFuture(
        code: String,
        languageName: String,
        input: String
    ) : CompletableFuture<String>
}
