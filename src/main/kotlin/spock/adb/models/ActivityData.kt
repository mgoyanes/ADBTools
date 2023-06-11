package spock.adb.models

data class ActivityData(val activity: String, val activityStackPosition: Int = -1, val isKilled: Boolean = false, val fragment: List<FragmentData>)
class BackStackData(val appPackage: String, val activitiesList: List<String>)
