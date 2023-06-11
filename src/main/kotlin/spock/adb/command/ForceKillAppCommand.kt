package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.MAX_TIME_TO_OUTPUT_RESPONSE

import spock.adb.isAppInstall
import spock.adb.forceKillApp

class ForceKillAppCommand:Command<String,Unit> {
    override fun execute(p: String, project: Project, device: IDevice) {
        if (device.isAppInstall(p))
            device.forceKillApp(p, MAX_TIME_TO_OUTPUT_RESPONSE)
        else
            throw Exception("Application $p not installed" )
    }
}
