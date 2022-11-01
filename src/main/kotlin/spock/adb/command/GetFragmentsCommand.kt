package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ShellOutputReceiver
import spock.adb.models.FragmentData
import java.util.concurrent.TimeUnit

class GetFragmentsCommand : Command<String, List<FragmentData>> {

    companion object {
        private val extractAppRegex = Regex("(A=|I=|u0\\s)([a-zA-Z.]+)")
        private val extractActivityRegex = Regex("(u0\\s[a-zA-Z.]+/)([a-zA-Z.]+)")
        private const val DASH = "/"
        private const val DELIMITER_ACTIVE_FRAGMENTS = "Active Fragments:"
        private const val DELIMITER_ACTIVITY_PREFIX = "."
        private const val DELIMITER_ADDED_FRAGMENTS = "Added Fragments:"
        private const val DELIMITER_BACK_STACK = "Back Stack"
        private const val DELIMITER_CLOSE_BRACKET = "}"
        private const val DELIMITER_M_PARENT = "mParent="
        private const val DELIMITER_NAV_HOST_FRAGMENT = "NavHostFragment"
        private const val DELIMITER_OPEN_BRACKET = "{"
        private const val DELIMITER_SPACE = ": "
        private const val DELIMITER_SUPPORT_REQUEST_MANAGER_FRAGMENT = "SupportRequestManagerFragment"
        private const val DELIMITER_TASK = "TASK"
        private const val EMPTY = ""
        private const val EMPTY_CHAR = ' '
        private const val GET_TOP_ACTIVITY_COMMAND = "dumpsys activity | grep -E \"mResumedActivity|topResumedActivity\""
        private const val ONE = 1
        private const val MAX_TIMEOUT_TO_OBTAIN_RESPONSE = 15L
    }

    override fun execute(p: String, project: Project, device: IDevice): List<FragmentData> {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommand(
            GET_TOP_ACTIVITY_COMMAND,
            shellOutputReceiver,
            MAX_TIMEOUT_TO_OBTAIN_RESPONSE,
            TimeUnit.SECONDS,
        )

        val output = shellOutputReceiver.toString()
        val appPackage = getAppPackage(output)
        val topActivity = getActivityName(output)

        val fullActivityName: String = if (topActivity.startsWith(DELIMITER_ACTIVITY_PREFIX)) {
            "$appPackage$DASH$topActivity"
        } else {
            topActivity
        }

        device.executeShellCommand(
            "dumpsys activity -p $appPackage $fullActivityName",
            shellOutputReceiver,
            MAX_TIMEOUT_TO_OBTAIN_RESPONSE,
            TimeUnit.SECONDS,
        )

        return getCurrentFragmentsFromLog(shellOutputReceiver.toString())
    }

    private fun getAppPackage(bulkAppData: String) =
        extractAppRegex.find(bulkAppData)?.groups?.lastOrNull()?.value ?: EMPTY

    private fun getActivityName(bulkAppData: String): String {
        return extractActivityRegex
            .find(bulkAppData)
            ?.groups
            ?.lastOrNull()
            ?.value
            ?: EMPTY
    }

    private fun getCurrentFragmentsFromLog(log: String): List<FragmentData> {
        val bulkTaskDetails = log.substringAfter(DELIMITER_TASK, EMPTY)
        val bulkAddedFragmentsDetails: String
        val addedFragments: List<FragmentData>

        return if (bulkTaskDetails.contains(DELIMITER_NAV_HOST_FRAGMENT)) {
            bulkAddedFragmentsDetails = getNavHostBulkFragmentDetails(bulkTaskDetails)

            addedFragments = getNavHostAddedFragments(bulkAddedFragmentsDetails)

            getFragments(addedFragments, bulkAddedFragmentsDetails)
        } else {
            bulkAddedFragmentsDetails = getAddedFragmentsDetails(bulkTaskDetails)

            addedFragments = getAddedFragments(bulkAddedFragmentsDetails)

            getFragments(addedFragments, bulkTaskDetails)
        }
    }

    private fun getNavHostBulkFragmentDetails(bulkTaskDetails: String): String =
        bulkTaskDetails
            .substringAfter(DELIMITER_NAV_HOST_FRAGMENT, EMPTY)
            .substringBeforeLast(DELIMITER_ADDED_FRAGMENTS, EMPTY)
            .substringAfter("$DELIMITER_ACTIVE_FRAGMENTS\n")
            .substringBeforeLast("$DELIMITER_ADDED_FRAGMENTS\n")

    private fun getNavHostAddedFragments(bulkTaskDetails: String): List<FragmentData> {
        var spaceCount: Int
        var tempLine: String
        return bulkTaskDetails
            .lines()
            .let { list ->
                spaceCount = list.firstOrNull()?.indexOfFirst { char -> char != EMPTY_CHAR } ?: -ONE
                list
            }
            .filter { line -> line.indexOfFirst { char -> char != EMPTY_CHAR } == spaceCount && isValidLine(line) }
            .map { line ->
                tempLine = line.trim()
                FragmentData(
                    fragment = tempLine.substringBefore(DELIMITER_OPEN_BRACKET, EMPTY),
                    fragmentIdentifier = tempLine.substringAfter(DELIMITER_OPEN_BRACKET, EMPTY).substringBefore(DELIMITER_CLOSE_BRACKET, EMPTY)
                )
            }
    }

    private fun getAddedFragmentsDetails(bulkTaskDetails: String): String =
        bulkTaskDetails
            .substringAfterLast(DELIMITER_ADDED_FRAGMENTS, EMPTY)
            .substringBefore(DELIMITER_BACK_STACK, EMPTY)

    private fun getAddedFragments(bulkAddedFragmentsDetails: String): List<FragmentData> =
        bulkAddedFragmentsDetails
            .lines()
            .filter { line -> isValidLine(line) }
            .map { line ->
                FragmentData(
                    fragment = line.substringAfter(DELIMITER_SPACE, EMPTY).substringBefore(DELIMITER_OPEN_BRACKET, EMPTY),
                    fragmentIdentifier = line.substringAfter(DELIMITER_OPEN_BRACKET, EMPTY).substringBefore(DELIMITER_CLOSE_BRACKET, EMPTY)
                )
            }

    private fun getFragments(addedFragments: List<FragmentData>, bulkTaskDetails: String): List<FragmentData> {
        var initDelimiter: String
        var endDelimiter: String
        addedFragments.forEach { fragment ->
            initDelimiter = "${fragment.fragment}$DELIMITER_OPEN_BRACKET${fragment.fragmentIdentifier}$DELIMITER_CLOSE_BRACKET"
            endDelimiter = "$DELIMITER_M_PARENT$initDelimiter"

            fragment.innerFragments = getAddedFragments(getAddedFragmentsDetails(bulkTaskDetails.substringAfter(initDelimiter, EMPTY).substringBefore(endDelimiter, EMPTY)))
            if (fragment.innerFragments.isNotEmpty()) {
                getFragments(fragment.innerFragments, bulkTaskDetails)
            }
        }

        return addedFragments
    }

    private fun isValidLine(line: String) = line.isNotBlank() && !line.contains(DELIMITER_SUPPORT_REQUEST_MANAGER_FRAGMENT)
}
