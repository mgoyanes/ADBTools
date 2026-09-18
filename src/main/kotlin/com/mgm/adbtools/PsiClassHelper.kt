package com.mgm.adbtools

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import java.util.concurrent.Callable

/**
 * Runs [block] on a pooled thread and waits for it when called from the EDT, otherwise runs it
 * in place. PSI/index lookups are index-scanning work that platform code (e.g. Android's
 * BindingClassFinder, triggered internally by JavaPsiFacade.findClass) expects to run off the
 * EDT under a classic runReadAction - the coroutine-based runReadActionBlocking grants the read
 * lock but keeps executing on whichever thread called it, which does not satisfy that.
 */
private fun <T> computeOffEdt(block: () -> T): T =
    if (ApplicationManager.getApplication().isDispatchThread) {
        ApplicationManager.getApplication().executeOnPooledThread(Callable(block)).get()
    } else {
        block()
    }

fun PsiClass.openIn(project: Project) {
    val virtualFile = computeOffEdt { runReadAction { containingFile.virtualFile } }
    OpenFileDescriptor(project, virtualFile, 1, 0).navigateInEditor(project, false)
}

fun String.psiClassByNameFromCache(project: Project): PsiClass? {
    return computeOffEdt {
        runReadAction {
            PsiShortNamesCache
                .getInstance(project)
                .getClassesByName(
                    this, GlobalSearchScope.allScope(project)
                )
                .firstOrNull()
        }
    }
}

fun String.psiClassByNameFromProject(project: Project): PsiClass? {
    return computeOffEdt {
        runReadAction {
            JavaPsiFacade.getInstance(project).findClass(this, GlobalSearchScope.allScope(project))
        }
    }
}
