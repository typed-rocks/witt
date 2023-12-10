package typed.rocks.witt

import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile


fun PsiFile.isTsFile() = this.fileType is TypeScriptFileType

const val TYPE_POINTER_STRING = "^?"

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