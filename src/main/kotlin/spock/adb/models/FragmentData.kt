package spock.adb.models

import spock.adb.EMPTY

data class FragmentData(
    val fragment: String,
    val fragmentIdentifier: String = EMPTY,
    var innerFragments: List<FragmentData> = emptyList()
)
