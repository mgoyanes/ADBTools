package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.base.util.substringAfterLastOrNull
import spock.adb.ShellOutputReceiver
import spock.adb.models.ActivityData
import spock.adb.models.FragmentData
import java.util.concurrent.TimeUnit

class GetApplicationBackStackCommand : ListCommand<String, List<ActivityData>> {

    companion object {
        private const val INVALID_POS = -1
        private const val ACTIVITY_PREFIX_DELIMITER = "."
        private const val DUMPSYS_ACTIVITY = "dumpsys activity"
        private const val LINE_SEPARATOR = "\n"
        private const val MAX_TIME_TO_OUTPUT_RESPONSE = 15L
        private const val UNKNOWN_COMMAND = "Unknown command"
        val extractActivityRegex = Regex("(ACTIVITY\\s)([a-zA-Z.]+/[a-zA-Z.]+)")
    }

    override fun execute(list: List<String>, project: Project, device: IDevice): List<ActivityData> {
        var shellOutputReceiver: ShellOutputReceiver
        var activityData: List<String>
        var topActivity: String
        var fullActivityName: String
        val activitiesData = mutableListOf<ActivityData>()
        var currentFragmentsFromLog: List<FragmentData>

        list.forEach loop@{ identifier ->
            shellOutputReceiver = ShellOutputReceiver()
            device.executeShellCommand(
                "$DUMPSYS_ACTIVITY $identifier",
                shellOutputReceiver,
                MAX_TIME_TO_OUTPUT_RESPONSE,
                TimeUnit.SECONDS
            )

            if (shellOutputReceiver.toString().startsWith(UNKNOWN_COMMAND)) {
                return@loop
            }

            val activityLog = shellOutputReceiver.toString().lines()
            activityLog.forEach innerLoop@{ line ->
                topActivity = getActivityName(line) ?: return@innerLoop
                fullActivityName = when {
                    topActivity.startsWith(ACTIVITY_PREFIX_DELIMITER) -> "$identifier$topActivity"
                    else -> topActivity
                }

                shellOutputReceiver = ShellOutputReceiver()
                device.executeShellCommand(
                    "$DUMPSYS_ACTIVITY $fullActivityName",
                    shellOutputReceiver,
                    MAX_TIME_TO_OUTPUT_RESPONSE,
                    TimeUnit.SECONDS
                )

                activityData = shellOutputReceiver.toString().lines()
                currentFragmentsFromLog = GetFragmentsCommand().getCurrentFragmentsFromLog(activityData.joinToString(LINE_SEPARATOR))

                val activity = fullActivityName.substringAfterLastOrNull(".")
                activitiesData.add(
                    ActivityData(
                        activity = fullActivityName,
                        activityStackPosition = getStackPosition(device, identifier, activity),
                        isKilled = isKilled(device, identifier, activity),
                        fragment = currentFragmentsFromLog
                    )
                )
            }

            return activitiesData
        }

        return activitiesData
    }

    private fun getActivityName(bulkAppData: String) = extractActivityRegex.find(bulkAppData)?.groups?.lastOrNull()?.value

    private fun getStackPosition(device: IDevice, identifier: String, activity: String?): Int {
        activity ?: return INVALID_POS
        val positionRegex = Regex(".*Hist.*#(\\d+).*")
        val shellOutputReceiver = ShellOutputReceiver()

        device.executeShellCommand(
            "$DUMPSYS_ACTIVITY activities | grep -E \"Hist.*${identifier}\"",
            shellOutputReceiver,
            MAX_TIME_TO_OUTPUT_RESPONSE,
            TimeUnit.SECONDS
        )

        return shellOutputReceiver
            .toString()
            .lines()
            .firstOrNull { value -> value.contains(activity) }
            ?.let { position -> positionRegex.find(position)?.groups?.lastOrNull()?.value?.toIntOrNull() ?: INVALID_POS }
            ?: INVALID_POS
    }

    private fun isKilled(device: IDevice, identifier: String, activity: String?): Boolean {
        activity ?: return true
        val isKilledRegex = Regex(".*pid=(\\d+)")
        val shellOutputReceiver = ShellOutputReceiver()

        device.executeShellCommand(
            "$DUMPSYS_ACTIVITY $identifier | grep ACTIVITY",
            shellOutputReceiver,
            MAX_TIME_TO_OUTPUT_RESPONSE,
            TimeUnit.SECONDS
        )

        return shellOutputReceiver
            .toString()
            .lines()
            .firstOrNull { value -> value.contains(activity) }
            ?.let { pidString -> isKilledRegex.find(pidString)?.groups?.lastOrNull()?.value == null }
            ?: false
    }
}
