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

package org.akanework.symphonica.ui.viewmodel

import androidx.lifecycle.ViewModel

/**
 * [AlbumDisplayViewModel]:
 * I forgot what this [ViewModel] does..
 * Maybe it saves what libraryAlbumDisplayFragment
 * is displaying?
 * [position] probably means the position inside
 * the whole library.
 */
class AlbumDisplayViewModel : ViewModel() {
    var position: Int? = null
}
