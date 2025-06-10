package com.example.lancelot.snippets

/** Simple data class representing a code snippet suggestion. */
data class Snippet(
    val prefix: String,
    val body: String,
    val description: String
)
