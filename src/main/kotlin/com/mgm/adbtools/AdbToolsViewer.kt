package com.mgm.adbtools

import ProcessCommand
import com.android.ddmlib.IDevice
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.mgm.adbtools.avsb.AVSBAdbController
import com.mgm.adbtools.command.AnimatorDurationScaleCommand
import com.mgm.adbtools.avsb.DMSCommand
import com.mgm.adbtools.avsb.KeyEventCommand
import com.mgm.adbtools.command.DontKeepActivitiesState
import com.mgm.adbtools.command.EnableDarkModeState
import com.mgm.adbtools.command.FirebaseCommand
import com.mgm.adbtools.command.GetApplicationPermission
import com.mgm.adbtools.command.Network
import com.mgm.adbtools.command.NetworkRateLimitCommand
import com.mgm.adbtools.command.ShowLayoutBoundsState
import com.mgm.adbtools.command.ShowTapsState
import com.mgm.adbtools.command.TransitionAnimatorScaleCommand
import com.mgm.adbtools.command.WindowAnimatorScaleCommand
import com.mgm.adbtools.premission.CheckBoxDialog
import com.mgm.adbtools.premission.ListItem
import java.awt.event.ActionEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField

class AdbToolsViewer(private val project: Project) : SimpleToolWindowPanel(true) {
    private lateinit var rootPanel: JPanel
    private lateinit var permissionPanel: JPanel
    private lateinit var networkPanel: JPanel
    private lateinit var developerPanel: JPanel
    private lateinit var avsbPanel: JPanel
    private lateinit var devicesListComboBox: JComboBox<String>
    private lateinit var currentActivityButton: JButton
    private lateinit var currentFragmentButton: JButton
    private lateinit var clearAppDataButton: JButton
    private lateinit var clearAppDataAndRestartButton: JButton
    private lateinit var uninstallAppButton: JButton
    private lateinit var refresh: JButton
    private lateinit var refresh2: JButton
    private lateinit var refresh3: JButton
    private lateinit var refresh4: JButton
    private lateinit var permissionButton: JButton
    private lateinit var grantAllPermissionsButton: JButton
    private lateinit var revokeAllPermissionsButton: JButton
    private lateinit var restartAppButton: JButton
    private lateinit var restartAppWithDebuggerButton: JButton
    private lateinit var forceKillAppButton: JButton
    private lateinit var testProcessDeathButton: JButton
    private lateinit var activitiesBackStackButton: JButton
    private lateinit var currentAppBackStackButton: JButton
    private lateinit var adbWifi: JButton
    private lateinit var setting: JButton
    private lateinit var devices: List<IDevice>
    private lateinit var enableDisableDontKeepActivities: JCheckBox
    private lateinit var enableDisableShowTaps: JCheckBox
    private lateinit var enableDisableShowLayoutBounds: JCheckBox
    private lateinit var enableDisableDarkMode: JCheckBox
    private lateinit var windowAnimatorScaleComboBox: JComboBox<String>
    private lateinit var transitionAnimatorScaleComboBox: JComboBox<String>
    private lateinit var animatorDurationScaleComboBox: JComboBox<String>
    private lateinit var networkRateLimitComboBox: JComboBox<String>
    private lateinit var wifiToggle: JButton
    private lateinit var mobileDataToggle: JButton
    private lateinit var inputOnDeviceTextField: JTextField
    private lateinit var openDeepLinkTextField: JTextField
    private lateinit var inputOnDeviceButton: JButton
    private lateinit var openDeepLinkButton: JButton
    private lateinit var openDeveloperOptionsButton: JButton
    private lateinit var openAccountsButton: JButton
    private lateinit var openAppSettingsButton: JButton
    private lateinit var firebaseButton: JButton
    private lateinit var firebaseTextField: JTextField
    private var selectedIDevice: IDevice? = null
    private lateinit var dmsComboBox: JComboBox<String>
    private lateinit var avsbOpenStatus: JButton
    private lateinit var avsbOpenSettings: JButton
    private lateinit var avsbEPG: JButton
    private lateinit var avsbBack: JButton
    private lateinit var avsbExit: JButton
    private lateinit var avsbReboot: JButton
    private lateinit var avsbUninstall: JButton
    private lateinit var avsbForceKill: JButton
    private lateinit var avsbClearData: JButton
    private lateinit var avsbPower: JButton
    private lateinit var avsbHome: JButton
    private lateinit var avsbSearch: JButton
    private lateinit var avsbAllApps: JButton
    private lateinit var avsbAppsOpen: JButton
    private lateinit var avsbAppsClose: JButton
    private lateinit var avsbAppsComboBox: JComboBox<String>
    private lateinit var avsbAppSettingsButton: JButton
    private lateinit var avsbProxySet: JButton
    private lateinit var avsbProxyNone: JButton
    private lateinit var avsbProxyHostname: JTextField
    private lateinit var avsbProxyPort: JTextField
    private lateinit var avsbTalkback: JButton
    private lateinit var avsbCopyBoxInfo: JButton
    private lateinit var avsbInstallAPK: JButton
    private lateinit var adbController: AdbController
    private val appSettingsService by lazy { service<AppSettingsService>() }

