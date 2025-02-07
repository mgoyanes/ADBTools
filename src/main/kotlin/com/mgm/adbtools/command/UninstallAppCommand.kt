package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project

import com.mgm.adbtools.isAppInstall

class UninstallAppCommand : Command<String, Unit> {

    override fun execute(p: String, project: Project, device: IDevice) {
        if (device.isAppInstall(p)) {
            device.uninstallPackage(p)
        } else
            throw Exception("Application $p not installed")
    }
}
