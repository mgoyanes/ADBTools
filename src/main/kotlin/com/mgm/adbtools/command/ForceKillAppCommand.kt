package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project

import com.mgm.adbtools.isAppInstall
import com.mgm.adbtools.forceKillApp

class ForceKillAppCommand:Command<String,Unit> {
    override fun execute(p: String, project: Project, device: IDevice) {
        if (device.isAppInstall(p))
            device.forceKillApp(p)
        else
            throw Exception("Application $p not installed" )
    }
}
