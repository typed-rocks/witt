package typed.rocks.witt

import com.intellij.lang.javascript.integration.JSAnnotationError
import com.intellij.lang.typescript.compiler.TypeScriptCompilerService
import com.intellij.lang.typescript.compiler.TypeScriptService
import com.intellij.lang.typescript.compiler.languageService.protocol.commands.response.TypeScriptQuickInfoResponse
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.concurrent.CompletableFuture


fun PsiFile.checkTsSetup(): Boolean {
    if (this.isTsFile()) {
        if (this.getTsc() != null && this.getTs() != null) {
            return true
        }
        sendNoTsNotification()
        return false
    }
    log.debug("${this.name} is not a typescript file. So skipping Collector.")
    return false
}

fun PsiFile.getTs(): TypeScriptService? = TypeScriptService.getForFile(this.project, this.virtualFile)
fun PsiFile.getTsc(): TypeScriptCompilerService? =
    TypeScriptService.getCompilerServiceForFile(this.project, this.virtualFile)

fun TypeScriptService?.getQuickInfoAtCf(
    possibleTypeAlias: PsiElement,
    virtualFile: VirtualFile
): CompletableFuture<TypeScriptQuickInfoResponse?> =
    (this?.getQuickInfoAt(possibleTypeAlias, virtualFile) as CompletableFuture?)?.nullOnTimeout(100)
        ?: CompletableFuture.completedFuture(
            null
        )

fun TypeScriptService?.getAllHighlights(psiFile: PsiFile): CompletableFuture<List<JSAnnotationError>?> =
    this?.highlight(psiFile)?.emptyOnTimeout(200) ?: CompletableFuture.completedFuture(listOf())

fun sendNoTsNotification() {
    val notification = Notification(
        "typed.rocks.witt",  // A unique ID for your plugin or notification group
        "WiTT - Typescript Compiler is not available",  // Title of the notification
        "Please check if your Typescript Service is configured",  // The actual message
        NotificationType.WARNING // Type of the notification
    )
    Notifications.Bus.notify(notification)
}