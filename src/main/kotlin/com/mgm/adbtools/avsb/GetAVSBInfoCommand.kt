package com.mgm.adbtools.avsb

import ai.grazie.utils.capitalize
import android.databinding.tool.ext.toCamelCase
import com.android.ddmlib.IDevice
import com.mgm.adbtools.EMPTY
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.command.NoInputCommand
import com.mgm.adbtools.executeShellCommandWithTimeout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.concurrent.TimeUnit

class GetAVSBInfoCommand : NoInputCommand<String> {

    companion object {
        private const val COMMAND_TIMEOUT_MILLIS = 150L
    }

    override fun execute(device: IDevice): String {
        var result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout("settings get secure com_vodafone_vtv_dms", result, COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        val market = DMSCommand.getDMSIndex(result.toString()).capitalize()
        result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout("getprop ro.product.manufacturer", result, COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        val manufacturer = result.toString().toCamelCase()
        result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout("getprop ro.product.device", result, COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        val boxModel = getBoxModel(result.toString())
        result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout("getprop ro.product.build.version.release", result, COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        val androidVersion = result.toString()
        result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout("getprop ro.product.build.version.incremental", result, COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        val firmwareVersion = result.toString()
        result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout("dumpsys package com.vodafone.vtv.avsb | grep \"flags=\\[\"", result, COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        val buildType = if (result.toString().contains("DEBUGGABLE", true)) "Debug" else "Release"
        result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout("getprop ro.boot.serialno", result, COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        val serialNumber = result.toString()
        result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout("settings get secure com_vodafone_vtv_cver", result, COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        val cver = result.toString()
        result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout("getprop persist.sys.nagra.casn", result, COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        val casID = result.toString()
        result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout(
            "dumpsys package com.vodafone.vtv.avsb | grep -Ei \"versionName=\" | head -n 1 | sed 's/versionName=//'",
            result,
            COMMAND_TIMEOUT_MILLIS,
            TimeUnit.MILLISECONDS
        )
        val uiVersion = result.toString()


        val boxInfo = """
        Device Under Test:
        $market | $manufacturer
        $boxModel | Android $androidVersion | FW: $firmwareVersion | SN: $serialNumber
        UI Version ${uiVersion.trimIndent()}
        casId $casID | cver $cver
        BuildType: $buildType
        """.trimIndent()

        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(boxInfo)
        clipboard.setContents(selection, selection)


        return "Information copied to clipboard"
    }

    private fun getBoxModel(boxModel: String): String {

        return when (boxModel) {
            "m393gena_vf" -> "AVSB"
            "m377_vf" -> "GEN 3"
            "m378_vf" -> "GEN 4 Cable"
            "m253_vf" -> "GEN 4 Ip"
            "vf_stb_k04_0" -> "GEN 4 Kaon Cable"
            "vf_stb_k04_1" -> "GEN 4 Kaon Ip"
            "vf_stb_k04_2" -> "GEN 4 Kaon Mini"
            else -> EMPTY
        }
    }
}
