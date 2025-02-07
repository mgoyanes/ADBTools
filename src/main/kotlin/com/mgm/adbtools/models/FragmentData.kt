package com.mgm.adbtools.models

import com.mgm.adbtools.EMPTY

data class FragmentData(
    val fragment: String,
    val fragmentIdentifier: String = EMPTY,
    var innerFragments: List<FragmentData> = emptyList()
)
