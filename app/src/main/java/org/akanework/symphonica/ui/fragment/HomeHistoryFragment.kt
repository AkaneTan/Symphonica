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

package org.akanework.symphonica.ui.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.transition.MaterialSharedAxis
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.customFragmentManager
import org.akanework.symphonica.MainActivity.Companion.historyDao
import org.akanework.symphonica.MainActivity.Companion.libraryViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.replacePlaylist
import org.akanework.symphonica.ui.adapter.HomeHistoryAdapter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.symphonica.PAGE_TRANSITION_DURATION

/**
 * [HomeHistoryFragment] is the history list
 * page.
 */
class HomeHistoryFragment : Fragment() {
    private val historySongList: MutableList<Song> = mutableListOf()
    private lateinit var adapter: HomeHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition =
                MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true).setDuration(
                    PAGE_TRANSITION_DURATION)
        exitTransition =
                MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false).setDuration(
                    PAGE_TRANSITION_DURATION)
    }

    @SuppressLint("StringFormatMatches")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home_history_view, container, false)

        val topAppBar: MaterialToolbar = rootView.findViewById(R.id.topAppBar)
        val historyCounter = rootView.findViewById<TextView>(R.id.home_history_song_count)

        val recyclerView = rootView.findViewById<RecyclerView>(R.id.home_history_recyclerview)

        val playButton = rootView.findViewById<MaterialButton>(R.id.home_history_play_button)
        val clearAllButton = rootView.findViewById<MaterialButton>(R.id.home_history_clear_button)

        val songNumber = libraryViewModel.libraryHistoryList.size

        val coroutineScope = CoroutineScope(Dispatchers.Main)

        playButton.setOnClickListener {
            if (historySongList.size > 0) {
                if (MainActivity.controllerViewModel.shuffleState) {
                    MainActivity.controllerViewModel.shuffleState = false
                    MainActivity.fullSheetShuffleButton!!.isChecked = false
                    MainActivity.playlistViewModel.originalPlaylist.clear()
                }
                replacePlaylist(historySongList, 0)
                MainActivity.playlistViewModel.currentLocation = 0
            }
        }

        clearAllButton.setOnClickListener {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    libraryViewModel.libraryHistoryList.clear()
                    historyDao.clearHistoryItems()
                    withContext(Dispatchers.Main) {
                        historyCounter.text = getString(R.string.home_history_counter_song, 0)
                        val animator = ObjectAnimator.ofFloat(recyclerView, "alpha", 1f, 0f)
                        animator.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                recyclerView.visibility = View.GONE
                            }
                        })
                        animator.duration = 500
                        animator.start()
                    }
                }
            }
        }

        topAppBar.setNavigationOnClickListener {
            customFragmentManager.popBackStack()
        }

        if (songNumber <= 1) {
            historyCounter.text = getString(R.string.home_history_counter_song, songNumber)
        } else {
            historyCounter.text = getString(R.string.home_history_counter_songs, songNumber)
        }

        for (i in libraryViewModel.libraryHistoryList) {
            val song = libraryViewModel.librarySongList.find { it.id == i }
            song?.let { historySongList.add(it) }
        }

        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        adapter = HomeHistoryAdapter(historySongList)
        recyclerView.adapter = adapter

        return rootView
    }
}
