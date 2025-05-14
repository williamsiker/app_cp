package com.example.lancelot.config

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class ConfigManager private constructor() {
    private var applicationContext: Context? = null

    companion object {
        @Volatile
        private var instance: ConfigManager? = null

        fun getInstance(): ConfigManager =
            instance ?: synchronized(this) {
                instance ?: ConfigManager().also { instance = it }
            }
    }

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun readQueryFile(language: String, fileName: String): String? {
        return try {
            val path = "queries/${language.lowercase()}/$fileName"
            applicationContext?.assets?.open(path)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            Log.d("ConfigManager", "Error reading query file: $e")
            null
        }
    }
    
    fun readLanguageQueries(language: String): LanguageQueries? {
        return try {
            val highlights = readQueryFile(language, "highlights.scm")
            val injections = readQueryFile(language, "injections.scm")
            val locals = readQueryFile(language, "locals.scm")

//            Log.d("ConfigManager", "Highlights: $highlights")
//            Log.d("ConfigManager", "Injections: $injections")
//            Log.d("ConfigManager", "Locals: $locals")
            
            if (highlights != null && injections != null && locals != null) {
                LanguageQueries(highlights, injections, locals)
            } else null
        } catch (e: Exception) {
            Log.e("ConfigManager", "Error reading language queries: $e")
            null
        }
    }
}

data class LanguageQueries(
    val highlights: String,
    val injections: String,
    val locals: String
)
