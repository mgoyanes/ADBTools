package com.mgm.adbtools

import ProcessCommand
import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import org.jetbrains.android.sdk.AndroidSdkUtils
import com.mgm.adbtools.avsb.AVSBAdbController
import com.mgm.adbtools.command.AnimatorDurationScaleCommand
import com.mgm.adbtools.command.ClearAppDataAndRestartCommand
import com.mgm.adbtools.command.ClearAppDataCommand
import com.mgm.adbtools.command.ConnectDeviceOverIPCommand
import com.mgm.adbtools.avsb.DMSCommand
import com.mgm.adbtools.avsb.KeyEventCommand
import com.mgm.adbtools.avsb.OpenStatusCommand
import com.mgm.adbtools.avsb.AppsCommand
import com.mgm.adbtools.avsb.GetAVSBInfoCommand
import com.mgm.adbtools.avsb.InstallApkCommand
import com.mgm.adbtools.avsb.OpenSettingsCommand
import com.mgm.adbtools.command.EnableDisableDarkModeCommand
import com.mgm.adbtools.command.EnableDisableDontKeepActivitiesCommand
import com.mgm.adbtools.command.EnableDisableShowLayoutBoundsCommand
import com.mgm.adbtools.command.EnableDisableShowTapsCommand
import com.mgm.adbtools.command.FirebaseCommand
import com.mgm.adbtools.command.ForceKillAppCommand
import com.mgm.adbtools.command.GetActivityCommand
import com.mgm.adbtools.command.GetApplicationBackStackCommand
import com.mgm.adbtools.command.GetApplicationIDCommand
import com.mgm.adbtools.command.GetApplicationPermission
import com.mgm.adbtools.command.GetBackStackCommand
import com.mgm.adbtools.command.GetFragmentsCommand
import com.mgm.adbtools.command.GetPackageNameCommand
import com.mgm.adbtools.command.GrantPermissionCommand
import com.mgm.adbtools.command.InputOnDeviceCommand
import com.mgm.adbtools.command.Network
import com.mgm.adbtools.command.NetworkRateLimitCommand
import com.mgm.adbtools.command.OpenAccountsCommand
import com.mgm.adbtools.command.OpenAppSettingsCommand
import com.mgm.adbtools.command.OpenDeepLinkCommand
import com.mgm.adbtools.command.OpenDeveloperOptionsCommand
import com.mgm.adbtools.command.ProcessDeathCommand
import com.mgm.adbtools.avsb.ProxyCommand
import com.mgm.adbtools.avsb.TalkbackToggleCommand
import com.mgm.adbtools.command.RestartAppCommand
import com.mgm.adbtools.command.RestartAppWithDebuggerCommand
import com.mgm.adbtools.command.RevokePermissionCommand
import com.mgm.adbtools.command.ToggleAirplaneModeCommand
import com.mgm.adbtools.command.ToggleNetworkCommand
import com.mgm.adbtools.command.TransitionAnimatorScaleCommand
import com.mgm.adbtools.command.UninstallAppCommand
import com.mgm.adbtools.command.WindowAnimatorScaleCommand
import com.mgm.adbtools.models.ActivityData
import com.mgm.adbtools.models.BackStackData
import com.mgm.adbtools.models.FragmentData
import com.mgm.adbtools.notification.CommonNotifier
import com.mgm.adbtools.premission.ListItem
import java.awt.Font
import java.awt.Window
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.ListCellRenderer
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.SwingUtilities


