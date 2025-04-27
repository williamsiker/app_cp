package com.example.lancelot.mcpe

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.substring
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset

@Stable
class TextState(
    text: String
) {
    companion object {
        const val INDENT_SIZE = 4
        const val LINE_SEPARATOR = '\n'
    }

    private val indent = " ".repeat(INDENT_SIZE)
    private val pairChars = mapOf(
        '(' to ')',
        '{' to '}',
        '[' to ']',
        '<' to '>',
        '"' to '"',
        '\'' to '\''
    )
    
    private var rope by mutableStateOf(Rope(text.replace("\r\n", LINE_SEPARATOR.toString())))
    val highlightedReferenceRanges = mutableStateListOf<TextRange>()
    var caretOffset by mutableIntStateOf(0)
    var selection by mutableStateOf(TextRange.Zero)
    var textLayoutResult by mutableStateOf<TextLayoutResult?>(null)
    
    var text: String
        get() = rope.toString()
        set(value) {
            rope = Rope(value)
        }

    private val boundingBox by derivedStateOf {
        textLayoutResult?.let { layoutResult ->
            if (layoutResult.layoutInput.text.isNotEmpty())
                layoutResult.getBoundingBox(0)
            else null
        }
    }

    val charWidth by derivedStateOf {
        boundingBox?.width ?: 0f
    }
    val lineHeight by derivedStateOf {
        boundingBox?.height ?: 0f
    }

    private fun isSelected() = caretOffset == -1

    fun insertIndent() {
        if (isSelected()) return
        insert(indent)
    }

    fun insertPair(char: Char) {
        if (isSelected()) return
        val pair = pairChars.getValue(char)
        insert("$char$pair", 1)
    }

    private fun shouldIncreaseIndent(line: String): Boolean {
        val trimmedLine = line.trim()
        return trimmedLine.endsWith("{") || 
               trimmedLine.endsWith("(") ||
               trimmedLine.endsWith("[") ||
               (trimmedLine.contains("class") && !trimmedLine.contains(";")) ||
               (trimmedLine.contains("struct") && !trimmedLine.contains(";")) ||
               trimmedLine.startsWith("if") ||
               trimmedLine.startsWith("else") ||
               trimmedLine.startsWith("for") ||
               trimmedLine.startsWith("while") ||
               trimmedLine.startsWith("do")
    }

    private fun shouldDecreaseIndent(line: String): Boolean {
        val trimmedLine = line.trim()
        return trimmedLine.startsWith("}") || 
               trimmedLine.startsWith(")") ||
               trimmedLine.startsWith("]")
    }

    private fun getCurrentLineIndentLevel(): Int {
        val lineStart = getLineStartOffset()
        var currentIndent = 0
        var i = lineStart
        while (i < text.length && text[i] == ' ') {
            currentIndent++
            i++
        }
        return currentIndent / INDENT_SIZE
    }

    private fun getAutoIndentLevel(): Int {
        val currentLine = getCurrentLine()
        var indentLevel = getCurrentLineIndentLevel()
        
        if (shouldDecreaseIndent(currentLine)) {
            indentLevel = (indentLevel - 1).coerceAtLeast(0)
        }
        
        if (shouldIncreaseIndent(currentLine)) {
            indentLevel++
        }
        
        return indentLevel
    }

    private fun getCurrentLine(): String {
        val lineStart = getLineStartOffset()
        val lineEnd = text.indexOf(LINE_SEPARATOR, caretOffset).let { if (it == -1) text.length else it }
        return rope.substring(lineStart, lineEnd)
    }

    fun newLineWithIndent() {
        if (isSelected()) return
        
        val indentLevel = getAutoIndentLevel()
        val newIndent = " ".repeat(indentLevel * INDENT_SIZE)
        
        text = text.replaceRange(caretOffset, caretOffset, "$LINE_SEPARATOR$newIndent")
        caretOffset += newIndent.length + 1
    }

    fun moveCaretToLineStartWithIndent(): Boolean {
        if (isSelected()) return false
        val lineIdent = getLineIndent(true)
        return if (lineIdent == caretOffset) false
        else {
            caretOffset = lineIdent
            true
        }
    }

    fun getPrefixBeforeCaret(): Prefix {
        val startOffset = getPrefixStartOffset()
        val prefix = text.substring(startOffset, caretOffset)
        val pasteOffset = if (startOffset < text.length && text[startOffset] == '.') startOffset + 1 else startOffset
        val pastePosition = getPositionForOffset(pasteOffset)
        return Prefix(pasteOffset, pastePosition, prefix)
    }

    fun getCaretPosition() = if (caretOffset != -1) getPositionForOffset(caretOffset) else IntOffset.Zero

    fun getCaretRect() = if (caretOffset != -1) getCursorRectForOffset(caretOffset) else Rect.Zero

    private fun getPositionForOffset(offset: Int): IntOffset = getCursorRectForOffset(offset).bottomLeft.round()

    private fun getCursorRectForOffset(offset: Int): Rect = textLayoutResult?.let { layoutResult ->
        if (offset >= layoutResult.layoutInput.text.length) {
            layoutResult.getCursorRect(layoutResult.layoutInput.text.length - 1)
        } else {
            layoutResult.getCursorRect(offset)
        }
    } ?: Rect.Zero

    fun getOffsetForCharacter(lineIndex: Int, characterOffset: Int): Int = textLayoutResult?.let { layoutResult ->
        if (lineIndex >= layoutResult.lineCount)
            layoutResult.getLineStart(layoutResult.lineCount - 1) + characterOffset
        else layoutResult.getLineStart(lineIndex) + characterOffset
    } ?: -1

    fun getOffsetForPosition(cursorPosition: IntOffset): Int {
        return textLayoutResult?.let { layoutResult ->
            if (cursorPosition.x < 0 || cursorPosition.y < 0 || cursorPosition.y > layoutResult.size.height) {
                return -1
            }
            val offsetForPosition = layoutResult.getOffsetForPosition(cursorPosition.toOffset())
            val lineIndex = layoutResult.getLineForOffset(offsetForPosition)
            if (cursorPosition.x > layoutResult.getLineRight(lineIndex)
                || cursorPosition.y > layoutResult.getLineBottom(lineIndex)
                || cursorPosition.y < layoutResult.getLineTop(lineIndex)
            ) {
                return -1
            }
            return offsetForPosition
        } ?: -1
    }

    fun getElementTextRange(offset: Int): TextRange {
        if (offset == -1) return TextRange.Zero
        var startOffset = adjustOffset(
            if (offset >= text.length) text.length - 1 else offset
        )
        var endOffset = startOffset
        var ch = text[startOffset]
        when {
            ch.isElementBoundary() -> return TextRange.Zero

            ch.isJavaIdentifierPart() -> {
                while (startOffset > 0 && text[startOffset - 1].isJavaIdentifierPart()) startOffset--
                while (endOffset < text.length && text[endOffset + 1].isJavaIdentifierPart()) endOffset++
            }

            else -> {
                while (startOffset > 0) {
                    ch = text[startOffset - 1]
                    if (ch.isElementBoundary() || ch.isJavaIdentifierPart()) break
                    startOffset--
                }
                while (endOffset < text.length - 1) {
                    ch = text[endOffset + 1]
                    if (ch.isElementBoundary() || ch.isJavaIdentifierPart()) break
                    endOffset++
                }
            }
        }
        return TextRange(startOffset, endOffset + 1)
    }

    private fun clearReferences() {
        if (highlightedReferenceRanges.isNotEmpty()) highlightedReferenceRanges.clear()
    }

    private fun adjustOffset(offset: Int): Int {
        var correctedOffset = offset
        if (!text[correctedOffset].isJavaIdentifierPart()) {
            correctedOffset--
        }
        if (correctedOffset >= 0) {
            val ch = text[correctedOffset]
            if (ch == '\'' || ch == '"' || ch == ')' || ch == ']' || ch.isJavaIdentifierPart()) {
                return correctedOffset
            }
        }
        return offset
    }

    fun highlightReferences(textRange: TextRange) {
        clearReferences()
        when {
            textRange.length == 1 -> {
                highlightedReferenceRanges.add(textRange)
                highlightPairBrackets(textRange, text[textRange.start])
            }

            textRange.length > 1 -> {
                if (text[textRange.start].isJavaIdentifierPart()) {
                    highlightedReferenceRanges.add(textRange)
                } else {
                    for (i in textRange.start until textRange.end) highlightedReferenceRanges.add(TextRange(i, i + 1))
                }
            }
        }
    }

    private fun highlightPairBrackets(textRange: TextRange, rightBracket: Char) {
        val leftBracket = when (rightBracket) {
            ')' -> '('
            ']' -> '['
            else -> return
        }
        var offset = textRange.start - 1
        var bracketsCount = 0
        while (offset > 0) {
            when (text[offset]) {
                rightBracket -> bracketsCount++
                leftBracket -> {
                    if (bracketsCount == 0) break
                    bracketsCount--
                }
            }
            offset--
        }
        if (offset >= 0) highlightedReferenceRanges.add(TextRange(offset, offset + 1))
    }

    fun getLineRight(lineIndex: Int): Float = textLayoutResult?.let { layoutResult ->
        if (lineIndex >= layoutResult.lineCount)
            layoutResult.getLineRight(layoutResult.lineCount - 1)
        else layoutResult.getLineRight(lineIndex)
    } ?: 0f

    fun getLineTop(lineIndex: Int): Float = textLayoutResult?.let { layoutResult ->
        if (lineIndex >= layoutResult.lineCount)
            layoutResult.getLineTop(layoutResult.lineCount - 1)
        else layoutResult.getLineTop(lineIndex)
    } ?: 0f

    fun getLineBottom(lineIndex: Int): Float = textLayoutResult?.let { layoutResult ->
        if (lineIndex >= layoutResult.lineCount)
            layoutResult.getLineBottom(layoutResult.lineCount - 1)
        else layoutResult.getLineBottom(lineIndex)
    } ?: 0f

    fun getLineStart(lineIndex: Int): Int = textLayoutResult?.let { layoutResult ->
        if (lineIndex >= layoutResult.lineCount)
            layoutResult.getLineStart(layoutResult.lineCount - 1)
        else layoutResult.getLineStart(lineIndex)
    } ?: 0

    fun getLineForOffset(offset: Int): Int = textLayoutResult?.let { layoutResult ->
        if (offset >= layoutResult.layoutInput.text.length)
            layoutResult.getLineForOffset(layoutResult.layoutInput.text.length - 1)
        else
            layoutResult.getLineForOffset(offset)
    } ?: 0

    fun getTextRangesOf(str: String): List<TextRange> {
        if (str.isEmpty()) return emptyList()
        val text = rope.toString() // For search operations, convert to string
        var i = 0
        val list = mutableListOf<TextRange>()
        while (true) {
            i = text.indexOf(str, i, true)
            if (i == -1) break
            list.add(TextRange(i, i + str.length))
            i += str.length
        }
        return list
    }

    fun getSelectedText(): String = text.substring(selection)

    fun selectTextRange(textRange: TextRange) {
        selection = textRange
        caretOffset = if (textRange.collapsed) {
            selection.start
        } else {
            -1
        }
    }

    fun unselect() {
        selection = TextRange(selection.end)
        caretOffset = selection.end
    }

    private fun insert(char: Char, offsetShift: Int = 0) = insert(char.toString(), offsetShift)

    private fun insert(str: String, offsetShift: Int = 0) {
        rope = rope.insert(caretOffset, str)
        caretOffset += str.length - offsetShift
    }

    private fun getLineStartOffset(): Int {
        if (caretOffset == 0) return 0
        var i = caretOffset - 1
        while (i >= 0) {
            if (rope.charAt(i) == LINE_SEPARATOR) return i + 1
            i--
        }
        return 0
    }

    private fun getLineIndent(absolute: Boolean = false, beforeCaret: Boolean = false): Int {
        val lineOffset = getLineStartOffset()
        var lineIndent = lineOffset
        while (lineIndent < text.length && text[lineIndent] == ' ' && (!beforeCaret || lineIndent < caretOffset)) {
            lineIndent++
        }
        return if (absolute) lineIndent else lineIndent - lineOffset
    }

    private fun getPrefixStartOffset(): Int {
        var wordStart = caretOffset
        while (wordStart > 0 && text[wordStart - 1].isJavaIdentifierPart()) {
            wordStart--
        }
        if (wordStart - 1 > 0 && text[wordStart - 1] == '.') wordStart--
        return wordStart
    }
}

private fun Char.isElementBoundary() = this == '.' || this == ';' || isWhitespace()

data class Prefix(
    val offset: Int,
    val position: IntOffset,
    val prefix: String
)