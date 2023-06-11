package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.MAX_TIME_TO_OUTPUT_RESPONSE
import spock.adb.clearAppData
import spock.adb.isAppInstall

class ClearAppDataCommand : Command<String, Unit> {
    override fun execute(p: String, project: Project, device: IDevice) {
        if (device.isAppInstall(p)) {
            device.clearAppData(p, MAX_TIME_TO_OUTPUT_RESPONSE)
        } else
            throw Exception("Application $p not installed")
    }
}