class AdbControllerImp(private val project: Project, private var debugBridge: AndroidDebugBridge?, private var toolWindow: ToolWindow? = null) :
    AdbController,
    AVSBAdbController,
    AndroidDebugBridge.IDeviceChangeListener,
    AndroidDebugBridge.IDebugBridgeChangeListener {

    companion object {
        private const val INDENT = "\t\t\t\t"
        private const val ACTIVITY_KILLED = " [Killed]"
    }

    private var updateDeviceList: ((List<IDevice>) -> Unit)? = null

    init {
        AndroidDebugBridge.addDeviceChangeListener(this)
        AndroidDebugBridge.addDebugBridgeChangeListener(this)
    }

    private fun getApplicationID(device: IDevice) =
        GetApplicationIDCommand().execute(Any(), project, device).toString()

    private fun getPackageName(device: IDevice) = GetPackageNameCommand().execute(Any(), project, device).toString()

    override fun restartBridgeFromScratch() {
        AndroidDebugBridge.terminate()
        debugBridge?.startAdb(Long.MAX_VALUE, TimeUnit.MILLISECONDS)

        AndroidDebugBridge.removeDeviceChangeListener(this)
        AndroidDebugBridge.addDeviceChangeListener(this)
        AndroidDebugBridge.removeDebugBridgeChangeListener(this)
        AndroidDebugBridge.addDebugBridgeChangeListener(this)
    }

    override fun restartBridge() {
        debugBridge?.restart(Long.MAX_VALUE, TimeUnit.MILLISECONDS)

        AndroidDebugBridge.removeDeviceChangeListener(this)
        AndroidDebugBridge.addDeviceChangeListener(this)
        AndroidDebugBridge.removeDebugBridgeChangeListener(this)
        AndroidDebugBridge.addDebugBridgeChangeListener(this)
    }

    override fun terminateAndRestartBridge() {
        AndroidDebugBridge.terminate()
        debugBridge?.restart(Long.MAX_VALUE, TimeUnit.MILLISECONDS)

        AndroidDebugBridge.removeDeviceChangeListener(this)
        AndroidDebugBridge.addDeviceChangeListener(this)
        AndroidDebugBridge.removeDebugBridgeChangeListener(this)
        AndroidDebugBridge.addDebugBridgeChangeListener(this)
    }

    override fun reinitializeBridgeFromSdk() {
        debugBridge = AndroidSdkUtils.getDebugBridge(project)

        AndroidDebugBridge.removeDeviceChangeListener(this)
        AndroidDebugBridge.addDeviceChangeListener(this)
        AndroidDebugBridge.removeDebugBridgeChangeListener(this)
        AndroidDebugBridge.addDebugBridgeChangeListener(this)
    }

    override fun connectedDevices(block: (devices: List<IDevice>) -> Unit) {
        updateDeviceList = block
        updateDeviceList?.invoke(debugBridge?.devices?.toList() ?: listOf())
    }


    //region IDebugBridgeChangeListener
    override fun bridgeChanged(bridge: AndroidDebugBridge?) {
        debugBridge = bridge
        updateDeviceList?.invoke(bridge?.devices?.toList() ?: listOf())
    }
    //endregion

    //region IDeviceChangeListener
    override fun deviceConnected(iDevice: IDevice) {
        updateDeviceList?.invoke(debugBridge?.devices?.toList() ?: listOf())
    }

    override fun deviceDisconnected(iDevice: IDevice) {
        updateDeviceList?.invoke(debugBridge?.devices?.toList() ?: listOf())
    }

    override fun deviceChanged(iDevice: IDevice, i: Int) {}
    //endregion

    override fun currentBackStack(device: IDevice) {
        object : Task.Backgroundable(project, "Fetching back stack", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true

                val activitiesClass: List<BackStackData> = GetBackStackCommand().execute(Any(), project, device)

                val entries = activitiesClass.flatMap { backStackData ->
                    val headerEntry = FragmentPopupEntry(
                        display = backStackData.appPackage,
                        fragment = backStackData.appPackage,
                        isEffectivelyCurrent = false,
                        depth = 0,
                        kind = EntryKind.HEADER
                    )

                    val backStackTotal = backStackData.activitiesList.count { !it.isCurrent }
                    var backStackSeen = 0

                    val activityEntries = backStackData.activitiesList.map { activityData ->
                        val killedSuffix = if (activityData.isKilled) ACTIVITY_KILLED else EMPTY
                        val label = if (activityData.isCurrent) {
                            "↳ Current: ${activityData.activity}$killedSuffix"
                        } else {
                            backStackSeen++
                            "↳ Back stack $backStackSeen/$backStackTotal: ${activityData.activity}$killedSuffix"
                        }

                        FragmentPopupEntry(label, activityData.activity, activityData.isCurrent, depth = 1)
                    }

                    listOf(headerEntry) + activityEntries
                }

                ApplicationManager.getApplication().invokeLater {
                    showFragmentPopup(entries, title = "Activities")
                }
            }
        }.queue()
    }

    override fun currentApplicationBackStack(device: IDevice) {
        val packageName = getPackageName(device)
        val applicationID = getApplicationID(device)

        object : Task.Backgroundable(project, "Fetching back stack", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true

                val backStackData: List<ActivityData> = GetApplicationBackStackCommand().execute(listOf(packageName, applicationID), device)

                val entries = backStackData
                    .sortedByDescending { it.activityStackPosition }
                    .flatMap { activityData ->
                        val activityLabel = "${activityData.activity}${if (activityData.isKilled) ACTIVITY_KILLED else EMPTY}"
                        val activityEntry = FragmentPopupEntry(
                            display = activityLabel,
                            fragment = activityData.activity,
                            isEffectivelyCurrent = false,
                            depth = 0,
                            kind = EntryKind.HEADER
                        )

                        listOf(activityEntry) + buildFragmentEntries(activityData.fragment, depth = 1)
                    }

                ApplicationManager.getApplication().invokeLater {
                    showFragmentPopup(entries, title = "Activities")
                }
            }
        }.queue()
    }

    override fun currentActivity(device: IDevice) {
        execute {
            val activity =
                GetActivityCommand().execute(Any(), project, device) ?: throw Exception("No activities found")
            activity.psiClassByNameFromProject(project)?.openIn(project)
                ?: throw Exception("class $activity  Not Found")
        }
    }

    override fun currentFragment(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)

            val fragmentsClass = GetFragmentsCommand().execute(applicationID, project, device)

            if (fragmentsClass.size > 1 || fragmentsClass.firstOrNull()?.innerFragments?.isNotEmpty() == true) {
                showFragmentPopup(buildFragmentEntries(fragmentsClass))
            } else {
                fragmentsClass
                    .firstOrNull()
                    ?.let {
                        it
                            .fragment
                            .psiClassByNameFromCache(project)
                            ?.openIn(project)
                            ?: throw Exception("Class $it Not Found")
                    }
            }
        }
    }

    override fun forceKillApp(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            ForceKillAppCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID force killed")
        }
    }

    override fun testProcessDeath(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            val activities = ProcessDeathCommand().execute(applicationID, project, device)
            launchActivityOrShowPicker(device, activities, "application $applicationID killed. App launched.")
        }
    }

    override fun restartApp(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            val activities = RestartAppCommand().execute(applicationID, project, device)
            launchActivityOrShowPicker(device, activities, "application $applicationID restarted")
        }
    }

    override fun restartAppWithDebugger(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            val command = RestartAppWithDebuggerCommand()
            val activities = command.execute(applicationID, project, device)
            launchActivityOrShowPicker(device, activities, "application $applicationID restarted with debugger") { activity ->
                command.startWithDebugger(activity, project, device, applicationID)
            }
        }
    }

    override fun clearAppData(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            ClearAppDataCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID data cleared")
        }
    }

    override fun clearAppDataAndRestart(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            val activities = ClearAppDataAndRestartCommand().execute(applicationID, project, device)
            launchActivityOrShowPicker(device, activities, "application $applicationID data cleared and restarted")
        }
    }

    override fun uninstallApp(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            UninstallAppCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID uninstalled")
        }
    }

    override fun getApplicationPermissions(device: IDevice, block: (devices: List<ListItem>) -> Unit) {
        execute {
            val applicationID = getApplicationID(device)
            val permissions = GetApplicationPermission().execute(applicationID, project, device)
            if (permissions.isNotEmpty()) {
                block(permissions)
            } else {
                error("Your Application Doesn't Require any of Runtime Permissions ")
            }
        }
    }

    override fun grantOrRevokeAllPermissions(device: IDevice, permissionOperation: GetApplicationPermission.PermissionOperation) {
        execute {
            val applicationID = getApplicationID(device)
            val permissions = GetApplicationPermission().execute(applicationID, project, device)
            if (permissions.isEmpty()) error("Your Application Doesn't Require any of Runtime Permissions")

            val operation: (ListItem) -> Unit = when (permissionOperation) {
                GetApplicationPermission.PermissionOperation.GRANT ->
                    { permission -> GrantPermissionCommand().execute(applicationID, permission, project, device) }

                GetApplicationPermission.PermissionOperation.REVOKE ->
                    { permission -> RevokePermissionCommand().execute(applicationID, permission, project, device) }
            }

            permissions.forEach { permission -> operation(permission) }
            showSuccess("All permissions ${permissionOperation.operationResult}")
        }
    }

    override fun revokePermission(device: IDevice, listItem: ListItem) {
        execute {
            val applicationID = getApplicationID(device)
            RevokePermissionCommand().execute(applicationID, listItem, project, device)
            showSuccess("permission $listItem revoked")
        }
    }

    override fun grantPermission(device: IDevice, listItem: ListItem) {
        execute {
            val applicationID = getApplicationID(device)
            GrantPermissionCommand().execute(applicationID, listItem, project, device)
            showSuccess("permission $listItem granted")
        }
    }

    override fun connectDeviceOverIp(ip: String) {
        execute {
            ConnectDeviceOverIPCommand().execute(ip, project)
            showSuccess("connected to $ip")
        }
    }

    override fun enableDisableShowTaps(device: IDevice) {
        execute {
            val result = EnableDisableShowTapsCommand().execute(Any(), project, device)
            showSuccess(result)
        }
    }

    override fun enableDisableDontKeepActivities(device: IDevice, onComplete: () -> Unit) {
        object : Task.Backgroundable(project, "Toggling \"Don't keep activities\"", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                execute {
                    val result = EnableDisableDontKeepActivitiesCommand().execute(device)
                    showSuccess(result)
                }
                ApplicationManager.getApplication().invokeLater { onComplete() }
            }
        }.queue()
    }

    override fun enableDisableShowLayoutBounds(device: IDevice) {
        execute {
            val result = EnableDisableShowLayoutBoundsCommand().execute(Any(), project, device)
            showSuccess(result)
        }
    }

    override fun enableDisableDarkMode(device: IDevice) {
        execute {
            val result = EnableDisableDarkModeCommand().execute(Any(), project, device)
            showSuccess(result)
        }
    }

    override fun setWindowAnimatorScale(scale: String, device: IDevice) {
        execute {
            val result = WindowAnimatorScaleCommand().execute(scale, project, device)
            showSuccess(result)
        }
    }

    override fun setTransitionAnimatorScale(scale: String, device: IDevice) {
        execute {
            val result = TransitionAnimatorScaleCommand().execute(scale, project, device)
            showSuccess(result)
        }
    }

    override fun setAnimatorDurationScale(scale: String, device: IDevice) {
        execute {
            val result = AnimatorDurationScaleCommand().execute(scale, project, device)
            showSuccess(result)
        }
    }

    override fun setNetworkRateLimit(scale: String, device: IDevice) {
        execute {
            val result = NetworkRateLimitCommand().execute(scale, project, device)
            showSuccess(result)
        }
    }

    override fun toggleNetwork(device: IDevice, network: Network) {
        execute {
            val result = ToggleNetworkCommand().execute(network, project, device)
            showSuccess(result)
        }
    }

    override fun toggleAirplaneMode(device: IDevice) {
        execute {
            val result = ToggleAirplaneModeCommand().execute(project, device)
            showSuccess(result)
        }
    }

    override fun inputOnDevice(input: String, device: IDevice) {
        execute {
            val result = InputOnDeviceCommand().execute(input, project, device)
            showSuccess(result)
        }
    }

    override fun setDMS(dms: String, device: IDevice) {
        execute {
            val result = DMSCommand().execute(dms, project, device)
            showSuccess(result)
        }
    }

    override fun openStatus(device: IDevice) {
        execute {
            val result = OpenStatusCommand().execute(project, device)
            showSuccess(result)
        }
    }

    override fun openSettings(device: IDevice) {
        execute {
            val result = OpenSettingsCommand().execute(project, device)
            showSuccess(result)
        }
    }

    override fun inputKeyEvent(keyEvent: Int, device: IDevice) {
        execute {
            val result = KeyEventCommand().execute(keyEvent, project, device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun openApp(app: String, device: IDevice) {
        execute {
            val result = AppsCommand().execute(app, AppsCommand.AppAction.OPEN, project, device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun closeApp(app: String, device: IDevice) {
        execute {
            val result = AppsCommand().execute(app, AppsCommand.AppAction.CLOSE, project, device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun processCommand(command: ProcessCommand.Command) {
        execute {
            val result = ProcessCommand().execute(command, project)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun openAVSBAppSettings(device: IDevice) {
        execute {
            showSuccess(com.mgm.adbtools.avsb.OpenAppSettingsCommand().execute(project, device))
        }
    }

    override fun setProxy(hostname: String?, port: String?, device: IDevice) {
        execute {
            val result = ProxyCommand().setProxy(hostname, port, project, device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun clearProxy(device: IDevice) {
        execute {
            val result = ProxyCommand().clearProxy(project, device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun toggleTalkback(device: IDevice) {
        execute {
            val result = TalkbackToggleCommand().execute(device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun copyBoxInfoToClipboard(device: IDevice) {
        execute {
            val result = GetAVSBInfoCommand().execute(device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun installApk(device: IDevice) {
        val desktopPath = System.getProperty("user.home") + File.separator + "Desktop"
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Select an APK File"
            fileSelectionMode = JFileChooser.FILES_ONLY
            currentDirectory = File(desktopPath)
        }

        val parentWindow: Window? = SwingUtilities.getWindowAncestor(toolWindow?.component)

        val dialogResult = fileChooser.showOpenDialog(parentWindow)

        if (dialogResult == JFileChooser.APPROVE_OPTION) {
            val selectedFile: File = fileChooser.selectedFile

            if (!selectedFile.name.endsWith(".apk", ignoreCase = true)) {
                JOptionPane.showMessageDialog(parentWindow, "Error: Selected file is not an APK!", "Invalid File", JOptionPane.ERROR_MESSAGE)
                return
            }

            execute {
                showSuccess("Please wait while app is being installed")
                val result = InstallApkCommand().execute(selectedFile.absolutePath, device)
                showSuccess(result)
            }
        }
    }

    private fun showError(message: String) {
        CommonNotifier.showNotifier(project = project, content = message, type = NotificationType.ERROR)
    }

    private fun showSuccess(message: String) {
        CommonNotifier.showNotifier(project = project, content = message, type = NotificationType.INFORMATION)
    }

    private fun execute(execute: () -> Unit) {
        try {
            execute.invoke()
        } catch (e: Exception) {
            showError(e.localizedMessage ?: e.javaClass.simpleName)
        }
    }

    private enum class EntryKind { HEADER, TRACKED }

    private data class FragmentPopupEntry(
        val display: String,
        val fragment: String,
        val isEffectivelyCurrent: Boolean,
        val depth: Int,
        val kind: EntryKind = EntryKind.TRACKED
    )

    private fun buildFragmentEntries(fragments: List<FragmentData>, depth: Int = 0, ancestorsAdded: Boolean = true): List<FragmentPopupEntry> {
        val backStackTotal = fragments.count { !it.isAdded }
        var backStackSeen = 0

        return fragments.flatMap { fragmentData ->
            val prefix = if (depth > 0) "↳ " else EMPTY
            val isEffectivelyCurrent = fragmentData.isAdded && ancestorsAdded

            val label = when {
                isEffectivelyCurrent -> "$prefix Current: ${fragmentData.fragment}"
                fragmentData.isAdded -> "$prefix Back stack (stale parent): ${fragmentData.fragment}"
                else -> {
                    backStackSeen++
                    "$prefix Back stack $backStackSeen/$backStackTotal: ${fragmentData.fragment}"
                }
            }

            listOf(FragmentPopupEntry(label, fragmentData.fragment, isEffectivelyCurrent, depth)) +
                buildFragmentEntries(fragmentData.innerFragments, depth + 1, ancestorsAdded = isEffectivelyCurrent)
        }
    }

    private fun showFragmentPopup(entries: List<FragmentPopupEntry>, title: String = "Fragments") {
        val list = JBList(entries.map { it.display })
        list.cellRenderer = ListCellRenderer { _, _, index, _, _ ->
            val entry = entries[index]
            JBLabel(entry.display).apply {
                border = JBUI.Borders.empty(5, 5 + entry.depth * 16, 5, 20)
                when {
                    entry.kind == EntryKind.HEADER -> {
                        icon = AllIcons.Nodes.Class
                        font = font.deriveFont(Font.BOLD)
                    }
                    entry.isEffectivelyCurrent -> {
                        icon = AllIcons.Actions.Commit
                        font = font.deriveFont(Font.BOLD)
                    }
                    else -> {
                        icon = AllIcons.Vcs.History
                        font = font.deriveFont(Font.ITALIC)
                        foreground = JBColor.GRAY
                    }
                }
            }
        }

        val selectedIndex = entries.indexOfLast { it.isEffectivelyCurrent }
        if (selectedIndex >= 0) list.selectedIndex = selectedIndex

        PopupChooserBuilder(list).apply {
            setTitle(title)
            setItemChosenCallback(Runnable {
                entries.getOrNull(list.selectedIndex)?.let { entry ->
                    val psiClass = if (entry.fragment.contains(DOT)) {
                        entry.fragment.psiClassByNameFromProject(project)
                    } else {
                        entry.fragment.psiClassByNameFromCache(project)
                    }
                    psiClass?.openIn(project)
                }
            })
            createPopup().showCenteredInCurrentWindow(project)
        }
    }

    override fun openDeveloperOptions(device: IDevice) {
        execute {
            showSuccess(OpenDeveloperOptionsCommand().execute(device))
        }
    }

    override fun openDeepLink(input: String, device: IDevice) {
        execute {
            val result = OpenDeepLinkCommand().execute(input, project, device)
            showSuccess(result)
        }
    }

    override fun openAccounts(device: IDevice) {
        execute {
            showSuccess(OpenAccountsCommand().execute(device))
        }
    }

    override fun openAppSettings(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            showSuccess(OpenAppSettingsCommand().execute(applicationID, project, device))
        }
    }

    override fun setFirebaseDebugApp(device: IDevice, firebaseDebugApp: String) {
        execute {
            FirebaseCommand().execute(getApplicationID(device), firebaseDebugApp, project, device)
        }
    }

    private fun launchActivityOrShowPicker(
        device: IDevice,
        activities: List<String>,
        successMessage: String,
        onActivitySelected: (String) -> Unit = { device.startActivity(it) }
    ) {
        if (activities.size == 1) {
            onActivitySelected(activities.first())
            showSuccess(successMessage)
        } else {
            val list = JBList(activities)
            list.cellRenderer = ListCellRenderer { _, value, _, _, _ ->
                val label = JBLabel(value)
                label.border = JBUI.Borders.empty(5, 10, 5, 20)
                label
            }
            PopupChooserBuilder(list).apply {
                setTitle("Select Launcher Activity")
                setItemChosenCallback(Runnable {
                    activities.getOrNull(list.selectedIndex)?.let { activity ->
                        execute {
                            onActivitySelected(activity)
                            showSuccess(successMessage)
                        }
                    }
                })
                createPopup().showCenteredInCurrentWindow(project)
            }
        }
    }
}
