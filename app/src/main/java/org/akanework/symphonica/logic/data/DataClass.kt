/*
 *     Copyright (C) 2023 AkaneWork Organization
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.symphonica.logic.data

import android.graphics.drawable.Drawable
import android.net.Uri

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
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val duration: Long,
    val path: String,
    val imgUri: Uri?
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
 */
data class Album(
    val title: String,
    val artist: String,
    val cover: Drawable?,
    val songList: List<Song>
)