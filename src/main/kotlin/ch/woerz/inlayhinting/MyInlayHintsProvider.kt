package ch.woerz.inlayhinting

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeAlias
import com.intellij.lang.typescript.compiler.TypeScriptService
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import java.util.concurrent.TimeUnit


private const val TYPE_POINTER_STRING = "^?"

class MyInlayHintsProvider : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector =
        Collector(editor.document, file)

    private class Collector(val document: Document, val psiFile: PsiFile) : SharedBypassCollector {
        val virtualFile: VirtualFile = psiFile.virtualFile
        val tss = if (psiFile.isTsFile()) TypeScriptService.getForFile(psiFile.project, virtualFile) else null

        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {

            if (tss == null) return
            val commentText = element.text
            if (element !is PsiComment || !commentText.contains(TYPE_POINTER_STRING)) return

            val commentLineIndex = document.getLineNumber(element.startOffsetInParent)

            if (commentLineIndex < 1) {
                return
            }


            // if an comment does not start at index 0 of the line, we need to add these
            val commentStartOffsetColumn = element.startOffsetInParent - document.getLineStartOffset(commentLineIndex)
            val typeLineIndex = commentLineIndex - 1
            val arrowUpColumn = commentStartOffsetColumn + commentText.indexOf(TYPE_POINTER_STRING)
            val typeElement = getElementAtLineAndColumn(typeLineIndex, arrowUpColumn)


            val possibleTypeAlias = typeElement?.parent
            if (possibleTypeAlias !is TypeScriptTypeAlias) {
                return
            }


            val quickInfoResponseFuture = tss.getQuickInfoAt(possibleTypeAlias, virtualFile)
            val quickinfoResponse = quickInfoResponseFuture?.get(1000, TimeUnit.MILLISECONDS)

            quickinfoResponse?.let {
                val position = InlineInlayPosition(element.endOffset, true)

                sink.addPresentation(position, hasBackground = true) {
                    text(it.displayString)
                }
            }
        }

        fun getElementAtLineAndColumn(line: Int, column: Int): PsiElement? {
            val lineStartOffset = document.getLineStartOffset(line)
            val offset = lineStartOffset + column
            return if (offset < 0) null else psiFile.findElementAt(offset)
        }
    }


}