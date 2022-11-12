package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ShellOutputReceiver
import spock.adb.models.ActivityData
import spock.adb.models.FragmentData
import java.util.concurrent.TimeUnit

class GetApplicationBackStackCommand : Command<String, List<ActivityData>> {

    companion object {
        private const val ACTIVITY_PREFIX_DELIMITER = "."
        private const val LINE_SEPARATOR = "\n"
        private const val GET_ALTERNATIVE_ACTIVITIES_COMMAND_FILTER = "grep [[:blank:]]Hist #"
        val extractActivityRegex = Regex("(u0\\s[a-zA-Z.]+/)([a-zA-Z.]+)")
    }

    override fun execute(p: String, project: Project, device: IDevice): List<ActivityData> {
        val shellOutputReceiver = ShellOutputReceiver()
        var activityData: List<String>
        var topActivity: String
        var fullActivityName: String
        val activitiesData = mutableListOf<ActivityData>()
        var currentFragmentsFromLog: List<FragmentData>

        device.executeShellCommand(
            "dumpsys activity -p $p | $GET_ALTERNATIVE_ACTIVITIES_COMMAND_FILTER",
            shellOutputReceiver,
            15L,
            TimeUnit.SECONDS
        )

        val activityLog = shellOutputReceiver.toString().lines()
        activityLog.forEach { line ->
            topActivity = getActivityName(line) ?: return mutableListOf()
            fullActivityName = when {
                topActivity.startsWith(ACTIVITY_PREFIX_DELIMITER) -> "$p$topActivity"
                else -> topActivity
            }

            device.executeShellCommand(
                "dumpsys activity -p $p $fullActivityName",
                shellOutputReceiver,
                15L,
                TimeUnit.SECONDS
            )

            activityData = shellOutputReceiver.toString().lines()
            currentFragmentsFromLog = GetFragmentsCommand().getCurrentFragmentsFromLog(activityData.joinToString(LINE_SEPARATOR))

            activitiesData.add(ActivityData(activity = fullActivityName, fragment = currentFragmentsFromLog))
        }

        return activitiesData
    }

    private fun getActivityName(bulkAppData: String) = extractActivityRegex.find(bulkAppData)?.groups?.lastOrNull()?.value
}
