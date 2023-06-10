package spock.adb.command

import com.android.ddmlib.IDevice
import com.android.tools.idea.projectsystem.getModuleSystem
import com.intellij.openapi.project.Project
import org.jetbrains.android.util.AndroidUtils

class GetPackageNameCommand : Command<Any, String?> {
    override fun execute(p: Any, project: Project, device: IDevice): String? =
        AndroidUtils
            .getApplicationFacets(project)
            .firstOrNull()
            ?.module
            ?.getModuleSystem()
            ?.getPackageName()
}
