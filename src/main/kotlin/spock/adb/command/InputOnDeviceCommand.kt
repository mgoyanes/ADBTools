package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ShellOutputReceiver
import spock.adb.executeShellCommandWithTimeout

class InputOnDeviceCommand : Command<String, String> {

    override fun execute(p: String, project: Project, device: IDevice): String {
        device.executeShellCommandWithTimeout("input text '$p'", ShellOutputReceiver())
        return "Input on device $p"
    }
}
