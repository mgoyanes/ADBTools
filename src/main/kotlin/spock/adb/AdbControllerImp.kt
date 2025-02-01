package spock.adb

import ProcessCommand
import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.psi.PsiClass
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import org.jetbrains.android.sdk.AndroidSdkUtils
import spock.adb.avsb.AVSBAdbController
import spock.adb.command.AnimatorDurationScaleCommand
import spock.adb.command.ClearAppDataAndRestartCommand
import spock.adb.command.ClearAppDataCommand
import spock.adb.command.ConnectDeviceOverIPCommand
import spock.adb.avsb.DMSCommand
import spock.adb.avsb.KeyEventCommand
import spock.adb.avsb.OpenStatusCommand
import spock.adb.avsb.AppsCommand
import spock.adb.avsb.OpenSettingsCommand
import spock.adb.command.EnableDisableDarkModeCommand
import spock.adb.command.EnableDisableShowLayoutBoundsCommand
import spock.adb.command.EnableDisableShowTapsCommand
import spock.adb.command.FirebaseCommand
import spock.adb.command.ForceKillAppCommand
import spock.adb.command.GetActivityCommand
import spock.adb.command.GetApplicationBackStackCommand
import spock.adb.command.GetApplicationIDCommand
import spock.adb.command.GetApplicationPermission
import spock.adb.command.GetBackStackCommand
import spock.adb.command.GetFragmentsCommand
import spock.adb.command.GetPackageNameCommand
import spock.adb.command.GrantPermissionCommand
import spock.adb.command.InputOnDeviceCommand
import spock.adb.command.Network
import spock.adb.command.NetworkRateLimitCommand
import spock.adb.command.OpenAccountsCommand
import spock.adb.command.OpenAppSettingsCommand
import spock.adb.command.OpenDeepLinkCommand
import spock.adb.command.OpenDeveloperOptionsCommand
import spock.adb.command.ProcessDeathCommand
import spock.adb.avsb.ProxyCommand
import spock.adb.command.RestartAppCommand
import spock.adb.command.RestartAppWithDebuggerCommand
import spock.adb.command.RevokePermissionCommand
import spock.adb.command.ToggleNetworkCommand
import spock.adb.command.TransitionAnimatorScaleCommand
import spock.adb.command.UninstallAppCommand
import spock.adb.command.WindowAnimatorScaleCommand
import spock.adb.models.ActivityData
import spock.adb.models.BackStackData
import spock.adb.models.FragmentData
import spock.adb.notification.CommonNotifier
import spock.adb.premission.ListItem
import java.util.concurrent.TimeUnit
import kotlin.math.max


