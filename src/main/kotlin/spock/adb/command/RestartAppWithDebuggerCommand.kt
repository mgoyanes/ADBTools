package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.MAX_TIME_TO_OUTPUT_RESPONSE
import spock.adb.ShellOutputReceiver
import spock.adb.debugger.Debugger
import spock.adb.executeShellCommandWithTimeout
import spock.adb.forceKillApp
import spock.adb.getDefaultActivityForApplication
import spock.adb.isAppInstall

class RestartAppWithDebuggerCommand : Command<String, Unit> {
    override fun execute(p: String, project: Project, device: IDevice) =
        when {
            device.isAppInstall(p) -> {
                device.forceKillApp(p, MAX_TIME_TO_OUTPUT_RESPONSE)
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
