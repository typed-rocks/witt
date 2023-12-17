package typed.rocks.witt

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.javascript.integration.JSAnnotationError
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeAlias
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import java.util.concurrent.CompletableFuture

@Suppress("UnstableApiUsage")
class WittCollector(
    private val psiFile: PsiFile,
    private val editor: Editor,
    val rangeHighlighter: MutableList<RangeHighlighter>
) : FactoryInlayHintsCollector(editor) {

    private val virtualFile = psiFile.virtualFile
    private val document = editor.document
    private val tss = psiFile.getTs()
    private val tsc = psiFile.getTsc()
    private val alreadyDone = mutableSetOf<PsiComment>()

    private var first = true

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {

        // we do the recursive call ourselves
        if (!first) return false
        first = false

        val result = retryOn({ collectInlays() }, { it.commentToType.all { el -> el.second == null } })
        addInfoToSink(result, editor, sink)
        return false
    }

    private fun <T> retryOn(fn: () -> T, whenToRetry: (T) -> Boolean): T {
        val res = fn()
        return if (whenToRetry(res)) {
            return fn()
        } else res

    }

    private fun addInfoToSink(
        result: VisitorResponse,
        editor: Editor,
        sink: InlayHintsSink
    ) {
        rangeHighlighter.forEach { editor.markupModel.removeHighlighter(it) }
        result.commentToType.forEach { (comment, type) ->
            val response = type?.displayString

            if (!alreadyDone.contains(comment)) {
                val err = result.errorList?.findErrorAboveComment(editor, comment)
                val errDescription = err?.description?.trimmedText(editor.getCharacterMax())
                val firstFound = errDescription ?: response
                firstFound?.let {
                    sink.addTypeComment(firstFound, comment, result.types)
                    alreadyDone.add(comment)
                    rangeHighlighter.add(editor.addHighlight(comment))
                }
            }
        }
    }

    private fun collectInlays(): VisitorResponse {
        val wittVisitor = WittVisitor(psiFile, document)
        psiFile.acceptChildren(wittVisitor)
        if (wittVisitor.result.isEmpty()) return VisitorResponse()
        val errorFuture = tsc.getAllHighlights(psiFile)

        val commentToTypeFutures = wittVisitor.result
            .map { (comment, type) -> comment to tss.getQuickInfoAtCf(type, virtualFile) }
        val futures = commentToTypeFutures.map { (_, value) -> value }.toTypedArray()
        CompletableFuture.allOf(*futures, errorFuture).join()
        val errorResult = errorFuture.get()?.filter { it.severity == HighlightSeverity.ERROR }
        val commentToType = commentToTypeFutures.map { it.first to it.second.get() }
        return VisitorResponse(commentToType, wittVisitor.types, errorResult)
    }

    private fun List<JSAnnotationError?>.findErrorAboveComment(
        editor: Editor,
        comment: PsiComment
    ) = this.filterNotNull()
        .find {
            if (it.line + 1 >= editor.document.lineCount) false else psiFile.getElementAtLineAndColumn(
                editor.document,
                it.line + 1,
                it.column
            ) == comment
        }

    private fun InlayHintsSink.addTypeComment(
        response: String,
        comment: PsiComment,
        typeMap: Map<String, TypeScriptTypeAlias>
    ) {
        val trimmed = response.trimmedText(editor.getCharacterMax())

        val foundAType = typeMap.keys.any { trimmed.contains(it) }
        val mapped = if (foundAType) trimmed.createRefs(typeMap) else listOf(trimmed.inlayText)
        val result = factory.list(mapped)
        this.addInlineElement(
            comment.endOffset, true, factory.roundWithBackground(result), false
        )
    }

    private val String.inlayText: InlayPresentation
        get() = factory.text(this)

    private fun String.createRefs(typeMap: TypeNameToAlias) = this.split(" ").map {
        val trimmed = it.trim()
        val end = trimmed.lastOrNull()?.toString() ?: ""
        val firstIsChar = trimmed.firstOrNull()?.isLetter() ?: false
        val sequenced = if (firstIsChar) trimmed.toRefSequence(end, typeMap) else it.inlayText
        factory.seq(" ".inlayText, sequenced)
    }

    private fun String.toRefSequence(
        end: String, typeMap: TypeNameToAlias
    ): InlayPresentation {
        val ends = listOf(";", ")")
        val closing = ends.find { it == end } ?: ""
        val removed = this.removeSuffix(closing)
        val ref = createReferenceInlay(removed, typeMap)
        return factory.seq(ref, closing.inlayText)
    }


    private fun createReferenceInlay(currentTypeElement: String, typeMapping: TypeNameToAlias): InlayPresentation {
        val navigationElement = typeMapping[currentTypeElement]?.navigationElement
        val inlayText = currentTypeElement.inlayText
        return if (navigationElement is Navigatable && navigationElement.canNavigate()) {
            factory.referenceOnHover(inlayText) { _, _ ->
                navigationElement.navigate(true)
            }
        } else {
            inlayText
        }
    }
}

@Suppress("UnstableApiUsage")
fun PresentationFactory.list(presentations: List<InlayPresentation>) = this.seq(*presentations.toTypedArray())