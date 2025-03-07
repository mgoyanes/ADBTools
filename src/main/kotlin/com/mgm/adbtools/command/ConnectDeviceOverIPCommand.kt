package com.mgm.adbtools.command

import com.intellij.openapi.project.Project
import org.jetbrains.android.sdk.AndroidSdkUtils
import com.mgm.adbtools.EMPTY
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class ConnectDeviceOverIPCommand : AdbCommand<String, Any> {
    override fun execute(p: String, project: Project): Any {
        val adbPath = AndroidSdkUtils.findAdb(project).adbPath?.path
        var process: Process? = null
        print("$adbPath connect $p:5555")
        try {
            process = Runtime.getRuntime().exec("$adbPath connect $p:5555")
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.run { destroy() }
            }
            val content = process.errorStream.readBytes().toString(Charset.defaultCharset())
            print(content)
            process.run {
                print(content)
                destroy()
            }
            if (content.isNotEmpty()) throw Exception("unable to connect to $p")
            return EMPTY
        } catch (e: Exception) {
            print(e)
            process?.destroy()
            throw Exception("unable to connect to $p")
      }
    }
}
