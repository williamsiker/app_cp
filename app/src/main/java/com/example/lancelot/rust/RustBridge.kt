package com.example.lancelot.rust

import android.util.Log
import com.example.lancelot.common.RustResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CompletableFuture

object RustBridge {
    private const val TAG = "RustBridge"
    private const val MAX_TEXT_LENGTH = 100_000
    private val initMutex = Mutex()
    private var isInitialized = false
    
    private suspend fun initializeSafely() {
        runCatching {
            if (!isInitialized) {
                initMutex.withLock {
                    if (!isInitialized) {
                        System.loadLibrary("runix")
                        initLogger()
                        isInitialized = true
                    }
                }
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to initialize RustBridge", e)
            throw RuntimeException("Failed to initialize RustBridge", e)
        }
    }

    suspend fun highlightSafe(
        code: String,
        languageName: String,
        h: String,
        i: String,
        l: String,
        t: String,
        hn: String
    ): RustResult<String> = runCatching {
        if (!isInitialized) {
            initializeSafely()
        }
        
        if (code.isBlank()) {
            return RustResult.Success("")
        }

        val result = highlight(
            code.take(MAX_TEXT_LENGTH),
            languageName,
            h.ifBlank { "{}" },
            i.ifBlank { "{}" },
            l.ifBlank { "{}" },
            t.ifBlank { "{}" },
            hn.ifBlank { "[]" }
        )
        
        if (result.isBlank()) {
            RustResult.Error("Highlighting failed: empty result")
        } else {
            RustResult.Success(result)
        }
    }.fold(
        onSuccess = { it },
        onFailure = {
            Log.e(TAG, "Error in highlighting", it)
            RustResult.Error("Highlighting failed: ${it.message}")
        }
    )

    suspend fun realTimeHighlightSafe(
        code: String,
        languageName: String,
        h: String,
        i: String,
        l: String,
        t: String,
        hn: String
    ): RustResult<String> = runCatching {
        if (!isInitialized) {
            initializeSafely()
        }

        if (code.isBlank()) {
            return RustResult.Success("")
        }

        val result = realTimeHighlight(
            code.take(MAX_TEXT_LENGTH),
            languageName,
            h.ifBlank { "{}" },
            i.ifBlank { "{}" },
            l.ifBlank { "{}" },
            t.ifBlank { "{}" },
            hn.ifBlank { "[]" }
        )

        if (result.isBlank()) {
            RustResult.Error("Highlighting failed: empty result")
        } else {
            RustResult.Success(result)
        }
    }.fold(
        onSuccess = { it },
        onFailure = {
            Log.e(TAG, "Error in highlighting", it)
            RustResult.Error("Highlighting failed: ${it.message}")
        }
    )

    external fun initLogger()
    private external fun helloRust() : String
    external fun highlight(
        code: String,
        languageName: String,
        h: String,
        i: String,
        l: String,
        t: String,
        hn: String
    ) : String

    private external fun realTimeHighlight(
        code: String,
        languageName: String,
        h: String,
        i: String,
        l: String,
        t: String,
        hn: String
    ) : String


    external fun executeCode(code: String, languageName: String, input: String) : String

    external fun executeCodeDetailed(code: String, languageName: String, input: String) : String

    external fun ktFuture(
        code: String,
        languageName: String,
        input: String
    ) : CompletableFuture<String>
}
