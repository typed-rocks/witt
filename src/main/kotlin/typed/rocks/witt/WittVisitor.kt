package typed.rocks.witt

import com.intellij.lang.javascript.integration.JSAnnotationError
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeAlias
import com.intellij.lang.typescript.compiler.languageService.protocol.commands.response.TypeScriptQuickInfoResponse
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.util.elementType

class WittVisitor(private val psiFile: PsiFile, private val document: Document) : PsiRecursiveElementVisitor() {
    val result: MutableSet<Pair<PsiComment, PsiElement>> = mutableSetOf()
    val types: MutableMap<String, TypeScriptTypeAlias> = mutableMapOf()
    override fun visitComment(comment: PsiComment) {
        collectCommentToPsiElement(comment)?.run(result::add)
    }

    override fun visitElement(element: PsiElement) {
        if (element is TypeScriptTypeAlias && element.name != null) {
            types[element.name!!] = element
        }
    }

    fun collectCommentToPsiElement(
        comment: PsiComment
    ): CommentToElement? {
        if (!comment.text.trim().endsWith(TYPE_POINTER_STRING)) return null

        val commentLineIndex = document.getLineNumber(comment.startOffsetInParent)

        if (commentLineIndex < 1) {
            log.debug("Comment found at line $commentLineIndex but there is no line above. So skipping further checking")
            return null
        }

        // if a comment does not start at index 0 of the line, we need to add the index before
        val typeElement = psiFile.getTypeAboveComment(document, comment, commentLineIndex)
        return if (typeElement == null || typeElement is PsiComment) null else comment to typeElement
    }

}


data class VisitorResponse(
    val commentToType: List<Pair<PsiComment, TypeScriptQuickInfoResponse?>> = listOf(),
    val types: MutableMap<String, TypeScriptTypeAlias> = mutableMapOf(),
    val errorList: List<JSAnnotationError?>? = listOf()
)