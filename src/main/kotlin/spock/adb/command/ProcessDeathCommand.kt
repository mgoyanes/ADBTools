package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ShellOutputReceiver
import spock.adb.executeShellCommandWithTimeout
import spock.adb.getDefaultActivityForApplication
import spock.adb.isAppInForeground
import spock.adb.isAppInstall
import spock.adb.startActivity
import java.util.concurrent.TimeUnit

class ProcessDeathCommand : Command<String, Unit> {

    override fun execute(p: String, project: Project, device: IDevice) {
        if (device.isAppInstall(p)) {
            sendAppToBackgroundIfInForeground(device, p)

            Thread.sleep(2500L) //If we don't add this delay, the following commands executes without the app
            // being on the background thus not working.

            killAppProcess(device, p)

            startApplication(device, p)
        } else {
            throw Exception("Application $p not installed")
        }
    }

    private fun sendAppToBackgroundIfInForeground(device: IDevice, p: String) {
        if (device.isAppInForeground(p)) {
            device.executeShellCommandWithTimeout("input keyevent 3", ShellOutputReceiver(), 0, TimeUnit.SECONDS)
        }
    }

    private fun killAppProcess(device: IDevice, p: String) =
        device.executeShellCommandWithTimeout("am kill $p", ShellOutputReceiver())

    private fun startApplication(device: IDevice, p: String) {
        val activity = device.getDefaultActivityForApplication(p)
        when {
            activity.isNotEmpty() -> device.startActivity(activity)
            else -> throw Exception("No Default Activity Found")
        }
    }
}
