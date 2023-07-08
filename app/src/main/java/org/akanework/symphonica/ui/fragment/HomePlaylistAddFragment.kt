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

package org.akanework.symphonica.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.symphonica.MainActivity.Companion.customFragmentManager
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.R

/**
 * [HomePlaylistAddFragment] is the history list
 * page.
 */
class HomePlaylistAddFragment : Fragment() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition =
            MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true).setDuration(500)
        returnTransition =
            MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false).setDuration(500)
    }

    @SuppressLint("StringFormatMatches")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home_playlist_add_view, container, false)

        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        val descTextBox = rootView.findViewById<TextInputEditText>(R.id.playlist_add_desc)
        val nameTextBox = rootView.findViewById<TextInputEditText>(R.id.playlist_add_name)
        val confirmButton = rootView.findViewById<FloatingActionButton>(R.id.confirm_button)

        topAppBar.setNavigationOnClickListener {
            customFragmentManager.popBackStack()
        }

        confirmButton.setOnClickListener {
            if (nameTextBox.text.isNullOrBlank() || nameTextBox.text.isNullOrEmpty()) {
                Snackbar.make(confirmButton, R.string.home_playlist_add_view_name_empty, Snackbar.LENGTH_SHORT)
                    .show()
            } else {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        val newPlaylist = playlistViewModel.createPlaylist(nameTextBox.text.toString(), descTextBox.text.toString())
                        playlistViewModel.playlistList.add(newPlaylist)
                    }
                }
                customFragmentManager.popBackStack()
            }
        }

        return rootView
    }

}