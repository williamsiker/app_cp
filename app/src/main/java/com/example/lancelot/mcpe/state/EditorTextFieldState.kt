package com.example.lancelot.mcpe.state

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
import com.example.lancelot.AppTheme
import com.example.lancelot.mcpe.model.TextState

@Stable
class EditorTextFieldState(
    private val textState: TextState
) {
    private val annotatedString by derivedStateOf { styleString(textState.text, textState.highlightedReferenceRanges) }

    private var _selection by mutableStateOf(TextRange.Zero)
    private val selection by derivedStateOf {
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

        if (wasEnterPressed) {
            handleAutoIndent()
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

    private fun styleString(str: String, highlightedReferenceRanges: List<TextRange>) = buildAnnotatedString {
        withStyle(AppTheme.code.simple) {
            val strFormatted = str.replace("\t", " ")
            append(strFormatted)
            addStyle(AppTheme.code.comment, strFormatted, RegExps.comment)
            addStyle(AppTheme.code.punctuation, strFormatted, RegExps.punctuation)
            addStyle(AppTheme.code.keyword, strFormatted, RegExps.keyword)
            addStyle(AppTheme.code.value, strFormatted, RegExps.value)
            addStyle(AppTheme.code.annotation, strFormatted, RegExps.annotation)
            for (range in highlightedReferenceRanges) {
                addStyle(AppTheme.code.reference, range.start, range.end)
            }
        }
    }

    private object RegExps {
        val keyword = Regex("\\b(" +
                "abstract" +
                "|actual" +
                "|as" +
                "|as\\?" +
                "|break" +
                "|by" +
                "|catch" +
                "|class" +
                "|const" +
                "|constructor" +
                "|continue" +
                "|do" +
                "|else" +
                "|expect" +
                "|finally" +
                "|for" +
                "|fun" +
                "|if" +
                "|import" +
                "|in" +
                "|!in" +
                "|interface" +
                "|internal" +
                "|is" +
                "|!is" +
                "|null" +
                "|object" +
                "|operator" +
                "|override" +
                "|package" +
                "|private" +
                "|protected" +
                "|public" +
                "|return" +
                "|super" +
                "|this" +
                "|throw" +
                "|try" +
                "|typealias" +
                "|typeof" +
                "|val" +
                "|var" +
                "|when" +
                "|while" +
                ")\\b")
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