package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.debugger.Debugger
import com.mgm.adbtools.executeShellCommandWithTimeout
import com.mgm.adbtools.forceKillApp
import com.mgm.adbtools.getDefaultActivityForApplication
import com.mgm.adbtools.isAppInstall

class RestartAppWithDebuggerCommand : Command<String, Unit> {
    override fun execute(p: String, project: Project, device: IDevice) =
        when {
            device.isAppInstall(p) -> {
                device.forceKillApp(p)
                val activity = device.getDefaultActivityForApplication(p)

                when {
                    activity.isNotEmpty() -> {
                        device.executeShellCommandWithTimeout("am start -D -n $activity", ShellOutputReceiver())

                        Debugger(project, device, p).attach()
                    }
                    else -> throw Exception("No Default Activity Found")
                }
            }
            else -> throw Exception("Application $p not installed")
        }
}
