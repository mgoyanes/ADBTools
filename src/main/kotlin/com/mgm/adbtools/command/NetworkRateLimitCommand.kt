package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout

class NetworkRateLimitCommand : Command<String, String> {

    companion object {
        fun getGetNetworkRateLimitIndex(scale: String?): String =
            when (scale) {
                "-1" -> "No Limit"
                "125" -> "1kbps"
                "1250" -> "10kbps"
                "16000" -> "128kbps"
                "32000" -> "256kbps"
                "125000" -> "1Mpps"
                "625000" -> "5Mbps"
                "1875000" -> "15Mbps"
                else -> "No Limit"
            }
    }

    override fun execute(p: String, project: Project, device: IDevice): String {
        val rateLimit =
            when (p) {
                "No Limit" -> -1
                "1kbps" -> 125
                "10kbps" -> 1250
                "128kbps" -> 16000
                "256kbps" -> 32000
                "1Mpps" -> 125000
                "5Mbps" -> 625000
                "15Mbps" -> 1875000

                else -> -1
            }


        device.executeShellCommandWithTimeout("settings put global ingress_rate_limit_bytes_per_second $rateLimit", ShellOutputReceiver())

        return "Set Network Download Rate Limit to $rateLimit bytes per second"
    }
}