    private val showTapsActionListener: (ActionEvent) -> Unit = {
        executeAction { device ->
            adbController.enableDisableShowTaps(device)
        }
    }

    private val showLayoutBoundsActionListener: (ActionEvent) -> Unit = {
        executeAction { device ->
            adbController.enableDisableShowLayoutBounds(device)
            device.refreshUi()
        }
    }

    private val showDarkModeActionListener: (ActionEvent) -> Unit = {
        executeAction { device ->
            adbController.enableDisableDarkMode(device)
            device.refreshUi()
        }
    }

    private val windowAnimatorScaleActionListener: (ActionEvent) -> Unit = {
        executeAction { device ->
            adbController.setWindowAnimatorScale(
                windowAnimatorScaleComboBox.selectedItem as String,
                device
            )
        }
    }

    private val transitionAnimatorScaleActionListener: (ActionEvent) -> Unit = {
        executeAction { device ->
            adbController.setTransitionAnimatorScale(
                transitionAnimatorScaleComboBox.selectedItem as String,
                device

            )
        }
    }

    private val animatorDurationScaleActionListener: (ActionEvent) -> Unit = {
        executeAction { device ->
            adbController.setAnimatorDurationScale(
                animatorDurationScaleComboBox.selectedItem as String,
                device
            )
        }
    }

    private val networkRateLimitActionListener: (ActionEvent) -> Unit = {
        executeAction { device ->
            adbController.setNetworkRateLimit(
                networkRateLimitComboBox.selectedItem as String,
                device
            )
        }
    }

    private val dmsActionListener: (ActionEvent) -> Unit = {
        executeAction { device ->
            (adbController as AVSBAdbController). setDMS(
                dmsComboBox.selectedItem as String,
                device
            )

            CoroutineScope(Dispatchers.IO)
                .launch {
                    delay(2000)
                    val receiver = ShellOutputReceiver()
                    device.executeShellCommandWithTimeout("pm clear $AVSB_PACKAGE ~", receiver, NO_TIME_TO_OUTPUT_RESPONSE)
                }
        }
    }

    private val _toolwindowState = MutableStateFlow(TollVisibilityWindowState(ToolWindowState.UNKNOWN, false))

    init {
        setContent(JScrollPane(rootPanel))
        setToolWindowListener()
        updateUi(appSettingsService.state)

        _toolwindowState
            .filter { windowState -> windowState.state == ToolWindowState.VISIBLE && windowState.isVisible }
            .onEach {
                removeListeners()
                setPredefinedValues()
                setListeners()
            }
            .launchIn(CoroutineScope(Dispatchers.Unconfined))
    }

