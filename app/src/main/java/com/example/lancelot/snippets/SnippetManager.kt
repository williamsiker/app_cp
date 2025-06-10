package com.example.lancelot.snippets

/**
 * Manages snippets available for different languages.
 */
object SnippetManager {
    private val snippetsByLanguage: Map<String, List<Snippet>> = mapOf(
        "cpp" to listOf(
            Snippet(
                prefix = "for",
                body = "for (int i = 0; i < N; ++i) {\n    \n}",
                description = "For loop"
            ),
            Snippet(
                prefix = "while",
                body = "while (condition) {\n    \n}",
                description = "While loop"
            ),
            Snippet(
                prefix = "func",
                body = "void functionName() {\n    \n}",
                description = "Function"
            ),
            Snippet(
                prefix = "class",
                body = "class ClassName {\npublic:\n    ClassName();\n    ~ClassName();\n};",
                description = "Class skeleton"
            )
        ),
        "kotlin" to listOf(
            Snippet(
                prefix = "for",
                body = "for (i in 0 until n) {\n    \n}",
                description = "For loop"
            ),
            Snippet(
                prefix = "while",
                body = "while (condition) {\n    \n}",
                description = "While loop"
            ),
            Snippet(
                prefix = "fun",
                body = "fun functionName() {\n    \n}",
                description = "Function"
            ),
            Snippet(
                prefix = "class",
                body = "class ClassName {\n    \n}",
                description = "Class skeleton"
            )
        )
    )

    fun getSnippets(language: String): List<Snippet> =
        snippetsByLanguage[language] ?: emptyList()

    fun getSuggestions(language: String, prefix: String): List<Snippet> {
        if (prefix.isBlank()) return emptyList()
        return getSnippets(language).filter { it.prefix.startsWith(prefix) }
    }
}
