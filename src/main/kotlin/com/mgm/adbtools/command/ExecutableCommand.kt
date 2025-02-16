package com.mgm.adbtools.command

import com.intellij.openapi.project.Project

interface ExecutableCommand<in P, out R> {
    @Throws(Exception::class)
    fun execute(command: P, project: Project): R
}
