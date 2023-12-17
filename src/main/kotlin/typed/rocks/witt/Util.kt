package typed.rocks.witt

import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.lang.javascript.TypeScriptJSXFileType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

var isTest = false

fun PsiFile.isTsFile() = this.fileType is TypeScriptFileType || this.fileType is TypeScriptJSXFileType

const val TYPE_POINTER_STRING = "^?"

@Suppress("UseJBColor")
val regularColor = Color(35, 90, 151)

@Suppress("UseJBColor")
val darkColor = Color(49, 120, 198)

val COMMENT_ATTRIBUTE = TextAttributes()
    .also {
        it.fontType = Font.BOLD
        it.foregroundColor = JBColor(regularColor, darkColor)
    }


fun PsiElement.getTypeAboveComment(
    document: Document,
    comment: PsiComment,
    commentLineIndex: Int
): PsiElement? {
    val commentStartOffsetColumn = comment.startOffsetInParent - document.getLineStartOffset(commentLineIndex)
    val arrowUpColumn = commentStartOffsetColumn + comment.text.indexOf(TYPE_POINTER_STRING)
    val typeLineIndex = commentLineIndex - 1
    return this.containingFile.getElementAtLineAndColumn(document, typeLineIndex, arrowUpColumn)
}

fun PsiFile.getElementAtLineAndColumn(document: Document, line: Int, column: Int): PsiElement? {
    val lineStartOffset = document.getLineStartOffset(line)
    val offset = lineStartOffset + column
    return if (offset < 0) null else this.findElementAt(offset)
}

fun Editor.addHighlight(comment: PsiComment): RangeHighlighter {
    return this.markupModel.addRangeHighlighter(
        comment.startOffsetInParent + comment.text.indexOf(TYPE_POINTER_STRING), comment.endOffset,
        HighlighterLayer.ADDITIONAL_SYNTAX,
        COMMENT_ATTRIBUTE,
        HighlighterTargetArea.EXACT_RANGE
    )
}

fun Editor.getCharacterMax(): Int {
    val font = colorsScheme.getFont(EditorFontType.PLAIN)
    val fontMetrics = component.getFontMetrics(font)
    val charWidth = fontMetrics.charWidth('m')
    val editorWidth = component.width
    return if (editorWidth == 0) 100 else editorWidth / charWidth
}

fun String.trimmedText(maxCharacters: Int): String {
    val singleSpaces = this.replace(Regex("\\n"), " ").replace("\\{( {2,})".toRegex(), "\\{ ")
    return if (maxCharacters > singleSpaces.length) singleSpaces else singleSpaces.substring(
        0,
        maxCharacters - 3
    ) + "..."
}

fun <T> CompletableFuture<T?>.nullOnTimeout(ms: Long): CompletableFuture<T?> =
    if (isTest) this else this.completeOnTimeout(null, ms, TimeUnit.MILLISECONDS)

fun <T> CompletableFuture<List<T>?>.emptyOnTimeout(ms: Long): CompletableFuture<List<T>?> =
    if (isTest) this else this.completeOnTimeout(listOf(), ms, TimeUnit.MILLISECONDS)
