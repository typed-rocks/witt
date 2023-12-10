package typed.rocks.witt

import com.intellij.codeInsight.hints.*
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeAlias
import com.intellij.lang.typescript.compiler.TypeScriptService
import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.refactoring.suggested.endOffset
import javax.swing.JComponent
import javax.swing.JPanel


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
    ): InlayHintsCollector? = if (file.isTsFile() && TypeScriptLanguageServiceUtil.isServiceEnabled(file.project)) {
        Collector(file, editor)
    } else {
        log.debug("${file.name} is not a typescript file. So skipping Collector.")
        null
    }

    class Collector(private val psiFile: PsiFile, editor: Editor) : FactoryInlayHintsCollector(editor) {
        private val virtualFile = psiFile.virtualFile
        private val tss = TypeScriptService.getForFile(psiFile.project, virtualFile)!!

        override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {

            // we do the recursive call ourselves
            if (element !is PsiFile) return false
            val visitor = Visitor(psiFile, editor.document)
            element.accept(visitor)
            Thread.sleep(100)

            visitor.result.forEach { (comment, possibleTypeAlias) -> addToSink(possibleTypeAlias, sink, comment) }
            return false
        }

        private fun addToSink(
            possibleTypeAlias: PsiElement,
            sink: InlayHintsSink,
            comment: PsiComment
        ) {
            val quickInfoResponseFuture = tss.getQuickInfoAt(possibleTypeAlias, virtualFile)
            val quickinfoResponse = quickInfoResponseFuture?.get()
            if (quickinfoResponse == null) {
                log.debug("No Quickresponse found for the type '${possibleTypeAlias.text}'")
            } else {
                sink.addInlineElement(
                    comment.endOffset,
                    true,
                    factory.roundWithBackground(factory.text(quickinfoResponse.displayString)),
                    false
                )
            }
        }


    }
}

fun inlaysToAdd(psiFile: PsiFile, element: PsiComment, document: Document): Pair<PsiComment, PsiElement>? {
    val commentText = element.text
    if (!commentText.trim().endsWith(TYPE_POINTER_STRING)) return null

    val commentLineIndex = document.getLineNumber(element.startOffsetInParent)

    if (commentLineIndex < 1) {
        log.debug("Comment found at line $commentLineIndex but there is no line above. So skipping further checking")
        return null
    }

    // if a comment does not start at index 0 of the line, we need to add the index before
    val typeElement = psiFile.getTypeAboveComment(document, element, commentLineIndex)

    val possibleTypeAlias = typeElement?.parent
    if (possibleTypeAlias !is TypeScriptTypeAlias) {
        log.debug("The found parent is no no TypescriptTypeAlias. So skipping the checking.", possibleTypeAlias)
        return null
    }
    return element to possibleTypeAlias
}

class Visitor(private val psiFile: PsiFile, private val document: Document) : PsiRecursiveElementVisitor() {
    val result: MutableSet<Pair<PsiComment, PsiElement>> = mutableSetOf()
    override fun visitComment(comment: PsiComment) {
        inlaysToAdd(psiFile, comment, document)?.let(result::add)
    }
}
