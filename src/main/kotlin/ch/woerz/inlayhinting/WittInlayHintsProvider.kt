package ch.woerz.inlayhinting

import com.intellij.codeInsight.hints.*
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeAlias
import com.intellij.lang.typescript.compiler.TypeScriptService
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.JPanel


private const val TYPE_POINTER_STRING = "^?"

private val log = Logger.getInstance(WittInlayHintsProvider::class.java)

@Suppress("UnstableApiUsage")
class WittInlayHintsProvider : InlayHintsProvider<NoSettings> {


    override val key = SettingsKey<NoSettings>("")

    override val name: String = "typed.rocks.witt"

    override val previewText: String = "WîTT"

    override fun createSettings(): NoSettings = NoSettings()

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = JPanel()

        }
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        return if (file.isTsFile()) {
            Collector(file, editor)
        } else {
            log.debug("${file.name} is not a typescript file. So skipping Collector.")
            return null
        }
    }


    class Collector(private val psiFile: PsiFile, editor: Editor) : FactoryInlayHintsCollector(editor) {
        private val document = editor.document
        private val virtualFile = psiFile.virtualFile
        private val tss = TypeScriptService.getForFile(psiFile.project, virtualFile)!!

        override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {


            val commentText = element.text
            if (element !is PsiComment || !commentText.trim().endsWith(TYPE_POINTER_STRING)) return true

            val commentLineIndex = document.getLineNumber(element.startOffsetInParent)

            if (commentLineIndex < 1) {
                log.debug("Comment found at line $commentLineIndex but there is no line above. So skipping further checking")
                return true
            }

            // if a comment does not start at index 0 of the line, we need to add the index before
            val typeElement = getTypeElement(document, element, commentLineIndex)

            val possibleTypeAlias = typeElement?.parent
            if (possibleTypeAlias !is TypeScriptTypeAlias) {
                log.debug("The found parent is no no TypescriptTypeAlias. So skipping the checking.", possibleTypeAlias)
                return true
            }

            val quickInfoResponseFuture = tss.getQuickInfoAt(possibleTypeAlias, virtualFile)
            val quickinfoResponse = quickInfoResponseFuture?.get()
            if (quickinfoResponse == null) {
                log.debug("No Quickresponse found for the type '${possibleTypeAlias.text}'")
                return true
            }

            sink.addInlineElement(
                element.endOffset,
                true,
                factory.roundWithBackground(factory.text(quickinfoResponse.displayString)),
                false
            )
            println("Trigger ${quickinfoResponse.displayString}")
            InlayHintsPassFactory.forceHintsUpdateOnNextPass()
            return true
        }

        private fun getTypeElement(document: Document, comment: PsiComment, commentLineIndex: Int): PsiElement? {
            val commentStartOffsetColumn = comment.startOffsetInParent - document.getLineStartOffset(commentLineIndex)
            val arrowUpColumn = commentStartOffsetColumn + comment.text.indexOf(TYPE_POINTER_STRING)
            val typeLineIndex = commentLineIndex - 1
            return getElementAtLineAndColumn(typeLineIndex, arrowUpColumn)
        }

        private fun getElementAtLineAndColumn(line: Int, column: Int): PsiElement? {
            val lineStartOffset = document.getLineStartOffset(line)
            val offset = lineStartOffset + column
            return if (offset < 0) null else psiFile.findElementAt(offset)
        }
    }
}
