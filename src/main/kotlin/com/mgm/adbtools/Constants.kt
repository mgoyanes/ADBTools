package com.mgm.adbtools

const val ZERO = 0
const val ONE = 1

const val EMPTY = ""
const val HYPHEN = "-"
const val LINE_SEPARATOR = "\n"
const val DOT = '.'
const val DASH = '/'
const val TAB = '\t'

const val MAX_TIME_TO_OUTPUT_RESPONSE = 15L
const val NO_TIME_TO_OUTPUT_RESPONSE = 0L
const val MIN_TIME_TO_OUTPUT_RESPONSE = 2L
const val ACTIVITY_PREFIX_DELIMITER = DOT.toString()
const val DUMPSYS_ACTIVITY = "dumpsys activity"
const val GET_TOP_ACTIVITY_COMMAND = "$DUMPSYS_ACTIVITY | grep -E \"mResumedActivity|topResumedActivity\""
val extractAppRegex = Regex("(A=|I=|u0\\s)([a-zA-Z.]+)")
val extractActivityRegex = Regex("(u0\\s[a-zA-Z.]+/)([a-zA-Z.]+)")
const val AVSB_PACKAGE = "com.vodafone.vtv.avsb"
