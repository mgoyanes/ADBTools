package spock.adb.models

data class FragmentData(
    val fragment: String,
    val fragmentIdentifier: String = "",
    var innerFragments: List<FragmentData> = emptyList()
)