class AdbControllerImp(private val project: Project, private var debugBridge: AndroidDebugBridge?) :
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
    }

    private fun getApplicationID(device: IDevice) =
        GetApplicationIDCommand().execute(Any(), project, device).toString()

    private fun getPackageName(device: IDevice) = GetPackageNameCommand().execute(Any(), project, device).toString()

    override fun refresh() {
        AndroidDebugBridge.terminate()
        debugBridge?.startAdb(Long.MAX_VALUE, TimeUnit.MILLISECONDS)

        AndroidDebugBridge.removeDeviceChangeListener(this)
        AndroidDebugBridge.addDeviceChangeListener(this)
    }

    override fun refresh2() {
        debugBridge?.restart(Long.MAX_VALUE, TimeUnit.MILLISECONDS)

        AndroidDebugBridge.removeDeviceChangeListener(this)
        AndroidDebugBridge.addDeviceChangeListener(this)
    }

    override fun refresh3() {
        AndroidDebugBridge.terminate()
        debugBridge?.restart(Long.MAX_VALUE, TimeUnit.MILLISECONDS)

        AndroidDebugBridge.removeDeviceChangeListener(this)
        AndroidDebugBridge.addDeviceChangeListener(this)
    }

    override fun refresh4() {
        debugBridge = AndroidSdkUtils.getDebugBridge(project)

        AndroidDebugBridge.removeDeviceChangeListener(this)
        AndroidDebugBridge.addDeviceChangeListener(this)
    }

    override fun connectedDevices(block: (devices: List<IDevice>) -> Unit) {
        updateDeviceList = block
        updateDeviceList?.invoke(debugBridge?.devices?.toList() ?: listOf())
    }


    //region IDebugBridgeChangeListener
    override fun bridgeChanged(bridge: AndroidDebugBridge?) {
        debugBridge = bridge

        showSuccess("bridgeChanged")
    }

    override fun restartInitiated() {
        super.restartInitiated()
        showSuccess("restartInitiated")
    }

    override fun restartCompleted(isSuccessful: Boolean) {
        super.restartCompleted(isSuccessful)
        showSuccess("restartCompleted isSuccessful=$isSuccessful")
    }

    override fun initializationError(exception: java.lang.Exception?) {
        super.initializationError(exception)
        showError("initializationError. Error was=${exception?.message}")
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
        val activitiesList = mutableListOf<String>()
        val activitiesClass: List<BackStackData> = GetBackStackCommand().execute(Any(), project, device)

        activitiesClass.forEachIndexed { index, activityData ->
            activitiesList.add("\t[$index]-${activityData.appPackage}")

            activityData.activitiesList.forEachIndexed { activityIndex, activityData ->
                activitiesList.add("\t\t\t\t[$activityIndex]-${activityData.activity}${if (activityData.isKilled) ACTIVITY_KILLED else EMPTY}")
            }
        }

        val list = JBList(activitiesList)
        showClassPopup(
            "Activities",
            list,
            activitiesList.map { it.trim().replace(ACTIVITY_KILLED, EMPTY).substringAfter(HYPHEN).psiClassByNameFromProjct(project) }
        )
    }

    override fun currentApplicationBackStack(device: IDevice) {
        val packageName = getPackageName(device)
        val applicationID = getApplicationID(device)
        val backStackList = mutableMapOf<String, Int>()
        val backStackData: List<ActivityData> = GetApplicationBackStackCommand().execute(listOf(packageName, applicationID), device)

        backStackData
            .sortedByDescending { it.activityStackPosition }
            .forEachIndexed { index, activityData ->
                backStackList[activityData.activity] = index

                activityData.fragment.forEachIndexed { fragmentIndex, fragmentData ->
                    backStackList[fragmentData.fragment] = fragmentIndex

                    addInnerFragmentsToList(fragmentData = fragmentData, fragmentsList = backStackList, indent = INDENT, includeIndex = false)
                }
            }

        val list = JBList(backStackList.keys.toList())
        var margin: Int
        list.installCellRenderer { o: Any ->
            val displayTitle: String
            val title = o.toString()
            displayTitle = if (title.contains(DOT)) {
                margin = 10
                StringBuilder().insert(ZERO, "[${backStackList[title]}]-").append(
                    (title.split(DOT).lastOrNull() ?: EMPTY) + " [Activity]${if (backStackData.firstOrNull { it.activity == title }?.isKilled == true) ACTIVITY_KILLED else EMPTY}"
                ).toString()
            } else {
                margin = 20
                StringBuilder(title).insert(max(ZERO, title.indexOfLast { char -> char == TAB }), "[${backStackList[title]}]-").append(" [Fragment]").toString()
            }

            val label = JBLabel(displayTitle)
            label.border = JBUI.Borders.empty(5, margin, 5, 20)
            label
        }
        PopupChooserBuilder(list).apply {
            this.setTitle("Activities")
            this.setItemChoosenCallback {
                val current = backStackList.keys.elementAtOrNull(list.selectedIndex)
                current?.let {
                    if (it.contains(DASH))
                        it.trim().replace(ACTIVITY_KILLED, EMPTY).replaceFirst(DASH.toString(), EMPTY).psiClassByNameFromProjct(project)?.openIn(project)
                    else
                        it.trim().psiClassByNameFromCache(project)?.openIn(project)
                }
            }
            this.createPopup().showCenteredInCurrentWindow(project)
        }
    }

    override fun currentActivity(device: IDevice) {
        execute {
            val activity =
                GetActivityCommand().execute(Any(), project, device) ?: throw Exception("No activities found")
            activity.psiClassByNameFromProjct(project)?.openIn(project)
                ?: throw Exception("class $activity  Not Found")
        }
    }

    override fun currentFragment(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)

            val fragmentsClass = GetFragmentsCommand().execute(applicationID, project, device)

            if (fragmentsClass.size > 1) {
                val fragmentsList = mutableMapOf<String, Int>()

                fragmentsClass.forEachIndexed { index, fragmentData ->
                    fragmentsList["\t[$index]-${fragmentData.fragment}"] = index

                    addInnerFragmentsToList(fragmentData = fragmentData, fragmentsList = fragmentsList, indent = INDENT, includeIndex = true)
                }

                val list = JBList(fragmentsList.keys.toList())
                showClassPopup(
                    "Fragments",
                    list,
                    fragmentsList.map { it.key.trim().substringAfter(HYPHEN).psiClassByNameFromCache(project) }
                )
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
            ProcessDeathCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID killed. App launched.")
        }
    }

    override fun restartApp(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            RestartAppCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID Restart")
        }
    }

    override fun restartAppWithDebugger(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            RestartAppWithDebuggerCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID Restarted with debugger")
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
            ClearAppDataAndRestartCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID data cleared and restarted")
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
        getApplicationPermissions(device) { permissionsList ->
            val applicationID = getApplicationID(device)

            val operation: (ListItem) -> Unit = when (permissionOperation) {
                GetApplicationPermission.PermissionOperation.GRANT ->
                    { permission -> GrantPermissionCommand().execute(applicationID, permission, project, device) }

                GetApplicationPermission.PermissionOperation.REVOKE ->
                    { permission -> RevokePermissionCommand().execute(applicationID, permission, project, device) }
            }

            permissionsList
                .forEach { permission -> operation(permission) }
                .also { showSuccess("All permissions ${permissionOperation.operationResult}") }
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

            if (result != EMPTY) {
                showSuccess(result)
            }
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
            val result = ProcessCommand().execute(command)
        }
    }

    override fun openAVSBAppSettings(device: IDevice) {
        execute {
            showSuccess(spock.adb.avsb.OpenAppSettingsCommand().execute(project, device))
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
            showError(e.message ?: "not found")
        }
    }

    private fun showClassPopup(
        title: String,
        list: JBList<String>,
        classes: List<PsiClass?>
    ) {
        list.installCellRenderer { displayTitle ->
            val label = JBLabel(displayTitle)
            label.border = JBUI.Borders.empty(5, 5, 5, 20)
            label
        }

        PopupChooserBuilder(list).apply {
            this.setTitle(title)
            this.setItemChoosenCallback {
                classes.getOrNull(list.selectedIndex)?.openIn(project)
            }
            this.createPopup().showCenteredInCurrentWindow(project)
        }
    }

    private fun addInnerFragmentsToList(
        fragmentData: FragmentData,
        fragmentsList: MutableMap<String, Int>,
        indent: String,
        includeIndex: Boolean,
    ) {
        fragmentData.innerFragments.forEachIndexed { fragmentIndex, innerFragmentData ->
            fragmentsList[
                if (includeIndex) {
                    "$indent[$fragmentIndex]-${innerFragmentData.fragment}"
                } else {
                    "$indent${innerFragmentData.fragment}"
                }
            ] = fragmentIndex
            addInnerFragmentsToList(innerFragmentData, fragmentsList, "$INDENT$indent", includeIndex)
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
}
