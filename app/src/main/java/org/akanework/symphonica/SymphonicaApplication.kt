/*
 *     Copyright (C) 2023 Akane Foundation
 *
 *     This file is part of Symphonica.
 *
 *     Symphonica is free software: you can redistribute it and/or modify it under the terms
 *     of the GNU General Public License as published by the Free Software Foundation,
 *     either version 3 of the License, or (at your option) any later version.
 *
 *     Symphonica is distributed in the hope that it will be useful, but WITHOUT ANY
 *     WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *     FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along with
 *     Symphonica. If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.symphonica

import android.app.Application
import com.google.android.material.color.DynamicColors
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.MusicPlayer

/**
 * [SymphonicaApplication] provides context
 * and applies dynamic colors.
 */
class SymphonicaApplication : Application() {

    companion object {
        lateinit var context: SymphonicaApplication
    }

    lateinit var musicPlayer: MusicPlayer<Song>

    override fun onCreate() {
        super.onCreate()
        context = this
        musicPlayer = MusicPlayer(this)

        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}