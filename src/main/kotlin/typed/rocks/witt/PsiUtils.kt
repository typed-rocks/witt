package typed.rocks.witt

import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.lang.javascript.TypeScriptJSXFileType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset

fun PsiElement.getElementAboveComment(
    document: Document, comment: PsiComment, commentLineIndex: Int
): PsiElement? {
    val commentStartOffsetColumn = comment.startOffset - document.getLineStartOffset(commentLineIndex)
    val arrowUpColumn = commentStartOffsetColumn + comment.text.indexOf(TYPE_POINTER_STRING)
    val typeLineIndex = commentLineIndex - 1
    return this.containingFile.getElementAtLineAndColumn(document, typeLineIndex, arrowUpColumn)
}

fun PsiFile.getElementAtLineAndColumn(document: Document, line: Int, column: Int): PsiElement? {
    val lineStartOffset = document.getLineStartOffset(line)
    val offset = lineStartOffset + column
    return if (offset < 0) null else this.findElementAt(offset)
}

fun Editor.addHighlight(comment: PsiComment): RangeHighlighter = this.markupModel.addRangeHighlighter(
    comment.startOffset + comment.text.indexOf(TYPE_POINTER_STRING),
    comment.endOffset,
    HighlighterLayer.ADDITIONAL_SYNTAX,
    COMMENT_ATTRIBUTE,
    HighlighterTargetArea.EXACT_RANGE
)

fun Editor.getCharacterMax(): Int {
    val font = colorsScheme.getFont(EditorFontType.PLAIN)
    val fontMetrics = component.getFontMetrics(font)
    val charWidth = fontMetrics.charWidth('m')
    val editorWidth = component.width
    return if (UNDER_TEST) 100 else editorWidth / charWidth
}

fun PsiFile.isTsFile() = listOf("mts", "ts", "tsx").contains(this.fileType.defaultExtension)
