import com.intellij.openapi.project.Project
import com.mgm.adbtools.AVSB_PACKAGE
import com.mgm.adbtools.ONE
import com.mgm.adbtools.ZERO
import com.mgm.adbtools.command.ExecutableCommand
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.util.concurrent.TimeUnit

class ProcessCommand : ExecutableCommand<ProcessCommand.Command, String> {

    enum class Command(private val command: String) {
        REBOOT("adb reboot"),
        UNINSTALL("adb shell pm uninstall $AVSB_PACKAGE"),
        FORCE_KILL("adb shell am force-stop $AVSB_PACKAGE"),
        CLEAR_DATA("adb shell pm clear $AVSB_PACKAGE");

        fun getCommand() = this.command
    }

    override fun execute(command: Command, project: Project): String = runCommand(
        command.getCommand(),
        command.name.lowercase().replaceFirstChar { it.uppercaseChar() },
        project,
        TimeUnit.SECONDS.toMillis(ONE.toLong())
    )

    fun execute(command: String, commandName: String, project: Project, duration: Long): String = runCommand(command, commandName, project, duration)

    private fun runCommand(command: String, commandName: String, project: Project, duration: Long) = try {
//        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c",command))
        val adbPath = AndroidSdkUtils.findAdb(project).adbPath?.path?.replace("adb", "")
        val process = Runtime.getRuntime().exec("$adbPath$command")

        val completed = process.waitFor(duration, TimeUnit.MILLISECONDS)

        val exitValue = process.exitValue()
        println("MGM $completed process=$exitValue path=$adbPath")

        if (completed && exitValue == ZERO) {
            "Command=$commandName executed successfully."
        } else {
            "Command=$commandName failed. exitValue=$exitValue ${process.info()}"
        }
    } catch (e: Exception) {
        "An error occurred while trying to execute command=$command Error was=${e.message}"
    }
}
