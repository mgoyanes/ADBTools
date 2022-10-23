package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ShellOutputReceiver
import java.util.concurrent.TimeUnit

class GetActivityCommand : Command<Any, String?> {

    companion object {
        private const val GET_TOP_ACTIVITY_COMMAND = "dumpsys activity | grep -E \"mResumedActivity|topResumedActivity\""
        private const val ACTIVITY_PREFIX_DELIMITER = "."
        private val extractAppRegex = Regex("(A=|I=|u0\\s)([a-zA-Z.]+)")
        private val extractActivityRegex = Regex("(u0\\s[a-zA-Z.]+/)([a-zA-Z.]+)")
    }


    override fun execute(p: Any, project: Project, device: IDevice): String? {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommand(
            GET_TOP_ACTIVITY_COMMAND,
            shellOutputReceiver,
            15L,
            TimeUnit.SECONDS
        )

        val appPackage = getAppPackage(shellOutputReceiver.toString())
        appPackage ?: return null

        val topActivity = getActivityName(shellOutputReceiver.toString())
        topActivity ?: return null


        return when {
            topActivity.startsWith(ACTIVITY_PREFIX_DELIMITER) -> "$appPackage$topActivity"
            else -> topActivity
        }
    }

    private fun getActivityName(bulkAppData: String) = extractActivityRegex.find(bulkAppData)?.groups?.lastOrNull()?.value

    private fun getAppPackage(bulkAppData: String) = extractAppRegex.find(bulkAppData)?.groups?.lastOrNull()?.value
}
