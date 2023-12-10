package typed.rocks.witt

import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.lang.typescript.compiler.TypeScriptService
import com.intellij.lang.typescript.compiler.languageService.protocol.commands.response.TypeScriptQuickInfoResponse
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font


fun PsiFile.isTsFile() = this.fileType is TypeScriptFileType

const val TYPE_POINTER_STRING = "^?"

val COMMENT_ATTRIBUTE = TextAttributes()
    .also {
        it.fontType = Font.BOLD
        it.foregroundColor = JBColor(Color(35, 90, 151), Color(49, 120, 198)) }


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

fun Editor.addHighlight(comment: PsiComment) {
    this.markupModel.addRangeHighlighter(
        comment.startOffsetInParent + comment.text.indexOf(TYPE_POINTER_STRING), comment.endOffset,
        HighlighterLayer.ADDITIONAL_SYNTAX,
        COMMENT_ATTRIBUTE,
        HighlighterTargetArea.EXACT_RANGE
    )
}

 fun TypeScriptService.callTsService(possibleTypeAlias: PsiElement, virtualFile: VirtualFile): TypeScriptQuickInfoResponse? =
    this.getQuickInfoAt(possibleTypeAlias, virtualFile)?.get()