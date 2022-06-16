package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ShellOutputReceiver
import spock.adb.isAndroid12OrAbove
import java.util.concurrent.TimeUnit

class GetActivityCommand : Command<Any, String?> {
    override fun execute(p: Any, project: Project, device: IDevice): String? {
        val shellOutputReceiver = ShellOutputReceiver()
        val command =
            when {
                device.isAndroid12OrAbove() -> "dumpsys activity activities | grep topResumedActivity"
                else -> "dumpsys activity activities | grep mResumedActivity"
            }

        device.executeShellCommand(command, shellOutputReceiver, 15L, TimeUnit.SECONDS)

        return shellOutputReceiver.toString().split(" ").find { it.contains("/") }
            ?.replace("/.", ".")
            ?.replace(Regex(".+/"), "")
    }
}
