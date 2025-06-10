package com.example.lancelot.common

/** Simple wrapper representing success or error results from native calls. */
sealed class RustResult<out T> {
    data class Success<T>(val data: T) : RustResult<T>()
    data class Error(val error: String) : RustResult<Nothing>()
}
