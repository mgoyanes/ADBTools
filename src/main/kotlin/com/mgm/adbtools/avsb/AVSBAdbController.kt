package com.mgm.adbtools.avsb

import com.android.ddmlib.IDevice
import com.mgm.adbtools.AdbController

interface AVSBAdbController : AdbController {
    fun setDMS(dms: String, device: IDevice)
    fun openStatus(device: IDevice)
    fun openSettings(device: IDevice)
    fun inputKeyEvent(keyEvent: Int, device: IDevice)
    fun openApp(app: String, device: IDevice)
    fun closeApp(app: String, device: IDevice)
    fun processCommand(command: ProcessCommand.Command)
    fun openAVSBAppSettings(device: IDevice)
    fun setProxy(hostname: String?, port: String?, device: IDevice)
    fun clearProxy(device: IDevice)
    fun toggleTalkback(device: IDevice)
    fun copyBoxInfoToClipboard(device: IDevice)
    fun installApk(device: IDevice)
}
