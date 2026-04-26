package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.debugger.Debugger
import com.mgm.adbtools.executeShellCommandWithTimeout
import com.mgm.adbtools.forceKillApp
import com.mgm.adbtools.getDefaultActivityForApplication
import com.mgm.adbtools.isAppInstall

class RestartAppWithDebuggerCommand : Command<String, List<String>> {
    override fun execute(p: String, project: Project, device: IDevice): List<String> {
        if (!device.isAppInstall(p)) throw Exception("Application $p not installed")
        device.forceKillApp(p)
        return device.getDefaultActivityForApplication(p).ifEmpty { throw Exception("No Launcher Activity Found") }
    }

    fun startWithDebugger(activity: String, project: Project, device: IDevice, packageName: String) {
        device.executeShellCommandWithTimeout("am start -D -n $activity", ShellOutputReceiver())
        Debugger(project, device, packageName).attach()
    }
}
