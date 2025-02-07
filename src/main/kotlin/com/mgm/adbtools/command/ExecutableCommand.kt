package com.mgm.adbtools.command

interface ExecutableCommand<in P, out R> {
    @Throws(Exception::class)
    fun execute(command: P): R
}
