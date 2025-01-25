package spock.adb.avsb

import ProcessCommand
import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.AVSB_PACKAGE
import spock.adb.EMPTY
import spock.adb.MIN_TIME_TO_OUTPUT_RESPONSE
import spock.adb.ONE
import spock.adb.ShellOutputReceiver
import spock.adb.ZERO
import spock.adb.avsb.KeyEventCommand.Companion.DPAD_DOWN
import spock.adb.avsb.KeyEventCommand.Companion.DPAD_RIGHT
import spock.adb.avsb.KeyEventCommand.Companion.DPAD_UP
import spock.adb.avsb.KeyEventCommand.Companion.EXIT
import spock.adb.avsb.KeyEventCommand.Companion.FOUR
import spock.adb.avsb.KeyEventCommand.Companion.HOME
import spock.adb.avsb.KeyEventCommand.Companion.OK
import spock.adb.avsb.KeyEventCommand.Companion.TWO
import spock.adb.command.Command
import spock.adb.command.GetFragmentsCommand
import spock.adb.executeShellCommandWithTimeout

class OpenCheatMenuCommand : Command<String, String> {

    companion object {
        private const val ENTER = "adb shell input keyevent $OK"
        private val FTU = listOf(
            "adb shell input keyevent $FOUR",
            "adb shell input keyevent $TWO",
            "adb shell input keyevent $FOUR",
            "adb shell input keyevent $TWO"
        )
        private val COMMANDS = listOf(
            "adb shell input keyevent $EXIT",
            "adb shell input keyevent $HOME",
            "adb shell input keyevent $DPAD_UP",
            "adb shell input keyevent $DPAD_RIGHT",
            "adb shell input keyevent $DPAD_RIGHT",
            "adb shell input keyevent $DPAD_RIGHT",
            "adb shell input keyevent $DPAD_RIGHT",
            "adb shell input keyevent $DPAD_RIGHT",
            "adb shell input keyevent $DPAD_RIGHT",
            "adb shell input keyevent $DPAD_RIGHT",
            "adb shell input keyevent $OK",
            "adb shell input keyevent $DPAD_DOWN",
            "adb shell input keyevent $DPAD_DOWN",
            "adb shell input keyevent $DPAD_DOWN",
            "adb shell input keyevent $OK",
            "adb shell input keyevent $DPAD_DOWN",
            "adb shell input keyevent $OK"
        )
    }


    override fun execute(p: String, project: Project, device: IDevice): String {
        val regex = """versionName=\d+\.(\d+)\.\d+""".toRegex()

        val receiver = ShellOutputReceiver()
        val fragmentsClass = GetFragmentsCommand().execute(AVSB_PACKAGE, project, device).toString()

        if (fragmentsClass.contains("FtuStepsFragment")) {
            FTU.forEach { command ->
                ProcessCommand().execute(command, EMPTY)
            }
        } else {
            device.executeShellCommandWithTimeout("dumpsys package $AVSB_PACKAGE | grep versionName", receiver, MIN_TIME_TO_OUTPUT_RESPONSE)

            val version = regex.find(receiver.toString())?.groups?.get(ONE)?.value?.toIntOrNull() ?: ZERO

            if (version >= 44) {
                COMMANDS.forEach { command ->
                    ProcessCommand().execute(command, EMPTY)
                }

                for (i in 0..20) {
                    ProcessCommand().execute(ENTER, EMPTY)
                }
            }
        }

        return "Opened Cheat Menu"
    }

}
