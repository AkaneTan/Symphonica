package org.akanework.symphonica.logic.util

fun convertDurationToTimeStamp(duration: String): String {
    val minutes = duration.toInt() / 1000 / 60
    val seconds = duration.toInt() / 1000 - minutes * 60
    if (seconds < 10) {
        return "$minutes:0$seconds"
    }
    return "$minutes:$seconds"
}