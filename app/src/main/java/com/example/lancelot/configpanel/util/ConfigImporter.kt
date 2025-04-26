package com.example.lancelot.configpanel.util

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import com.example.lancelot.AppDatabase
import com.example.lancelot.Styles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
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
            // Mostrar estado de carga
            snackbarHostState.showSnackbar("Importing styles...")
            
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
            snackbarHostState.showSnackbar("${styles.size} styles imported successfully")
        } catch (e: Exception) {
            snackbarHostState.showSnackbar(
                message = "Error importing styles",
                actionLabel = "Retry",
                duration = SnackbarDuration.Long
            )
        }
    }

    private suspend fun readFileContent(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedInputStream(inputStream).use { buffered ->
                buffered.reader().use { reader ->
                    reader.readText()
                }
            }
        } ?: throw Exception("Could not read file")
    }
}