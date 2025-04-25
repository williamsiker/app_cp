package com.example.lancelot.configpanel.util

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import com.example.lancelot.AppDatabase
import com.example.lancelot.Styles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

@Serializable
data class StyleConfig(
    val name: String,
    val color: String,
    val sizeFont: Int,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false
)

class ConfigImporter(private val context: Context, private val database: AppDatabase) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importStyles(uri: Uri, snackbarHostState: SnackbarHostState) {
        try {
            val jsonString = readFileContent(uri)
            val styles = json.decodeFromString<List<StyleConfig>>(jsonString)
            
            withContext(Dispatchers.IO) {
                styles.forEach { config ->
                    database.styleDAO().insertStyle(
                        Styles(
                            name = config.name,
                            color = config.color,
                            sizeFont = config.sizeFont,
                            isBold = config.isBold,
                            isItalic = config.isItalic,
                            isUnderline = config.isUnderline
                        )
                    )
                }
            }
            snackbarHostState.showSnackbar("Styles imported successfully")
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Error importing styles: ${e.message}")
        }
    }

    private suspend fun readFileContent(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.joinToString("\n")
            }
        } ?: throw Exception("Could not read file")
    }
}