    fun initPlugin(adbController: AdbController) {
        this.adbController = adbController

        updateDevicesList()

        setting.isEnabled = true
        setting.isVisible = true
        setting.addActionListener {
            val appSettings = appSettingsService.state

            val dialog = CheckBoxDialog(appSettings.getAllAvailableAppSettings()) { selectedItem: ListItem ->

                val updatedSettings = appSettings.settings.mapValues { (action, isSelected) ->
                    if (action.displayName == selectedItem.name) selectedItem.isSelected else isSelected
                }

                appSettings.settings = updatedSettings

                updateUi(appSettings)
            }
            dialog.apply {
                setLocationRelativeTo(WindowManager.getInstance().getFrame(project))
                pack()
                isVisible = true
            }
        }

        adbWifi.addActionListener {
            val ip = Messages.showInputDialog(
                "Enter your android device IP address",
                "Device Connect Over WiFi",
                null,
                EMPTY,
                IPAddressInputValidator()
            )
            ip?.let { adbController.connectDeviceOverIp(ip = ip) }
        }

        refresh.isVisible = false
        refresh.addActionListener {
            adbController.refresh()
            updateDevicesList()
        }

        refresh2.addActionListener {
            adbController.refresh2()
            updateDevicesList()
        }

        refresh3.isVisible = false
        refresh3.addActionListener {
            adbController.refresh3()
            updateDevicesList()
        }

        refresh4.isVisible = false
        refresh4.addActionListener {
            adbController.refresh4()
            updateDevicesList()
        }

        devicesListComboBox.addItemListener {
            selectedIDevice = devices[devicesListComboBox.selectedIndex]

        }

        activitiesBackStackButton.addActionListener {
            executeAction { device ->
                adbController.currentBackStack(device)
            }
        }

        currentAppBackStackButton.addActionListener {
            executeAction { device ->
                adbController.currentApplicationBackStack(device)
            }
        }

        currentActivityButton.addActionListener {
            executeAction { device ->
                adbController.currentActivity(device)
            }
        }

        currentFragmentButton.addActionListener {
            executeAction { device ->
                adbController.currentFragment(device)
            }
        }

        restartAppButton.addActionListener {
            executeAction { device ->
                adbController.restartApp(device)
            }
        }

        restartAppWithDebuggerButton.addActionListener {
            executeAction { device ->
                adbController.restartAppWithDebugger(device)
            }
        }

        forceKillAppButton.addActionListener {
            executeAction { device ->
                adbController.forceKillApp(device)
            }
        }

        testProcessDeathButton.addActionListener {
            executeAction { device ->
                adbController.testProcessDeath(device)
            }
        }

        clearAppDataButton.addActionListener {
            executeAction { device ->
                adbController.clearAppData(device)
            }
        }

        clearAppDataAndRestartButton.addActionListener {
            executeAction { device ->
                adbController.clearAppDataAndRestart(device)
            }
        }

        uninstallAppButton.addActionListener {
            executeAction { device ->
                adbController.uninstallApp(device)
            }
        }

        permissionButton.addActionListener {
            executeAction { device ->
                adbController.getApplicationPermissions(device) { list ->
                    val dialog = CheckBoxDialog(list) { selectedItem ->
                        if (selectedItem.isSelected)
                            adbController.grantPermission(device, selectedItem)
                        else
                            adbController.revokePermission(device, selectedItem)
                    }
                    dialog.setLocationRelativeTo(WindowManager.getInstance().getFrame(project))
                    dialog.pack()
                    dialog.isVisible = true

                }
            }
        }

        grantAllPermissionsButton.addActionListener {
            executeAction { device ->
                adbController.grantOrRevokeAllPermissions(device, GetApplicationPermission.PermissionOperation.GRANT)
            }
        }

        revokeAllPermissionsButton.addActionListener {
            executeAction { device ->
                adbController.grantOrRevokeAllPermissions(device, GetApplicationPermission.PermissionOperation.REVOKE)
            }
        }

        wifiToggle.addActionListener {
            executeAction { device ->
                adbController.toggleNetwork(device, Network.WIFI)
            }
        }

        mobileDataToggle.addActionListener {
            executeAction { device ->
                adbController.toggleNetwork(device, Network.MOBILE)
            }
        }

        inputOnDeviceButton.addActionListener {
            executeAction { device ->
                adbController.inputOnDevice(inputOnDeviceTextField.text, device)
            }
        }
        inputOnDeviceTextField.addActionListener { inputOnDeviceButton.doClick() }

        openDeveloperOptionsButton.addActionListener {
            executeAction { device ->
                adbController.openDeveloperOptions(device)
            }
        }

        openDeepLinkButton.addActionListener {
            executeAction { device ->
                adbController.openDeepLink(openDeepLinkTextField.text, device)
            }
        }
        openDeepLinkTextField.addActionListener { openDeepLinkButton.doClick() }

        openAccountsButton.addActionListener {
            executeAction { device ->
                adbController.openAccounts(device)
            }
        }

        openAppSettingsButton.addActionListener {
            executeAction { device ->
                adbController.openAppSettings(device)
            }
        }

        firebaseButton.addActionListener {
            executeAction { device ->
                val firebaseDebugApp = device.getFirebaseDebugApp()
                adbController.setFirebaseDebugApp(device, firebaseDebugApp)
                setFirebaseData(firebaseDebugApp)
            }
        }

        avsbOpenStatus.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).openStatus(device)
            }
        }

        avsbOpenSettings.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).openSettings(device)
            }
        }

        avsbAppSettingsButton.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).openAVSBAppSettings(device)
            }
        }

        avsbEPG.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.EPG, device)
            }
        }

        avsbBack.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.BACK, device)
            }
        }

        avsbExit.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.EXIT, device)
            }
        }

        avsbReboot.addActionListener {
            executeAction { _ ->
                (adbController as AVSBAdbController).processCommand(ProcessCommand.Command.REBOOT)
            }
        }

        avsbUninstall.addActionListener {
            executeAction { _ ->
                (adbController as AVSBAdbController).processCommand(ProcessCommand.Command.UNINSTALL)
            }
        }

        avsbForceKill.addActionListener {
            executeAction { _ ->
                (adbController as AVSBAdbController).processCommand(ProcessCommand.Command.FORCE_KILL)
            }
        }

        avsbClearData.addActionListener {
            executeAction { _ ->
                (adbController as AVSBAdbController).processCommand(ProcessCommand.Command.CLEAR_DATA)
            }
        }

        avsbPower.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.POWER, device)
            }
        }

        avsbHome.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.HOME, device)
            }
        }

        avsbSearch.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.SEARCH, device)
            }
        }

        avsbAllApps.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.ALL_APPS, device)
            }
        }

        avsbAppsOpen.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).openApp(avsbAppsComboBox.selectedItem as String, device)
            }
        }

        avsbAppsClose.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).closeApp(avsbAppsComboBox.selectedItem as String, device)
            }
        }

        avsbProxySet.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).setProxy(avsbProxyHostname.text, avsbProxyPort.text, device)
            }
        }

        avsbProxyNone.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).clearProxy(device)
            }
        }

        avsbTalkback.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).toggleTalkback(device)
            }
        }

        avsbCopyBoxInfo.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).copyBoxInfoToClipboard(device)
            }
        }

        avsbInstallAPK.addActionListener {
            executeAction { device ->
                (adbController as AVSBAdbController).installApk(device)
            }
        }
    }

    private fun updateUi(appSettings: AppSettings) {
        appSettings.settings.forEach { (action, visibility) ->
            when (action) {
                ADBToolsAction.CODE_HELPERS -> {
                    currentActivityButton.isVisible = visibility
                    currentFragmentButton.isVisible = visibility
                    currentAppBackStackButton.isVisible = visibility
                    activitiesBackStackButton.isVisible = visibility
                    clearAppDataButton.isVisible = visibility
                    clearAppDataAndRestartButton.isVisible = visibility
                    restartAppButton.isVisible = visibility
                    restartAppWithDebuggerButton.isVisible = visibility
                    testProcessDeathButton.isVisible = visibility
                    forceKillAppButton.isVisible = visibility
                    uninstallAppButton.isVisible = visibility
                    openAppSettingsButton.isVisible = visibility
                }

                ADBToolsAction.TOGGLE_NETWORK -> networkPanel.isVisible = visibility
                ADBToolsAction.PERMISSIONS -> permissionPanel.isVisible = visibility
                ADBToolsAction.DEVELOPER_OPTIONS -> developerPanel.isVisible = visibility

                ADBToolsAction.AVSB -> avsbPanel.isVisible = visibility
                ADBToolsAction.INPUT -> {
                    inputOnDeviceButton.isVisible = visibility
                    inputOnDeviceTextField.isVisible = visibility
                }

                ADBToolsAction.DEEP_LINK -> {
                    openDeepLinkButton.isVisible = visibility
                    openDeepLinkTextField.isVisible = visibility
                }

                ADBToolsAction.OPEN_ACCOUNTS -> openAccountsButton.isVisible = visibility

                ADBToolsAction.FIREBASE -> {
                    firebaseButton.isVisible = visibility
                    firebaseTextField.isVisible = visibility
                }
            }
            rootPanel.invalidate()
        }
    }

    private fun updateDevicesList() {
        adbController.connectedDevices { devices ->
            this.devices = devices
            selectedIDevice = this.devices.getOrElse(devices.indexOf(selectedIDevice)) { this.devices.getOrNull(ZERO) }

            devicesListComboBox.model = DefaultComboBoxModel(
                devices.map { device ->
                    device.name
                }.toTypedArray()
            )
        }
    }

    private fun removeListeners() {
        enableDisableShowTaps.actionListeners.forEach {
            enableDisableShowTaps.removeActionListener(it)
        }

        enableDisableShowLayoutBounds.actionListeners.forEach {
            enableDisableShowLayoutBounds.removeActionListener(it)
        }

        enableDisableDarkMode.actionListeners.forEach {
            enableDisableDarkMode.removeActionListener(it)
        }

        windowAnimatorScaleComboBox.actionListeners.forEach {
            windowAnimatorScaleComboBox.removeActionListener(it)
        }

        transitionAnimatorScaleComboBox.actionListeners.forEach {
            transitionAnimatorScaleComboBox.removeActionListener(it)
        }

        animatorDurationScaleComboBox.actionListeners.forEach {
            animatorDurationScaleComboBox.removeActionListener(it)
        }

        networkRateLimitComboBox.actionListeners.forEach {
            networkRateLimitComboBox.removeActionListener(it)
        }

        dmsComboBox.actionListeners.forEach {
            dmsComboBox.removeActionListener(it)
        }
    }

    private fun setPredefinedValues() {
        enableDisableDontKeepActivities.isSelected =
            selectedIDevice?.areDontKeepActivitiesEnabled() == DontKeepActivitiesState.ENABLED

        enableDisableShowTaps.isSelected = selectedIDevice?.areShowTapsEnabled() == ShowTapsState.ENABLED

        enableDisableShowLayoutBounds.isSelected =
            selectedIDevice?.areShowLayoutBoundsEnabled() == ShowLayoutBoundsState.ENABLED

        enableDisableDarkMode.isSelected =
            selectedIDevice?.isDarkModeEnabled() == EnableDarkModeState.ENABLED

        windowAnimatorScaleComboBox.selectedItem =
            WindowAnimatorScaleCommand.getWindowAnimatorScaleIndex(selectedIDevice?.getWindowAnimatorScale())

        transitionAnimatorScaleComboBox.selectedItem =
            TransitionAnimatorScaleCommand.getTransitionAnimatorScaleIndex(selectedIDevice?.getTransitionAnimationScale())

        animatorDurationScaleComboBox.selectedItem =
            AnimatorDurationScaleCommand.getAnimatorDurationScaleIndex(selectedIDevice?.getAnimatorDurationScale())

        networkRateLimitComboBox.selectedItem =
            NetworkRateLimitCommand.getGetNetworkRateLimitIndex(selectedIDevice?.getNetworkRateLimit())

        dmsComboBox.selectedItem = DMSCommand.getDMSIndex(selectedIDevice?.getDMS())

        selectedIDevice?.let { device ->
            setFirebaseData(device.getFirebaseDebugApp())
        }
    }

    private fun setListeners() {
        enableDisableShowTaps.addActionListener(showTapsActionListener)

        enableDisableShowLayoutBounds.addActionListener(showLayoutBoundsActionListener)

        enableDisableDarkMode.addActionListener(showDarkModeActionListener)

        windowAnimatorScaleComboBox.addActionListener(windowAnimatorScaleActionListener)

        transitionAnimatorScaleComboBox.addActionListener(transitionAnimatorScaleActionListener)

        animatorDurationScaleComboBox.addActionListener(animatorDurationScaleActionListener)

        networkRateLimitComboBox.addActionListener(networkRateLimitActionListener)

        dmsComboBox.addActionListener(dmsActionListener)
    }

    private fun setFirebaseData(currentFirebaseDebugApp: String) {
        firebaseTextField.text = currentFirebaseDebugApp
        firebaseButton.text = when (currentFirebaseDebugApp) {
            FirebaseCommand.NO_DEBUG_APP -> "Enable Firebase Debug"
            EMPTY -> "Firebase"
            else -> "Disable Firebase Debug"
        }
    }

    private fun executeAction(action: (device: IDevice) -> Unit) {
        val currentDevice = selectedIDevice
        when {
            currentDevice != null && currentDevice.isOnline -> action.invoke(currentDevice)
            else -> {
                Messages.showErrorDialog(
                    project,
                    "You are either connected to another device or not connected at all. Make sure you are connected and have selected the correct device from the list.",
                    "Not Connected To Device"
                )
            }
        }
    }

    private fun setToolWindowListener() {
        ToolWindowManager
            .getInstance(project)
            .run {
                val toolWindow = getToolWindow("ADBTools")
                if (toolWindow != null) {
                    project
                        .messageBus
                        .connect()
                        .subscribe(
                            ToolWindowManagerListener.TOPIC,
                            object : ToolWindowManagerListener {
                                override fun stateChanged(toolWindowManager: ToolWindowManager, changeType: ToolWindowManagerListener.ToolWindowManagerEventType) {
                                    super.stateChanged(toolWindowManager, changeType)

                                    _toolwindowState.tryEmit(
                                        TollVisibilityWindowState(
                                            if (ToolWindowManagerListener.ToolWindowManagerEventType.ActivateToolWindow == changeType) ToolWindowState.VISIBLE
                                            else ToolWindowState.UNKNOWN,
                                            toolWindow.isVisible
                                        )
                                    )
                                }
                            }
                        )
                }
            }
    }

    data class TollVisibilityWindowState(val state: ToolWindowState, val isVisible: Boolean)

    enum class ToolWindowState {
        VISIBLE,
        UNKNOWN,
    }
}
