package spock.adb.command

import com.android.ddmlib.IDevice
import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.util.androidFacet
import com.intellij.facet.ProjectFacetManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ChooseModulesDialog
import com.intellij.util.ui.UIUtil
import org.jetbrains.android.facet.AndroidFacet
import java.awt.Component
import java.awt.Dimension
import javax.swing.JTable

class GetApplicationIDCommand : Command<Any, String?> {
    override fun execute(p: Any, project: Project, device: IDevice): String? {
        val facets = ProjectFacetManager
            .getInstance(project)
            .getFacets(AndroidFacet.ID)

        if (facets.isEmpty()) return null

        val facet = getFacet(facets, project) ?: return null

        return AndroidModel.get(facet)?.applicationId ?: return null
    }

    private fun getFacet(facets: List<AndroidFacet>, project: Project): AndroidFacet? {
        val facetList =
            facets
                .mapNotNull { androidFacet ->
                    androidFacet.module.androidFacet
                }
                .distinct()

        val facet: AndroidFacet?
        if (facetList.size > 1) {
            facet = showDialogForFacets(project, facetList)
            if (facet == null) {
                return null
            }
        } else {
            facet = facetList[0]
        }

        return facet
    }


    private fun showDialogForFacets(project: Project, facets: List<AndroidFacet>): AndroidFacet? {
        val modules = facets.map { it.module }
        val previousModuleName = getSavedModuleName(project)
        val previousSelectedModule = modules.firstOrNull { it.name == previousModuleName }

        val selectedModule = showDialog(project, modules, previousSelectedModule) ?: return null
        saveModuleName(project, selectedModule.name)
        return facets[modules.indexOf(selectedModule)]
    }

    private fun showDialog(project: Project, modules: List<Module>, previousSelectedModule: Module?): Module? {
        with(ChooseModulesDialog(project, modules, "Choose Module", "")) {
            setSingleSelectionMode()
            getSizeForTableContainer(preferredFocusedComponent)?.let {
                setSize(it.width, it.height)
            }
            previousSelectedModule?.let { selectElements(listOf(it)) }
            return showAndGetResult().firstOrNull()
        }
    }

    private fun getSizeForTableContainer(component: Component?): Dimension? {
        if (component == null) return null
        val tables = UIUtil.uiTraverser(component).filter(JTable::class.java)
        if (!tables.isNotEmpty) return null
        val size = component.preferredSize
        for (table in tables) {
            val tableSize = table.preferredSize
            size.width = size.width.coerceAtLeast(tableSize.width)
            size.height = size.height.coerceAtLeast(tableSize.height + size.height - table.parent.height)
        }
        size.width = 1000.coerceAtMost(600.coerceAtLeast(size.width))
        size.height = 800.coerceAtMost(size.height)
        return size
    }


    private fun saveModuleName(project: Project, moduleName: String) {
        PropertiesComponent.getInstance(project).setValue(SELECTED_MODULE_PROPERTY, moduleName)
    }

    private fun getSavedModuleName(project: Project): String? {
        return PropertiesComponent.getInstance(project).getValue(SELECTED_MODULE_PROPERTY)
    }

    private val SELECTED_MODULE_PROPERTY = GetApplicationIDCommand::class.java.canonicalName + "-SELECTED_MODULE"


}
