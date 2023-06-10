package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project

interface ListCommand<in P, out R> {
    @Throws(Exception::class)
    fun execute(list: List<P>, project: Project, device: IDevice): R
}
