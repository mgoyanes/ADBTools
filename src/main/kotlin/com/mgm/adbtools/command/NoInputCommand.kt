package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project

interface NoInputCommand<out R> {
    @Throws(Exception::class)
    fun execute(device: IDevice): R {
        throw NotImplementedError()
    }

    @Throws(Exception::class)
    fun execute(project: Project, device: IDevice): R {
        throw NotImplementedError()
    }
}
