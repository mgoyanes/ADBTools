package com.mgm.adbtools.avsb

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.EMPTY
import com.mgm.adbtools.MIN_TIME_TO_OUTPUT_RESPONSE
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.command.Command
import com.mgm.adbtools.executeShellCommandWithTimeout

class DMSCommand : Command<String, String> {

    companion object {
        fun getDMSIndex(dms: String?): String =
            when (dms) {
                "http://185.dms-atp2.ott.kaltura.com/" -> "ATP"
                "http://185.dms-ind2.ott.kaltura.com/" -> "ID2"
                "https://195.dms-vfp2.ott.kaltura.com/" -> "ES"
                "https://3035.dms-vfp1.ott.kaltura.com/" -> "GR"
                "https://3038.dms-vfp1.ott.kaltura.com/" -> "PT"
                "https://3041.dms-vfp1.ott.kaltura.com/" -> "RO"
                "https://3062.dms-vfp1.ott.kaltura.com/" -> "CZ"
                "https://3044.dms-vfp1.ott.kaltura.com/" -> "DE"
                "https://3218.dms-vfp1.ott.kaltura.com/" -> "AL"
                "https://8388.dms-vfp2.ott.kaltura.com/" -> "IE"
                "https://222.dms-vfp1.ott.kaltura.com/" -> "IT"
                "https://430.dms-vfp1.ott.kaltura.com/" -> "NZ"
                "https://3047.dms-vfp1.ott.kaltura.com/" -> "HU"

                else -> EMPTY
            }
    }

    override fun execute(p: String, project: Project, device: IDevice): String {
        val dms =
            when (p) {
                "ATP" -> "http://185.dms-atp2.ott.kaltura.com/"
                "ID2" -> "http://185.dms-ind2.ott.kaltura.com/"
                "ES" -> "https://195.dms-vfp2.ott.kaltura.com/"
                "GR" -> "https://3035.dms-vfp1.ott.kaltura.com/"
                "PT" -> "https://3038.dms-vfp1.ott.kaltura.com/"
                "RO" -> "https://3041.dms-vfp1.ott.kaltura.com/"
                "CZ" -> "https://3062.dms-vfp1.ott.kaltura.com/"
                "DE" -> "https://3044.dms-vfp1.ott.kaltura.com/"
                "AL" -> "https://3218.dms-vfp1.ott.kaltura.com/"
                "IE" -> "https://8388.dms-vfp2.ott.kaltura.com/"
                "IT" -> "https://222.dms-vfp1.ott.kaltura.com/"
                "NZ" -> "https://430.dms-vfp1.ott.kaltura.com/"
                "HU" -> "https://3047.dms-vfp1.ott.kaltura.com/"

                else -> EMPTY
            }


        device.executeShellCommandWithTimeout("settings put secure com_vodafone_vtv_dms $dms", ShellOutputReceiver(), MIN_TIME_TO_OUTPUT_RESPONSE)

        return "Set DMS to $dms"
    }
}
