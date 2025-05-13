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
import com.example.lancelot.rust.RustBridge
import com.example.lancelot.rust.Token
import com.example.lancelot.ui.theme.DefaultAppTheme

@Stable
class EditorTextFieldState(
    internal val textState: TextState,
    private var treePointer: Long = 0L,
    private var tokens: ArrayList<Token> = ArrayList()
) {
    private val annotatedString by derivedStateOf {
        try {
            styleString(textState.text, tokens, textState.highlightedReferenceRanges)
        } catch (e: Exception) {
            Log.e("EditorTextFieldState", "Error in syntax highlighting", e)
            // Fallback to regex-based highlighting
            styleStringRegex(textState.text, textState.highlightedReferenceRanges)
        }
    }

    fun updateTokens(newTokens: ArrayList<Token>) {
        tokens = newTokens
    }

    fun updateTreePointer(newPointer: Long) {
        treePointer = newPointer
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

    var lineCount by mutableIntStateOf(1)
        private set

    fun onTextFieldValueChange(textFieldValue: TextFieldValue) {
        val oldText = textState.text
        val newText = textFieldValue.text
        val wasEnterPressed = newText.length > oldText.length && 
                            textFieldValue.selection.end > 0 &&
                            newText[textFieldValue.selection.end - 1] == '\n'
        
        textState.text = textFieldValue.text
        _selection = textFieldValue.selection
        textState.caretOffset =
            if (_selection.collapsed) _selection.start
            else -1
        textState.selection = _selection

        try {
            processCodeIncrementally(newText)
        } catch (e: Exception) {
            Log.e("EditorTextFieldState", "Error processing code", e)
            // Reset state on error
            treePointer = 0L
            tokens.clear()
        }

        if (wasEnterPressed) {
            handleAutoIndent()
        }
    }

    private fun processCodeIncrementally(newText: String) {
        try {
            // Reset state if the tree pointer is invalid
            if (treePointer < 0) {
                treePointer = 0L
            }

            // Only attempt to parse if the text has actually changed
            if (newText.length > 100000) {
                Log.w("EditorTextFieldState", "Text too long for parsing, falling back to regex highlighting")
                treePointer = 0L
                tokens.clear()
                return
            }

            // Use coroutine with timeout
            val timeout = System.currentTimeMillis() + 1000 // 1 second timeout
            val newTreePointer = try {
                RustBridge.parseIncremental(
                    code = newText,
                    languageName = "cpp",
                    tree_ptr = treePointer
                ).also {
                    if (System.currentTimeMillis() > timeout) {
                        throw RuntimeException("Parsing timeout exceeded")
                    }
                }
            } catch (e: Exception) {
                Log.e("EditorTextFieldState", "Error in parsing", e)
                0L
            }
            
            // If parsing succeeded, update the tree pointer
            if (newTreePointer > 0) {
                treePointer = newTreePointer
                
                // Only tokenize if we have a valid tree and haven't exceeded timeout
                if (System.currentTimeMillis() <= timeout) {
                    try {
                        val newTokens = RustBridge.tokenizeCode(newText, "cpp", treePointer)
                        if (newTokens.isNotEmpty()) {
                            tokens.clear()
                            tokens.addAll(newTokens)
                        }
                    } catch (e: Exception) {
                        Log.e("EditorTextFieldState", "Error in tokenization", e)
                        tokens.clear()
                    }
                }
            } else {
                Log.w("EditorTextFieldState", "Failed to parse code, tree pointer is invalid")
                treePointer = 0L
                tokens.clear()
            }
        } catch (e: Exception) {
            Log.e("EditorTextFieldState", "Error in incremental parsing", e)
            treePointer = 0L
            tokens.clear()
        }
    }

    // Cleanup method to be called when the editor is disposed
    fun cleanup() {
        if (treePointer > 0) {
            try {
                RustBridge.freeTree(treePointer)
                treePointer = 0L
            } catch (e: Exception) {
                Log.e("EditorTextFieldState", "Error cleaning up tree", e)
            }
        }
    }

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

    fun onTextLayoutChange(textLayoutResult: TextLayoutResult) {
        lineCount = textLayoutResult.lineCount
        textState.textLayoutResult = textLayoutResult
    }

    private val tokenStyleMap = mapOf(
        "preprocessor.include" to DefaultAppTheme.code.keyword,
        "string.system" to DefaultAppTheme.code.keyword,
        "string.library" to DefaultAppTheme.code.keyword,
        "string.include" to DefaultAppTheme.code.keyword,
        "keyword.type" to DefaultAppTheme.code.keyword,
        "keyword.modifier" to DefaultAppTheme.code.keyword,
        "variable.member" to DefaultAppTheme.code.keyword,
        "variable.parameter" to DefaultAppTheme.code.keyword,
        "function.method" to DefaultAppTheme.code.reference,
        "type.definition" to DefaultAppTheme.code.keyword,
        "type.builtin" to DefaultAppTheme.code.keyword,
        "module" to DefaultAppTheme.code.keyword,
        "constant" to DefaultAppTheme.code.value,
        "function" to DefaultAppTheme.code.keyword,
        "function.call" to DefaultAppTheme.code.keyword,
        "constructor" to DefaultAppTheme.code.keyword,
        "variable.builtin" to DefaultAppTheme.code.keyword,
        "constant.builtin" to DefaultAppTheme.code.keyword,
        "boolean" to DefaultAppTheme.code.keyword,
        "string" to DefaultAppTheme.code.value,
        "keyword.exception" to DefaultAppTheme.code.keyword,
        "keyword" to DefaultAppTheme.code.keyword,
        "keyword.coroutine" to DefaultAppTheme.code.keyword,
        "keyword.operator" to DefaultAppTheme.code.keyword,
        "operator" to DefaultAppTheme.code.punctuation,
        "punctuation.delimiter" to DefaultAppTheme.code.keyword,
        "punctuation.bracket" to DefaultAppTheme.code.keyword,
        "function.definition" to DefaultAppTheme.code.keyword,
        "type.primitive" to DefaultAppTheme.code.keyword,
        "function.declarator" to DefaultAppTheme.code.keyword,
        "function.body" to DefaultAppTheme.code.keyword,
        "function.name" to DefaultAppTheme.code.keyword,
        "function.parameters" to DefaultAppTheme.code.simple
    )

    private fun styleString(
        str: String,
        tokens: List<Token>,
        highlightedReferenceRanges: List<TextRange>
    ) = buildAnnotatedString {
        withStyle(DefaultAppTheme.code.simple) {
            val strFormatted = str.replace("\t", " ")
            append(strFormatted)

            // Aplicar estilos basados en tokens
            tokens.forEach { token ->
                tokenStyleMap[token.kind]?.let { style ->
                    addStyle(
                        style,
                        token.startColumn,
                        token.endColumn
                    )
                }
            }

            // Resaltado de referencias
            for (range in highlightedReferenceRanges) {
                addStyle(DefaultAppTheme.code.reference, range.start, range.end)
            }
        }
    }

    private fun styleStringRegex(str: String, highlightedReferenceRanges: List<TextRange>) = buildAnnotatedString {
        withStyle(DefaultAppTheme.code.simple) {
            val strFormatted = str.replace("\t", " ")
            append(strFormatted)
            addStyle(DefaultAppTheme.code.comment, strFormatted, RegExps.comment)
            addStyle(DefaultAppTheme.code.punctuation, strFormatted, RegExps.punctuation)
            addStyle(DefaultAppTheme.code.keyword, strFormatted, RegExps.keyword)
            addStyle(DefaultAppTheme.code.value, strFormatted, RegExps.value)
            addStyle(DefaultAppTheme.code.annotation, strFormatted, RegExps.annotation)
            for (range in highlightedReferenceRanges) {
                addStyle(DefaultAppTheme.code.reference, range.start, range.end)
            }
        }
    }

    private fun AnnotatedString.Builder.addStyle(
        style: SpanStyle,
        start: Int,
        end: Int
    ) {
        if (start >= 0 && end <= length && start < end) {
            addStyle(style, start, end)
        }
    }

    private object RegExps {
        val keyword = Regex("\\b(" +
                "abstract|actual|as|as\\?|break|by|catch|class|const|constructor|continue|do|else|" +
                "expect|finally|for|fun|if|import|in|!in|interface|internal|is|!is|null|object|" +
                "operator|override|package|private|protected|public|return|super|this|throw|try|" +
                "typealias|typeof|val|var|when|while)\\b")
        val punctuation = Regex("[:=\"\\[\\]{}(),]")
        val value = Regex("\\b(true|false|[0-9]+)\\b")
        val annotation = Regex("\\b@[a-zA-Z_]+\\b")
        val comment = Regex("//.*")
    }

    private fun AnnotatedString.Builder.addStyle(style: SpanStyle, text: String, regexp: Regex) {
        for (result in regexp.findAll(text)) {
            addStyle(style, result.range.first, result.range.last + 1)
        }
    }
}