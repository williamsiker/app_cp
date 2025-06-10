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
import com.example.lancelot.snippets.Snippet
import com.example.lancelot.ui.theme.DefaultAppTheme
import com.example.lancelot.theme.ThemeManager
import kotlinx.coroutines.*
import org.json.JSONArray
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

@Stable
class EditorTextFieldState(
    internal val textState: TextState,
    private var tokens: ArrayList<Token> = ArrayList(),
    val languageName: String = "cpp"
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isDisposed = false
    private var lastParsedText = ""
    private val tokenCache = mutableMapOf<Int, List<Token>>()
    private var queries: LanguageQueries? = null
    private var parsingJob: Job? = null
    private var lastHighlightVersion = 0L
    private var cachedHighlightRanges = mutableListOf<Token>()
    
    init {
        scope.launch {
            queries = getLanguageQueries(languageName)
        }
    }
    
    private suspend fun getLanguageQueries(language: String): LanguageQueries? {
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
            
            val ranges = resultObj.optJSONArray("ranges")!!
            val highlightNames = resultObj.getJSONArray("highlight_names")
            val version = resultObj.optLong("version", 0)
            val changedRanges = resultObj.optJSONArray("changed_ranges")
            val reusedRanges = resultObj.optJSONArray("reused_ranges")
            
            // Si tenemos un cambio incremental válido, actualizar solo las regiones necesarias
            if (version > 0 && version == lastHighlightVersion + 1 && changedRanges != null) {
                return updateIncrementalHighlights(
                    ranges,
                    highlightNames,
                    changedRanges,
                    reusedRanges
                )
            }
            
            // Si no podemos hacer actualización incremental, hacer un parseo completo
            val tokens = ArrayList<Token>()
            
            for (i in 0 until ranges.length()) {
                val range = ranges.getJSONObject(i)
                val start = range.getInt("start")
                val end = range.getInt("end")
                val type = range.getInt("highlight_type")
                
                if (start < end && end <= textState.text.length) {
                    val highlightType = if (type < highlightNames.length()) {
                        highlightNames.getString(type)
                    } else "text"
                    
                    tokens.add(Token(
                        kind = highlightType,
                        nodeType = highlightType,
                        positions = intArrayOf(
                            start, end,
                            0, start,
                            0, end
                        )
                    ))
                }
            }
            
            // Actualizar posiciones de línea para todos los tokens
            updateLinePositions(tokens, textState.text)
            
            if (version > 0) {
                lastHighlightVersion = version
                cachedHighlightRanges = tokens.toMutableList()
            }
            
            tokens
            
        } catch (e: Exception) {
            Log.e("EditorTextFieldState", "Error parsing highlight result: $e")
            emptyList()
        }
    }

    private fun updateIncrementalHighlights(
        ranges: JSONArray,
        highlightNames: JSONArray,
        changedRanges: JSONArray,
        reusedRanges: JSONArray?
    ): List<Token> {
        val tokens = cachedHighlightRanges.toMutableList()
        
        for (i in 0 until changedRanges.length()) {
            val range = changedRanges.getJSONArray(i)
            val start = range.getInt(0)
            val end = range.getInt(1)
            
            tokens.removeAll { token ->
                token.startByte >= start && token.endByte <= end
            }
        }
        
        for (i in 0 until ranges.length()) {
            val range = ranges.getJSONObject(i)
            val start = range.getInt("start")
            val end = range.getInt("end")
            val type = range.getInt("highlight_type")
            
            if (start < end && end <= textState.text.length) {
                val highlightType = if (type < highlightNames.length()) {
                    highlightNames.getString(type)
                } else "text"
                
                tokens.add(Token(
                    kind = highlightType,
                    nodeType = highlightType,
                    positions = intArrayOf(
                        start, end,
                        0, start,
                        0, end
                    )
                ))
            }
        }
        
        // Ordenar tokens por posición de inicio
        tokens.sortBy { it.startByte }
        
        // Actualizar posiciones de línea
        updateLinePositions(tokens, textState.text)
        
        cachedHighlightRanges = tokens.toMutableList()
        return tokens
    }

    private fun parseRangesFormat(resultObj: JSONObject): List<Token> {
        val ranges = resultObj.getJSONArray("ranges")
        val highlightNames = resultObj.getJSONArray("highlight_names")
        val version = resultObj.optLong("version", 0)
        val tokens = ArrayList<Token>()
        
        for (i in 0 until ranges.length()) {
            val range = ranges.getJSONObject(i)
            val start = range.getInt("start")
            val end = range.getInt("end") 
            val type = range.getInt("highlight_type")
            
            if (start < end && end <= textState.text.length) {
                val highlightType = if (type < highlightNames.length()) {
                    highlightNames.getString(type)
                } else "text"
                
                tokens.add(Token(
                    kind = highlightType,
                    nodeType = highlightType,
                    positions = intArrayOf(
                        start, end,  // byte range
                        0, start,    // startRow, startCol (temporal)
                        0, end       // endRow, endCol (temporal) 
                    )
                ))
            }
        }
        
        updateLinePositions(tokens, textState.text)
        
        if (version > 0) {
            lastHighlightVersion = version
            cachedHighlightRanges = tokens.toMutableList()
        }
        
        return tokens
    }
    
    private fun updateIncrementalHighlights(
        changedRanges: JSONArray,
        events: JSONArray,
        highlightNames: JSONArray
    ): List<Token> {
        val tokens = cachedHighlightRanges.toMutableList()
        
        // Remove tokens in changed regions
        for (i in 0 until changedRanges.length()) {
            val range = changedRanges.getJSONArray(i)
            val start = range.getInt(0)
            val end = range.getInt(1)
            
            tokens.removeAll { token ->
                token.startByte >= start && token.endByte <= end
            }
        }
        
        // Add new tokens for changed regions
        var currentType: String? = null
        var currentStart = -1
        
        for (i in 0 until events.length()) {
            val event = events.get(i)
            if (event is JSONObject) {
                when {
                    event.has("Source") -> {
                        val source = event.getJSONObject("Source")
                        val start = source.getInt("start")
                        val end = source.getInt("end")
                        
                        if (start < end && end <= textState.text.length) {
                            if (currentType != null) {
                                tokens.add(Token(
                                    kind = currentType,
                                    nodeType = currentType,
                                    positions = intArrayOf(
                                        start, end,
                                        0, start,
                                        0, end
                                    )
                                ))
                            }
                        }
                    }
                    event.has("Start") -> {
                        val start = event.getJSONObject("Start")
                        val index = start.getInt("index")
                        currentType = if (index < highlightNames.length()) {
                            highlightNames.getString(index)
                        } else "text"
                    }
                    event.has("End") -> {
                        currentType = null
                    }
                }
            }
        }
        
        // Sort tokens by start position
        tokens.sortBy { it.startByte }
        
        // Update line positions
        updateLinePositions(tokens, textState.text)
        
        cachedHighlightRanges = tokens.toMutableList()
        return tokens
    }

    private fun parseFullHighlights(events: JSONArray, highlightNames: JSONArray): List<Token> {
        val tokens = ArrayList<Token>()
        
        var currentType: String? = null

        for (i in 0 until events.length()) {
            val event = events.get(i)
            if (event is JSONObject) {
                when {
                    event.has("Source") -> {                            val source = event.getJSONObject("Source")
                        val start = source.getInt("start")
                        val end = source.getInt("end")
                        
                        // Validar rangos antes de crear el token
                        if (start < end && end <= textState.text.length) {
                            // Si hay un tipo activo, crear un token
                            if (currentType != null) {
                                tokens.add(Token(
                                    kind = currentType,
                                    nodeType = currentType,
                                    positions = intArrayOf(
                                        start, end,  // byte range
                                        0, start,    // startRow, startCol (temporal)
                                        0, end       // endRow, endCol (temporal)
                                    )
                                ))
                            }
                        } else {
                            Log.w("EditorTextFieldState", "Invalid token range: start=$start, end=$end, textLength=${textState.text.length}")
                        }
                    }
                    event.has("Start") -> {
                        val start = event.getJSONObject("Start")
                        val index = start.getInt("index")
                        currentType = if (index < highlightNames.length()) {
                            highlightNames.getString(index)
                        } else "text"
                    }
                    event.has("End") -> {
                        currentType = null
                    }
                }
            }
        }
        
        // Actualizar las posiciones de línea para cada token
        updateLinePositions(tokens, textState.text)
        
        return tokens
    }
    
    private fun updateLinePositions(tokens: List<Token>, text: String) {
        var currentLine = 0
        var currentCol = 0
        var pos = 0
        
        tokens.forEach { token ->
            // Encontrar la posición de inicio
            while (pos < token.startByte) {
                if (pos < text.length) {
                    if (text[pos] == '\n') {
                        currentLine++
                        currentCol = 0
                    } else {
                        currentCol++
                    }
                }
                pos++
            }
            
            // Guardar posición de inicio
            token.positions[2] = currentLine  // startRow
            token.positions[3] = currentCol   // startCol
            
            // Encontrar la posición final
            while (pos < token.endByte) {
                if (pos < text.length) {
                    if (text[pos] == '\n') {
                        currentLine++
                        currentCol = 0
                    } else {
                        currentCol++
                    }
                }
                pos++
            }
            
            // Guardar posición final
            token.positions[4] = currentLine  // endRow
            token.positions[5] = currentCol   // endCol
        }
    }

    // Actualizar el AnnotatedString cuando cambian los tokens
    private val annotatedString by derivedStateOf {
        buildAnnotatedString {
            val text = textState.text
            var lastIndex = 0
            for (token in tokens) {
                try {
                    // Texto sin resaltar antes del token
                    if (token.startByte > lastIndex && lastIndex < text.length) {
                        val endIndex = minOf(token.startByte, text.length)
                        withStyle(DefaultAppTheme.code.simple) {
                            append(text.substring(lastIndex, endIndex))
                        }
                    }
                    
                    // Texto resaltado del token
                    if (token.startByte < text.length) {
                        val endIndex = minOf(token.endByte, text.length)
                        withStyle(getStyleForToken(token.kind)) {
                            append(text.substring(token.startByte, endIndex))
                        }
                    }
                    
                    lastIndex = minOf(token.endByte, text.length)
                } catch (e: Exception) {
                    Log.e("EditorTextFieldState", "Error processing token: $token, text length: ${text.length}", e)
                    // Skip this token if there's an error
                    continue
                }
            }
            
            // Texto restante sin resaltar
            if (lastIndex < text.length) {
                withStyle(DefaultAppTheme.code.simple) {
                    append(text.substring(lastIndex))
                }
            }
            
            // Aplicar resaltado de referencias seleccionadas
            textState.highlightedReferenceRanges.forEach { range ->
                addStyle(DefaultAppTheme.code.reference, range.start, range.end)
            }
        }
    }
    
    private fun getStyleForToken(type: String): SpanStyle {
        return ThemeManager.styleFor(type.lowercase())
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

    fun currentPrefix(): String {
        val cursor = textFieldValue.selection.start
        val beforeCursor = textFieldValue.text.substring(0, cursor)
        val match = "[A-Za-z_]+$".toRegex().find(beforeCursor)
        return match?.value ?: ""
    }

    fun insertSnippet(snippet: Snippet) {
        val prefix = currentPrefix()
        val cursor = textFieldValue.selection.start
        val start = (cursor - prefix.length).coerceAtLeast(0)
        val newText = textFieldValue.text.replaceRange(start, cursor, snippet.body)
        onTextFieldValueChange(
            textFieldValue.copy(
                text = newText,
                selection = TextRange(start + snippet.body.length)
            )
        )
    }
}