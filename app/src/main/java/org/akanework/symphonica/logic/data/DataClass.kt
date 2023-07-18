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

package org.akanework.symphonica.logic.data

import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log

/**
 * [Song] stores & labels Symphonica's library.
 *
 * Arguments are extracted from the file itself.
 * You can see the extraction process in MediaStoreReader.kt.
 *
 * [id] equals to MediaStore ID.
 * [album] is a single String.
 * [duration] is its length in ms.
 * [path] is its path.
 * [imgUri] is its album Uri. Which can be used
 * to extract its album information using Glide.
 * @property id
 * @property title
 * @property artist
 * @property album
 * @property albumArtist
 * @property duration
 * @property path
 * @property imgUri
 * @property addDate
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val duration: Long,
    val path: String,
    val imgUri: Uri?,
    val addDate: Long?
)

/**
 * [Album] stores album list information.
 *
 * [title] is this album's title.
 * And other variables are extracted from
 * the FIRST song of the songList.
 * TODO: Rewrite this
 *
 * [songList] contains all the songs from
 * this single album.
 * @property title
 * @property artist
 * @property cover
 * @property songList
 */
data class Album(
    val title: String,
    val artist: String,
    val cover: Drawable?,
    val songList: List<Song>
)

/**
 * Represents a lyric object containing lines of lyrics and their corresponding start times.
 * Each line of lyrics is associated with a start time indicating when it should be displayed or played.
 *
 * @property lines The list of lyric lines.
 * @property startTimes The list of start times for each lyric line.
 */
data class Lyric(
    val lines: MutableList<String>,
    val startTimes: MutableList<Long>
) {
    fun parseLrcFile(lrcContent: String) {
        val linesRegex = "\\[(\\d{2}:\\d{2}\\.\\d{2})](.*)".toRegex()

        lrcContent.lines().forEach { line ->
            val matchResult = linesRegex.find(line)
            if (matchResult != null) {
                val startTime = parseTime(matchResult.groupValues[1])
                val lyricLine = matchResult.groupValues[2]
                lines.add(lyricLine)
                startTimes.add(startTime)
            }
        }
    }

    private fun parseTime(timeString: String): Long {
        val timeRegex = "(\\d{2}):(\\d{2})\\.(\\d{2})".toRegex()
        val matchResult = timeRegex.find(timeString)

        val minutes = matchResult?.groupValues?.get(1)?.toLongOrNull() ?: 0
        val seconds = matchResult?.groupValues?.get(2)?.toLongOrNull() ?: 0
        val milliseconds = matchResult?.groupValues?.get(3)?.toLongOrNull() ?: 0

        return minutes * 60000 + seconds * 1000 + milliseconds * 10
    }
}
