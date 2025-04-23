package com.example.lancelot.configpanel.util

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import com.example.lancelot.AppDatabase
import com.example.lancelot.KeywordGroup
import com.example.lancelot.Keywords
import com.example.lancelot.Styles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

@Serializable
data class KeywordConfig(
    val keyword: String,
    val styleId: Long? = null,
    val groupName: String? = null
)

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

    suspend fun importKeywords(uri: Uri, languageId: Long, snackbarHostState: SnackbarHostState) {
        try {
            val jsonString = readFileContent(uri)
            val keywords = json.decodeFromString<List<KeywordConfig>>(jsonString)
            
            withContext(Dispatchers.IO) {
                // Crear mapa de grupos
                val groupMap = mutableMapOf<String, Long>()
                
                keywords.forEach { config ->
                    // Si el keyword pertenece a un grupo, asegurarse de que existe
                    val groupId = config.groupName?.let { groupName ->
                        groupMap.getOrPut(groupName) {
                            database.keywordGroupDAO().insertGroup(
                                KeywordGroup(
                                    name = groupName,
                                    languageId = languageId,
                                    styleId = config.styleId
                                )
                            )
                        }
                    }
                    
                    database.keywordDAO().insertKeyword(
                        Keywords(
                            keyword = config.keyword,
                            languageId = languageId,
                            groupId = groupId,
                            styleId = config.styleId
                        )
                    )
                }
            }
            snackbarHostState.showSnackbar("Keywords imported successfully")
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Error importing keywords: ${e.message}")
        }
    }

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