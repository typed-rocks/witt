package typed.rocks.witt

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.ui.JBColor
import com.intellij.util.text.findTextRange
import java.awt.Font


class CustomCommentAnnotator : Annotator {


    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiFile) return
        val commentFinder = CommentFinder()
        element.accept(commentFinder)
        ReadAction.run<Throwable> {
            Thread.sleep(100)

            commentFinder.foundComments.forEach {comment ->  // Do something with the range, like creating an annotation
                // For example, highlighting the range:
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(comment)
                    .textAttributes(
                        TextAttributesKey.createTextAttributesKey(
                            "SPECIAL_COMMENT",
                            TextAttributes(JBColor.BLUE, null, null, null, Font.BOLD)
                        )
                    )
                    .create()}

        }

    }
}

class CommentFinder : PsiRecursiveElementVisitor() {

    val foundComments = mutableSetOf<TextRange>()
    override fun visitComment(comment: PsiComment) {
        if (comment.text.trim().endsWith("^?")) {
            // Implement your logic to identify the specific part of the comment here
            val range = comment.text.findTextRange("^?")!!.shiftRight(comment.startOffsetInParent)
            foundComments.add(range)


        }
    }
}