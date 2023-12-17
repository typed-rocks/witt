package typed.rocks.witt

import com.intellij.codeInsight.hints.*
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeAlias
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.JComponent
import javax.swing.JPanel

val log = Logger.getInstance(WittInlayHintsProvider::class.java)

typealias TypeNameToAlias = Map<String, TypeScriptTypeAlias>
typealias CommentToElement = Pair<PsiComment, PsiElement>

@Suppress("UnstableApiUsage")
class WittInlayHintsProvider : InlayHintsProvider<NoSettings> {

    override val key = SettingsKey<NoSettings>("typed.rocks.witt")

    override val name: String = "typed.rocks.witt"

    override val previewText: String = "WîTT"

    private var rangeHighlighter = mutableListOf<RangeHighlighter>()

    override fun createSettings(): NoSettings = NoSettings()

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = JPanel()
        }
    }

    override fun getCollectorFor(
        file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink
    ): InlayHintsCollector? = if (file.checkTsSetup()) {
        val res = WittCollector(file, editor, rangeHighlighter)
        rangeHighlighter = res.rangeHighlighter
        res
    } else {
        null
    }

}

