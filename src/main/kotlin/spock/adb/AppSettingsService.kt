package spock.adb

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import spock.adb.premission.ListItem

@State(
    name = "adbtools-localData",
    storages = [Storage("adbtools-localData.xml")]
)
class AppSettingsService : PersistentStateComponent<AppSettings> {

    private var localData: AppSettings

    init {
        val map =
            ADBToolsAction.entries.associateWith { _ -> true }
        localData = AppSettings(map)
    }

    override fun getState(): AppSettings {
        return localData
    }

    override fun loadState(state: AppSettings) {
        localData = state
    }

    companion object {
        @JvmStatic
        fun getInstance(): PersistentStateComponent<AppSettings> {
            return ApplicationManager.getApplication().getService(AppSettingsService::class.java)
        }
    }
}

data class AppSettings(var settings: Map<ADBToolsAction, Boolean>) {
    fun getAllAvailableAppSettings(): List<ListItem> = settings.entries.map { (action, enabled) -> ListItem(action.displayName, enabled) }
}

enum class ADBToolsAction(val displayName: String) {
    CODE_HELPERS("Android Code Helpers"),
    TOGGLE_NETWORK("Toggle Network"),
    PERMISSIONS("Permissions"),
    DEVELOPER_OPTIONS("Developer Options"),
    AVSB("AVSB"),
    INPUT("Input"),
    DEEP_LINK("Deep Link"),
    OPEN_ACCOUNTS("Open Accounts"),
    FIREBASE("Firebase"),
}
