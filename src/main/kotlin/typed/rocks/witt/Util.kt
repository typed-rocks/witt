package typed.rocks.witt

import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.MILLISECONDS

var UNDER_TEST = false

const val TYPE_POINTER_STRING = "^?"

@Suppress("UseJBColor")
val regularColor = Color(35, 90, 151)

@Suppress("UseJBColor")
val darkColor = Color(49, 120, 198)

val COMMENT_ATTRIBUTE = TextAttributes().also {
    it.fontType = Font.BOLD
    it.foregroundColor = JBColor(regularColor, darkColor)
}


fun String.trimmedText(maxCharacters: Int): String {
    val singleSpaces = this.replace(Regex("\\n"), " ").replace("\\{( {2,})".toRegex(), "\\{ ").replace(";\\s+([A-Za-z].*)".toRegex(), ";  $1")
    return if (maxCharacters > singleSpaces.length) singleSpaces else singleSpaces.substring(
        0, maxCharacters - 3
    ) + "..."
}

fun <T> CompletableFuture<T?>.nullOnTimeout(ms: Long): CompletableFuture<T?> =
    this.completeOnTimeout(null, ms.orTest, MILLISECONDS)

fun <T> CompletableFuture<List<T>>.emptyOnTimeout(ms: Long): CompletableFuture<List<T>?> =
    this.completeOnTimeout(listOf(), ms.orTest, MILLISECONDS)

val Long.orTest: Long
    get() = if (UNDER_TEST) 1_000 else this
