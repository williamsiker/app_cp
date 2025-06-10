package com.example.lancelot.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.lancelot.common.RustResult
import com.example.lancelot.rust.RustBridge
import com.example.lancelot.rust.Token
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import android.util.Log
import org.json.JSONObject

class EditorState(
    initialText: String = "",
    private val languageName: String,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private var highlightJob: Job? = null
    private val debounceTime = 150L // ms
    
    var text by mutableStateOf(initialText)
        private set
    
    private var lastProcessedText = initialText
    private val _highlightedTokens = MutableStateFlow<List<Token>>(emptyList())
    val highlightedTokens = _highlightedTokens
    
    private var isProcessing = false
    
    fun onTextChanged(newText: String) {
        if (newText == text) return
        
        scope.launch(Dispatchers.Main) {
            mutex.withLock {
                text = newText
                scheduleHighlighting()
            }
        }
    }
    
    private fun scheduleHighlighting() {
        highlightJob?.cancel()
        highlightJob = scope.launch(Dispatchers.IO) {
            try {
                delay(debounceTime)
                if (text == lastProcessedText) return@launch
                
                mutex.withLock {
                    if (isProcessing) return@launch
                    isProcessing = true
                    
                    try {
                        val currentText = text
                        val result = RustBridge.highlightSafe(
                            code = currentText,
                            languageName = languageName,
                            h = getHighlightsQuery(),
                            i = getInjectionsQuery(),
                            l = getLocalsQuery(),
                            t = getThemeJson(),
                            hn = getHighlightNames()
                        )
                        
                        when (result) {
                            is RustResult.Success -> {
                                val tokens = parseHighlightResult(result.data)
                                _highlightedTokens.value = tokens
                                lastProcessedText = currentText
                            }
                            is RustResult.Error -> {
                                Log.e("EditorState", "Highlighting failed: ${result.error}")
                                // Mantener los tokens anteriores en caso de error
                            }
                        }
                    } finally {
                        isProcessing = false
                    }
                }
            } catch (e: Exception) {
                Log.e("EditorState", "Error in highlighting", e)
            }
        }
    }
    
    private fun getHighlightsQuery(): String {
        return try {
            // Implementar lógica para obtener highlights query
            ""
        } catch (e: Exception) {
            Log.e("EditorState", "Error getting highlights query", e)
            ""
        }
    }
    
    private fun getInjectionsQuery(): String {
        return try {
            // Implementar lógica para obtener injections query
            ""
        } catch (e: Exception) {
            Log.e("EditorState", "Error getting injections query", e)
            ""
        }
    }
    
    private fun getLocalsQuery(): String {
        return try {
            // Implementar lógica para obtener locals query
            ""
        } catch (e: Exception) {
            Log.e("EditorState", "Error getting locals query", e)
            ""
        }
    }
    
    private fun getThemeJson(): String {
        return try {
            // Implementar lógica para obtener theme json
            "{}"
        } catch (e: Exception) {
            Log.e("EditorState", "Error getting theme json", e)
            "{}"
        }
    }
    
    private fun getHighlightNames(): String {
        return try {
            // Implementar lógica para obtener highlight names
            "[]"
        } catch (e: Exception) {
            Log.e("EditorState", "Error getting highlight names", e)
            "[]"
        }
    }

    private fun parseHighlightResult(json: String): List<Token> {
        return try {
            val obj = JSONObject(json)
            val ranges = obj.getJSONArray("ranges")
            val names = obj.getJSONArray("highlight_names")
            val tokens = mutableListOf<Token>()
            for (i in 0 until ranges.length()) {
                val range = ranges.getJSONObject(i)
                val start = range.getInt("start")
                val end = range.getInt("end")
                val typeIdx = range.getInt("highlight_type")
                val type = if (typeIdx < names.length()) names.getString(typeIdx) else "text"
                tokens.add(Token(type, type, intArrayOf(start, end, 0, 0, 0, 0)))
            }
            tokens
        } catch (e: Exception) {
            Log.e("EditorState", "Error parsing highlight result", e)
            emptyList()
        }
    }
    
    companion object {
        private const val TAG = "EditorState"
    }
}
