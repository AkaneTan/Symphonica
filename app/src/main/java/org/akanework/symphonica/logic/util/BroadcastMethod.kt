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

import android.content.Intent
import org.akanework.symphonica.SymphonicaApplication.Companion.context

/**
 * [broadcastMetaDataUpdate] updates the notification's metadata.
 */
fun broadcastMetaDataUpdate() {
    val intentBroadcast = Intent("internal.play_update")
    context.sendBroadcast(intentBroadcast)
}

/**
 * [broadcastSliderSeek] broadcast in case of needing to seek
 * the player for the notification.
 */
fun broadcastSliderSeek() {
    val intentBroadcast = Intent("internal.play_seek")
    context.sendBroadcast(intentBroadcast)
}

/**
 * [broadcastPlayStopped] broadcast in case of play stopped.
 */
fun broadcastPlayStopped() {
    val intentBroadcast = Intent("internal.play_stop")
    context.sendBroadcast(intentBroadcast)
}

/**
 * [broadcastPlayStart] broadcast in case of play start.
 */
fun broadcastPlayPaused() {
    val intentBroadcast = Intent("internal.play_pause")
    context.sendBroadcast(intentBroadcast)
}

/**
 * [broadcastPlayStart] broadcast in case of pause.
 */
fun broadcastPlayStart() {
    val intentBroadcast = Intent("internal.play_start")
    context.sendBroadcast(intentBroadcast)
}

/**
 * [broadcastSquigglyUpdate] broadcast in case of needing to seek
 * the player for the notification.
 */
fun broadcastSquigglyUpdate() {
    val intentBroadcast = Intent("internal.play_squiggly_update")
    context.sendBroadcast(intentBroadcast)
}
