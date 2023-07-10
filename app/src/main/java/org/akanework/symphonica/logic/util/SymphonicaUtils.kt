/*
 *     Copyright (C) 2023  Akane Foundation
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.symphonica.logic.util

import org.akanework.symphonica.FORMAT_ZERO_APPEND
import org.akanework.symphonica.HOUR_IN_MINUTES
import org.akanework.symphonica.MILLISECOND

/**
 * [convertDurationToTimeStamp] makes a string format
 * of duration (presumably long) converts into timestamp
 * like 300 to 5:00.
 *
 * @param duration
 * @return
 */
fun convertDurationToTimeStamp(duration: String): String {
    val minutes = duration.toInt() / MILLISECOND / HOUR_IN_MINUTES
    val seconds = duration.toInt() / MILLISECOND - minutes * HOUR_IN_MINUTES
    if (seconds < FORMAT_ZERO_APPEND) {
        return "$minutes:0$seconds"
    }
    return "$minutes:$seconds"
}
