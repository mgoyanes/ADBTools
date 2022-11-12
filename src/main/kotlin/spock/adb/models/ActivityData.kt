package spock.adb.models

data class ActivityData(val activity: String, val fragment: List<FragmentData>)
class BackStackData(val appPackage: String, val activitiesList: List<String>)
