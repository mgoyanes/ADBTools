package spock.adb.avsb

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.GET_TOP_ACTIVITY_COMMAND
import spock.adb.ShellOutputReceiver
import spock.adb.command.NoInputCommand
import spock.adb.executeShellCommandWithTimeout

class OpenStatusCommand : NoInputCommand<String> {

    override fun execute(project: Project, device: IDevice): String {
        val result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout(GET_TOP_ACTIVITY_COMMAND, result)

        return when {
            result.toString().contains("com.android.tv.settings/.about.StatusActivity", true) -> {
                KeyEventCommand().execute(KeyEventCommand.BACK, project, device)
                "Closed status"
            }

            else -> {
                device.executeShellCommandWithTimeout("am start -n com.android.tv.settings/com.android.tv.settings.about.StatusActivity", result)
                "Opened Status"
            }
        }
    }
}
