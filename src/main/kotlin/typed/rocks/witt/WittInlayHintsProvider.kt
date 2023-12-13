package typed.rocks.witt

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeAlias
import com.intellij.lang.typescript.compiler.TypeScriptService
import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
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


    override val key = SettingsKey<NoSettings>("typed.rocks.witt")

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

}

fun inlaysToAdd(psiFile: PsiFile, comment: PsiComment, document: Document): Pair<PsiComment, PsiElement>? {
    if (!comment.text.trim().endsWith(TYPE_POINTER_STRING)) return null

    val commentLineIndex = document.getLineNumber(comment.startOffsetInParent)

    if (commentLineIndex < 1) {
        log.debug("Comment found at line $commentLineIndex but there is no line above. So skipping further checking")
        return null
    }

    // if a comment does not start at index 0 of the line, we need to add the index before
    val typeElement = psiFile.getTypeAboveComment(document, comment, commentLineIndex)
    return if (typeElement == null) null else comment to typeElement
}

class Visitor(private val psiFile: PsiFile, private val document: Document) : PsiRecursiveElementVisitor() {
    val result: MutableSet<Pair<PsiComment, PsiElement>> = mutableSetOf()
    val types: MutableMap<String, TypeScriptTypeAlias> = mutableMapOf()
    override fun visitComment(comment: PsiComment) {
        inlaysToAdd(psiFile, comment, document)?.run(result::add)
    }

    override fun visitElement(element: PsiElement) {
        if (element is TypeScriptTypeAlias && element.name != null) {
            types[element.name!!] = element
        }
    }
}


@Suppress("UnstableApiUsage")
class Collector(private val psiFile: PsiFile, private val editor: Editor) : FactoryInlayHintsCollector(editor) {

    private val virtualFile = psiFile.virtualFile
    private val tss = TypeScriptService.getForFile(psiFile.project, virtualFile)!!
    private var first = true

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {

        // we do the recursive call ourselves
        if (!first) return false
        first = false
        val visitor = Visitor(psiFile, editor.document)
        psiFile.acceptChildren(visitor)
        return ReadAction.compute<Boolean, Throwable> {
            Thread.sleep(10)
            visitor.result.forEach { (comment, possibleTypeAlias) ->
                editor.addHighlight(comment)
                addToSink(possibleTypeAlias, sink, comment, visitor.types)
            }
            false
        }
    }

    private fun addToSink(
        possibleTypeAlias: PsiElement,
        sink: InlayHintsSink,
        comment: PsiComment,
        typeMap: Map<String, TypeScriptTypeAlias>
    ): Boolean {
        val quickinfoResponse = tss.callTsService(possibleTypeAlias, virtualFile)?.displayString
        if (quickinfoResponse == null) {
            log.debug("No Quickresponse found for the type '${possibleTypeAlias.text}'")
            return false
        }
        val editorWidth = editor.getCharacterMax()
        val trimmed = quickinfoResponse.trimmedText(editorWidth)

        val foundAType = typeMap.keys.any { trimmed.contains(it) }
        val mapped = if (foundAType) trimmed.createRefs(typeMap) else arrayOf(trimmed.inlayText)
        val result = factory.seq(*mapped)
        sink.addInlineElement(
            comment.endOffset,
            true,
            factory.roundWithBackground(result),
            false
        )
        return true
    }

    private val String.inlayText: InlayPresentation
        get() = factory.text(this)

    private val ends = listOf(";", ")")
    private fun String.createRefs(typeMap: Map<String, TypeScriptTypeAlias>) =
        this.split(" ").map {
            val trimmed = it.trim()
            val end = trimmed.lastOrNull()?.toString() ?: ""
            val firstIsChar = trimmed.firstOrNull()?.isLetter() ?: false
            if (!firstIsChar) factory.seq(" ".inlayText, it.inlayText) else toRefSequence(end, trimmed, typeMap)
        }.toTypedArray()

    private fun toRefSequence(
        end: String,
        trimmed: String,
        typeMap: Map<String, TypeScriptTypeAlias>
    ): InlayPresentation {
        val closing = ends.find { e -> e == end } ?: ""
        val removed = trimmed.removeSuffix(closing)
        val ref = typeText(removed, typeMap)
        val normalSeq = factory.seq(" ".inlayText, ref)
        return factory.seq(normalSeq, closing.inlayText)
    }


    private fun typeText(currentNonLimiter: String, typeMapping: Map<String, TypeScriptTypeAlias>): InlayPresentation {
        val navigationElement = typeMapping[currentNonLimiter]?.navigationElement
        val inlayText = currentNonLimiter.inlayText
        return if (navigationElement != null && navigationElement is Navigatable && navigationElement.canNavigate()) {
            factory.referenceOnHover(inlayText) { _, _ ->
                navigationElement.navigate(true)
            }
        } else {
            inlayText
        }
    }


}

