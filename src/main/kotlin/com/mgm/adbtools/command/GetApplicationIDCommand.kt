package com.mgm.adbtools.command

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

        return AndroidModel.get(facet)?.applicationId
    }

    private fun getFacet(facets: List<AndroidFacet>, project: Project): AndroidFacet? {
        val facetList =
            facets
                .mapNotNull { androidFacet ->
                    androidFacet.module.androidFacet
                }
                .distinct()

        if (facetList.size <= 1) return facetList.firstOrNull()

        // Multiple modules: reuse the previously chosen one for this project instead of asking
        // again on every call - only prompt when there is no saved choice yet, or the saved
        // module no longer exists among the current facets (e.g. renamed/removed).
        val previousModuleName = getSavedModuleName(project)
        val previousFacet = facetList.firstOrNull { it.module.name == previousModuleName }
        if (previousFacet != null) return previousFacet

        return showDialogForFacets(project, facetList)
    }

    private fun showDialogForFacets(project: Project, facets: List<AndroidFacet>): AndroidFacet? {
        val modules = facets.map { it.module }

        val selectedModule = showDialog(project, modules) ?: return null
        saveModuleName(project, selectedModule.name)
        return facets[modules.indexOf(selectedModule)]
    }

    private fun showDialog(project: Project, modules: List<Module>): Module? {
        with(ChooseModulesDialog(project, modules, "Choose Module", "")) {
            setSingleSelectionMode()
            getSizeForTableContainer(preferredFocusedComponent)?.let {
                setSize(it.width, it.height)
            }
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
