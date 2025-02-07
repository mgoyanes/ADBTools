package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ACTIVITY_PREFIX_DELIMITER
import com.mgm.adbtools.DASH
import com.mgm.adbtools.DUMPSYS_ACTIVITY
import com.mgm.adbtools.EMPTY
import com.mgm.adbtools.GET_TOP_ACTIVITY_COMMAND
import com.mgm.adbtools.ONE
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout
import com.mgm.adbtools.extractActivityRegex
import com.mgm.adbtools.extractAppRegex
import com.mgm.adbtools.models.FragmentData

class GetFragmentsCommand : Command<String, List<FragmentData>> {

    companion object {
        private const val DELIMITER_ACTIVE_FRAGMENTS = "Active Fragments:"
        private const val DELIMITER_ADDED_FRAGMENTS = "Added Fragments:"
        private const val DELIMITER_BACK_STACK = "Back Stack"
        private const val DELIMITER_CLOSE_BRACKET = "}"
        private const val DELIMITER_FRAGMENTS_CREATED_MENUS = "Fragments Created Menus:"
        private const val DELIMITER_M_PARENT = "mParent="
        private const val DELIMITER_NAV_HOST_FRAGMENT = "NavHostFragment"
        private const val DELIMITER_OPEN_BRACKET = "{"
        private const val DELIMITER_REQUEST_FRAGMENT = "ReportFragment"
        private const val DELIMITER_SPACE = ": "
        private const val DELIMITER_SUPPORT_REQUEST_MANAGER_FRAGMENT = "SupportRequestManagerFragment"
        private const val DELIMITER_TASK = "TASK"
        private const val EMPTY_CHAR = ' '
    }

    override fun execute(p: String, project: Project, device: IDevice): List<FragmentData> {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout(GET_TOP_ACTIVITY_COMMAND, shellOutputReceiver)

        val output = shellOutputReceiver.toString()
        val appPackage = getAppPackage(output)
        val topActivity = getActivityName(output)

        val fullActivityName: String = if (topActivity.startsWith(ACTIVITY_PREFIX_DELIMITER)) {
            "$appPackage${DASH}$topActivity"
        } else {
            topActivity
        }

        device.executeShellCommandWithTimeout("$DUMPSYS_ACTIVITY -p $appPackage $fullActivityName", shellOutputReceiver)

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

    fun getCurrentFragmentsFromLog(log: String): List<FragmentData> {
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
        var fragment: String
        return bulkTaskDetails
            .lines()
            .let { list ->
                spaceCount = list.firstOrNull()?.indexOfFirst { char -> char != EMPTY_CHAR } ?: -ONE
                list
            }
            .filter { line -> line.indexOfFirst { char -> char != EMPTY_CHAR } == spaceCount && isValidLine(line) }
            .mapNotNull { line ->
                tempLine = line.trim()
                fragment = tempLine.substringBefore(DELIMITER_OPEN_BRACKET, EMPTY)

                if (fragment.isBlank()) {
                    return@mapNotNull null
                }

                FragmentData(fragment = fragment, fragmentIdentifier = getFragmentIdentifier(tempLine))
            }
    }

    private fun getAddedFragmentsDetails(bulkTaskDetails: String): String =
        bulkTaskDetails
            .substringAfterLast(DELIMITER_ADDED_FRAGMENTS, EMPTY)
            .substringBefore(DELIMITER_BACK_STACK, EMPTY)
            .substringBefore(DELIMITER_FRAGMENTS_CREATED_MENUS)

    private fun getAddedFragments(bulkAddedFragmentsDetails: String): List<FragmentData> {
        var fragment: String
        return bulkAddedFragmentsDetails
            .lines()
            .filter { line -> isValidLine(line) }
            .mapNotNull { line ->
                fragment = line.substringAfter(DELIMITER_SPACE, EMPTY).substringBefore(DELIMITER_OPEN_BRACKET, EMPTY)

                if (fragment.isBlank()) {
                    return@mapNotNull null
                }

                FragmentData(fragment = fragment, fragmentIdentifier = getFragmentIdentifier(line))
            }
    }

    private fun getFragmentIdentifier(line: String) = line.substringAfter(DELIMITER_OPEN_BRACKET, EMPTY).substringBefore(DELIMITER_CLOSE_BRACKET, EMPTY)

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

    private fun isValidLine(line: String): Boolean {
        return line.isNotBlank() && !line.contains(DELIMITER_SUPPORT_REQUEST_MANAGER_FRAGMENT) && !line.contains(DELIMITER_REQUEST_FRAGMENT)
    }
}
