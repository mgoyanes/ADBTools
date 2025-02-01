package spock.adb.avsb

import com.android.ddmlib.IDevice
import spock.adb.AdbController

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
}
