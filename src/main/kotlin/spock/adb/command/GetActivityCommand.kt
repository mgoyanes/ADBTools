package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ACTIVITY_PREFIX_DELIMITER
import spock.adb.GET_TOP_ACTIVITY_COMMAND
import spock.adb.ShellOutputReceiver
import spock.adb.executeShellCommandWithTimeout
import spock.adb.extractActivityRegex
import spock.adb.extractAppRegex

class GetActivityCommand : Command<Any, String?> {

    override fun execute(p: Any, project: Project, device: IDevice): String? {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout(GET_TOP_ACTIVITY_COMMAND, shellOutputReceiver)

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
