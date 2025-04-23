package com.example.lancelot.utils

object FileUtils {
    fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".cpp", true) -> "text/x-c++src"
            fileName.endsWith(".c", true) -> "text/x-csrc"
            fileName.endsWith(".h", true) -> "text/x-chdr"
            fileName.endsWith(".java", true) -> "text/x-java-source"
            fileName.endsWith(".py", true) -> "text/x-python"
            fileName.endsWith(".kt", true) -> "text/x-kotlin"
            fileName.endsWith(".js", true) -> "text/javascript"
            fileName.endsWith(".ts", true) -> "text/typescript"
            fileName.endsWith(".html", true) -> "text/html"
            fileName.endsWith(".css", true) -> "text/css"
            fileName.endsWith(".md", true) -> "text/markdown"
            fileName.endsWith(".json", true) -> "application/json"
            fileName.endsWith(".xml", true) -> "application/xml"
            else -> "text/plain"
        }
    }
}