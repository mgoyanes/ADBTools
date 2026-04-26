package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout
import com.mgm.adbtools.getDefaultActivityForApplication
import com.mgm.adbtools.isAppInForeground
import com.mgm.adbtools.isAppInstall
import com.mgm.adbtools.startActivity
import java.util.concurrent.TimeUnit

class ProcessDeathCommand : Command<String, List<String>> {
    override fun execute(p: String, project: Project, device: IDevice): List<String> {
        if (!device.isAppInstall(p)) throw Exception("Application $p not installed")
        sendAppToBackgroundIfInForeground(device, p)
        Thread.sleep(2500L)
        killAppProcess(device, p)
        return device.getDefaultActivityForApplication(p).ifEmpty { throw Exception("No Launcher Activity Found") }
    }

    private fun sendAppToBackgroundIfInForeground(device: IDevice, p: String) {
        if (device.isAppInForeground(p)) {
            device.executeShellCommandWithTimeout("input keyevent 3", ShellOutputReceiver(), 0, TimeUnit.SECONDS)
        }
    }

    private fun killAppProcess(device: IDevice, p: String) =
        device.executeShellCommandWithTimeout("am kill $p", ShellOutputReceiver())
}
