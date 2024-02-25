package spock.adb.command

import com.android.ddmlib.IDevice
import com.android.tools.idea.projectsystem.getModuleSystem
import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.project.Project
import org.jetbrains.android.facet.AndroidFacet

class GetPackageNameCommand : Command<Any, String?> {
    override fun execute(p: Any, project: Project, device: IDevice): String? {
        val facets = ProjectFacetManager
            .getInstance(project)
            .getFacets(AndroidFacet.ID)
        return facets
            .firstOrNull()
            ?.module
            ?.getModuleSystem()
            ?.getPackageName()
    }
}
