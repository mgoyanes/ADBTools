import spock.adb.AVSB_PACKAGE
import spock.adb.ONE
import spock.adb.ZERO
import spock.adb.command.ExecutableCommand
import java.util.concurrent.TimeUnit

class ProcessCommand : ExecutableCommand<ProcessCommand.Command, String> {

    enum class Command(private val command: String) {
        REBOOT("adb reboot"),
        UNINSTALL("adb shell pm uninstall $AVSB_PACKAGE"),
        FORCE_KILL("adb  shell am force-stop $AVSB_PACKAGE"),
        CLEAR_DATA("adb shell pm clear $AVSB_PACKAGE");

        fun getCommand() = this.command
    }

    override fun execute(command: Command): String = runCommand(
        command.getCommand(),
        command.name.lowercase().replaceFirstChar { it.uppercaseChar() },
        TimeUnit.SECONDS.toMillis(ONE.toLong())
    )

    fun execute(command: String, commandName: String, duration: Long): String = runCommand(command, commandName, duration)

    private fun runCommand(command: String, commandName: String, duration: Long) = try {
//        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c",command))
        val process = Runtime.getRuntime().exec(command)

        val completed = process.waitFor(duration, TimeUnit.MILLISECONDS)

        println("MGM $completed process=${process.exitValue()}")

        if (completed && process.exitValue() == ZERO) {
            "Command=$commandName executed successfully."
        } else {
            "Command=$commandName failed"
        }
    } catch (e: Exception) {
        "An error occurred while trying to execute command=$command Error was=${e.message}"
    }
}
