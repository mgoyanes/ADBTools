package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ShellOutputReceiver
import spock.adb.executeShellCommandWithTimeout

class OpenDeepLinkCommand : Command<String, String> {

    override fun execute(p: String, project: Project, device: IDevice): String {
        device.executeShellCommandWithTimeout("am start -a android.intent.action.VIEW -d \"$p\"", ShellOutputReceiver())
        return "Open DeepLink $p"
    }
}
