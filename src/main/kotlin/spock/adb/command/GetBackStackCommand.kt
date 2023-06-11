package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ACTIVITY_PREFIX_DELIMITER
import spock.adb.DUMPSYS_ACTIVITY
import spock.adb.EMPTY
import spock.adb.ShellOutputReceiver
import spock.adb.executeShellCommandWithTimeout
import spock.adb.models.ActivityData
import spock.adb.models.BackStackData

class GetBackStackCommand : Command<Any, List<BackStackData>> {

    companion object {
        const val HIST_PREFIX = "* Hist"
        val extractAppRegex = Regex("(A=|I=|u0\\s)([a-zA-Z.\\d]+)")
        val extractActivityRegex = Regex("(u0\\s[a-zA-Z.\\d]+/)([a-zA-Z.\\d]+)")
    }

    override fun execute(p: Any, project: Project, device: IDevice): List<BackStackData> {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("$DUMPSYS_ACTIVITY activities | grep Hist", shellOutputReceiver)
        return getCurrentRunningActivitiesAboveApi11(device, shellOutputReceiver.toString())
    }

    private fun getCurrentRunningActivitiesAboveApi11(device: IDevice, bulkActivitiesData: String): List<BackStackData> {
        lateinit var appPackage: String
        lateinit var activity: String

        return bulkActivitiesData
            .lines()
            .filter { line -> line.trim().startsWith(HIST_PREFIX) }
            .groupBy(
                keySelector = { line ->
                    appPackage = extractAppRegex.find(line)?.groups?.lastOrNull()?.value ?: EMPTY
                    appPackage
                },
                valueTransform = { bulkActivityData ->
                    activity = extractActivityRegex.find(bulkActivityData)?.groups?.lastOrNull()?.value
                        ?.let { activityName ->
                            when {
                                activityName.startsWith(ACTIVITY_PREFIX_DELIMITER) -> "$appPackage$activityName"
                                else -> activityName
                            }
                        }
                        ?: EMPTY

                    ActivityData(activity = activity, isKilled = isKilled(device, activity))
                }
            )
            .filter { entry -> entry.key.isNotBlank() }
            .map { activityData -> BackStackData(activityData.key, activityData.value) }
    }

    private fun isKilled(device: IDevice, activity: String?): Boolean {
        activity ?: return true
        val isKilledRegex = Regex(".*pid=(\\d+)")
        val shellOutputReceiver = ShellOutputReceiver()

        device.executeShellCommandWithTimeout("$DUMPSYS_ACTIVITY $activity | grep ACTIVITY", shellOutputReceiver)

        return shellOutputReceiver
            .toString()
            .let { pidString -> isKilledRegex.find(pidString)?.groups?.lastOrNull()?.value == null }
    }
}
