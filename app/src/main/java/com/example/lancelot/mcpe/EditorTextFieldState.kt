package com.example.lancelot.mcpe

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import com.example.lancelot.config.ConfigManager
import com.example.lancelot.config.LanguageQueries
import com.example.lancelot.rust.RustBridge
import com.example.lancelot.rust.Token
import com.example.lancelot.ui.theme.DefaultAppTheme
import kotlinx.coroutines.*
import org.json.JSONArray
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

@Stable
class EditorTextFieldState(
    internal val textState: TextState,
    private var tokens: ArrayList<Token> = ArrayList(),
    private val languageName: String = "cpp"
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isDisposed = false
    private var lastParsedText = ""
    private val tokenCache = mutableMapOf<Int, List<Token>>()
    private var queries: LanguageQueries? = null
    private var parsingJob: Job? = null
      init {
        scope.launch {
            queries = getLanguageQueries(languageName)
        }
    }
    
    private suspend fun getLanguageQueries(language: String): com.example.lancelot.config.LanguageQueries? {
        return withContext(Dispatchers.IO) {
            org.koin.java.KoinJavaComponent.getKoin()
                .get<ConfigManager>()
                .readLanguageQueries(language)
        }
    }
      private fun parseAndHighlight(text: String) {
        if (isDisposed) return
        
        try {
            // Si no tenemos queries para el lenguaje, no podemos resaltar
            val currentQueries = queries ?: return
            
            // Limpiar cache cuando el texto cambia
            if (text != lastParsedText) {
                tokenCache.clear()
            }
            
            // Obtener los tokens usando las queries y un nuevo formato JSON para los highlight names
            val highlightNamesJson = """[
                "keyword", "function", "type", "string", "number", 
                "comment", "constant", "variable", "operator", "property"
            ]""".trimIndent()
            
            // El parámetro t es para el tema, por ahora usaremos un tema básico
            val themeJson = """{ 
                "keyword": "#0000FF",
                "function": "#795E26",
                "type": "#267F99",
                "string": "#A31515",
                "number": "#098658",
                "comment": "#008000",
                "constant": "#0070C1",
                "variable": "#001080",
                "operator": "#000000",
                "property": "#001080"
            }""".trimIndent()
            
            val highlightResult = RustBridge.highlight(
                text,
                languageName,
                currentQueries.highlights,
                currentQueries.injections,
                currentQueries.locals,
                themeJson,
                highlightNamesJson
            )

            val newTokens = parseHighlightResult(highlightResult)
            tokens.clear()
            tokens.addAll(newTokens)
            lastParsedText = text
            
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("EditorTextFieldState", "Error in parseAndHighlight", e)
            tokens.clear()
            tokenCache.clear()
        }
    }

    private fun parseHighlightResult(json: String): List<Token> {
        return try {
            val resultObj = JSONObject(json)
            Log.d("EditorTextFieldState", "Highlight result: $json")
            val events = resultObj.getJSONArray("events")
            val tokens = ArrayList<Token>()

            for (i in 0 until events.length()) {
                val event = events.get(i)

                if (event is JSONObject) {
                    when {
                        event.has("Source") -> {
                            val obj = event.getJSONObject("Source")
                            tokens.add(
                                Token(
                                    kind = "source",
                                    nodeType = "source",
                                    positions = intArrayOf(
                                        obj.getInt("start"),
                                        obj.getInt("end"),
                                        0, 0, 0, 0 // Opcional si Tree-sitter no devuelve líneas/columnas
                                    )
                                )
                            )
                        }
                        event.has("Highlight") -> {
                            val obj = event.getJSONObject("Highlight")
                            tokens.add(
                                Token(
                                    kind = obj.getString("kind"),
                                    nodeType = "highlight",
                                    positions = intArrayOf(
                                        obj.getInt("start"),
                                        obj.getInt("end"),
                                        0, 0, 0, 0
                                    )
                                )
                            )
                        }
                        // Agrega más tipos de eventos si necesitas
                    }
                }
            }

            tokens
        } catch (e: Exception) {
            Log.e("EditorTextFieldState", "Error parsing highlight result: $e")
            emptyList()
        }
    }

    // Actualizar el AnnotatedString cuando cambian los tokens
    private val annotatedString by derivedStateOf {
        buildAnnotatedString {
            val text = textState.text
            if (text.isEmpty()) return@buildAnnotatedString
            
            var lastIndex = 0
            for (token in tokens) {
                // Texto sin resaltar antes del token
                if (token.startColumn > lastIndex) {
                    withStyle(DefaultAppTheme.code.simple) {
                        append(text.substring(lastIndex, token.startColumn))
                    }
                }
                
                // Texto resaltado del token
                withStyle(getStyleForToken(token.kind)) {
                    append(text.substring(token.startColumn, token.endColumn))
                }
                
                lastIndex = token.endColumn
            }
            
            // Texto restante sin resaltar
            if (lastIndex < text.length) {
                withStyle(DefaultAppTheme.code.simple) {
                    append(text.substring(lastIndex))
                }
            }
        }
    }
    
    private fun getStyleForToken(type: String): SpanStyle = when(type.lowercase()) {
        "keyword" -> DefaultAppTheme.code.keyword
        "function" -> DefaultAppTheme.code.keyword
        "type" -> DefaultAppTheme.code.value
        "string" -> DefaultAppTheme.code.annotation
        "number" -> DefaultAppTheme.code.value
        "comment" -> DefaultAppTheme.code.comment
        "constant" -> DefaultAppTheme.code.reference
        "variable" -> DefaultAppTheme.code.simple
        else -> DefaultAppTheme.code.simple
    }

    private fun AnnotatedString.Builder.appendStyledLine(line: String, tokens: List<Token>) {
        withStyle(DefaultAppTheme.code.simple) {
            append(line)

            // Resaltado de referencias
            textState.highlightedReferenceRanges
                .filter { range -> 
                    val lineStart = getLineStartOffset(line)
                    val lineEnd = lineStart + line.length
                    range.start >= lineStart && range.end <= lineEnd
                }
                .forEach { range ->
                    val lineStart = getLineStartOffset(line)
                    addStyle(
                        DefaultAppTheme.code.reference, 
                        range.start - lineStart, 
                        range.end - lineStart
                    )
                }
        }
    }

    private fun getLineStartOffset(line: String): Int {
        val text = textState.text
        return text.indexOf(line)
    }

    fun onTextFieldValueChange(textFieldValue: TextFieldValue) {
        if (isDisposed) return
        
        val oldText = textState.text
        val newText = textFieldValue.text
        
        textState.text = newText
        _selection = textFieldValue.selection
        textState.caretOffset = if (_selection.collapsed) _selection.start else -1
        textState.selection = _selection
        
        if (shouldParse(oldText, newText)) {
            // Cancelar el trabajo anterior si existe
            parsingJob?.cancel()
            
            // Iniciar nuevo parseo
            parsingJob = scope.launch {
                delay(200) // Pequeño delay para evitar parsear en cada tecla
                parseAndHighlight(newText)
            }
        }
    }
    
    private fun shouldParse(oldText: String, newText: String): Boolean {
        if (newText == lastParsedText) return false
        if (newText.length > 100000) return false
        
        // Parsear si:
        // 1. Es el primer parseo
        // 2. Se completó una palabra (espacio, punto, etc)
        // 3. Se presionó enter
        // 4. Hay un cambio significativo
        return lastParsedText.isEmpty() ||
               newText.endsWith(" ") || 
               newText.endsWith(".") ||
               newText.endsWith(";") ||
               newText.endsWith("\n") ||
               calculateSignificantChange(oldText, newText)
    }
    
    private fun calculateSignificantChange(oldText: String, newText: String): Boolean {
        // Si el texto es corto, cualquier cambio es significativo
        if (newText.length < 20) return true
        
        val lengthDiff = kotlin.math.abs(oldText.length - newText.length)
        
        // Cambios grandes son significativos
        if (lengthDiff > 10) return true
        
        // Comparar contenido char por char para detectar cambios pequeños pero importantes
        val minLength = minOf(oldText.length, newText.length)
        var changes = 0
        
        for (i in 0 until minLength) {
            if (oldText[i] != newText[i]) {
                changes++
                if (changes >= 3) return true // 3 o más caracteres cambiados
            }
        }
        
        return false
    }

    var lineCount by mutableIntStateOf(1)
        private set

    fun onTextLayoutChange(textLayoutResult: TextLayoutResult) {
        lineCount = textLayoutResult.lineCount
        textState.textLayoutResult = textLayoutResult
    }

    private var _selection by mutableStateOf(TextRange.Zero)
    val selection by derivedStateOf {
        if (textState.caretOffset == -1) {
            if (textState.selection != TextRange.Zero) {
                textState.selection
            } else {
                _selection
            }
        } else TextRange(textState.caretOffset)
    }

    val textFieldValue by derivedStateOf { TextFieldValue(annotatedString, selection) }



    private fun handleAutoIndent() {
        val currentLine = getCurrentLine()
        val indentLevel = calculateIndentLevel(currentLine)
        val indent = " ".repeat(indentLevel * 3)
        
        val position = textState.caretOffset
        textState.text = textState.text.replaceRange(position, position, indent)
        textState.caretOffset = position + indent.length
        _selection = TextRange(position + indent.length)
    }

    private fun getCurrentLine(): String {
        val text = textState.text
        val position = textState.caretOffset
        val lineStart = text.lastIndexOf('\n', position - 2).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', position).let { if (it == -1) text.length else it }
        return text.substring(lineStart, lineEnd)
    }

    private fun calculateIndentLevel(line: String): Int {
        var level = getLineIndentLevel(line)
        
        if (line.trim().endsWith("{")) {
            level++
        }
        
        return level
    }

    private fun getLineIndentLevel(line: String): Int {
        var spaces = 0
        for (char in line) {
            if (char == ' ') spaces++
            else break
        }
        return spaces / 4
    }
}