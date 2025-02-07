package com.mgm.adbtools.avsb

import com.android.ddmlib.IDevice
import com.android.ddmlib.InstallReceiver
import com.mgm.adbtools.command.Command

class InstallApkCommand : Command<String, String> {

    override fun execute(p: String, device: IDevice): String {
        val result = InstallReceiver()

        device.installPackage(p, true, result, "-t")

        return if (result.isSuccessfullyCompleted) {
            "App installed successfully"
        } else {
            throw Exception("App not installed: error code=${result.errorCode} message=${result.errorMessage}")
        }
    }
}
