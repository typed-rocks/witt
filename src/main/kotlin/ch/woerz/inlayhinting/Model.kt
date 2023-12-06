package ch.woerz.inlayhinting

import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.psi.PsiFile


fun PsiFile.isTsFile() = this.fileType is TypeScriptFileType