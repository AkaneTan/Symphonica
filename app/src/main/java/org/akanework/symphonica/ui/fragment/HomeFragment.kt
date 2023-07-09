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

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialSharedAxis
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.customFragmentManager
import org.akanework.symphonica.MainActivity.Companion.fullSheetLoopButton
import org.akanework.symphonica.MainActivity.Companion.fullSheetShuffleButton
import org.akanework.symphonica.MainActivity.Companion.isAkaneVisible
import org.akanework.symphonica.MainActivity.Companion.isColorfulButtonEnabled
import org.akanework.symphonica.MainActivity.Companion.isListShuffleEnabled
import org.akanework.symphonica.MainActivity.Companion.libraryViewModel
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.Song
import org.akanework.symphonica.logic.util.replacePlaylist
import org.akanework.symphonica.ui.adapter.SongCarouselAdapter

/**
 * [HomeFragment] is homepage fragment.
 */
class HomeFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reenterTransition =
                MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false).setDuration(500)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        if (isAkaneVisible) {
            rootView.findViewById<ImageView>(R.id.akane).visibility = VISIBLE
        }

        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        val shuffleRefreshButton = rootView.findViewById<MaterialButton>(R.id.refresh_shuffle_list)
        val collapsingToolbar =
            rootView.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        val appBarLayout = rootView.findViewById<AppBarLayout>(R.id.appBarLayout)
        val shuffleCarouselRecyclerView =
            rootView.findViewById<RecyclerView>(R.id.shuffle_recycler_view)
        val recentCarouselRecyclerView =
            rootView.findViewById<RecyclerView>(R.id.recent_recycler_view)

        val homeShuffleButton =
            rootView.findViewById<MaterialButton>(R.id.home_shuffle_all)
        val homeFavouriteButton =
            rootView.findViewById<MaterialButton>(R.id.home_favourite)
        val homeHistoryButton =
            rootView.findViewById<MaterialButton>(R.id.home_history)
        val homePlaylistButton =
            rootView.findViewById<MaterialButton>(R.id.home_playlist)

        if (isColorfulButtonEnabled) {
            homeShuffleButton.setBackgroundColor(
                MaterialColors.getColor(
                    homeShuffleButton,
                    com.google.android.material.R.attr.colorTertiaryContainer
                )
            )
            homeShuffleButton.iconTint = ColorStateList.valueOf(
                MaterialColors.getColor(
                    homeShuffleButton,
                    com.google.android.material.R.attr.colorOnTertiaryContainer
                )
            )
            homeFavouriteButton.setBackgroundColor(
                MaterialColors.getColor(
                    homeShuffleButton,
                    com.google.android.material.R.attr.colorSecondaryContainer
                )
            )
            homeFavouriteButton.iconTint = ColorStateList.valueOf(
                MaterialColors.getColor(
                    homeShuffleButton,
                    com.google.android.material.R.attr.colorOnSecondaryContainer
                )
            )
            homeHistoryButton.setBackgroundColor(
                MaterialColors.getColor(
                    homeShuffleButton,
                    com.google.android.material.R.attr.colorPrimaryContainer
                )
            )
            homeHistoryButton.iconTint = ColorStateList.valueOf(
                MaterialColors.getColor(
                    homeShuffleButton,
                    com.google.android.material.R.attr.colorOnPrimaryContainer
                )
            )
            homePlaylistButton.setBackgroundColor(
                MaterialColors.getColor(
                    homeShuffleButton,
                    com.google.android.material.R.attr.colorTertiaryContainer
                )
            )
            homePlaylistButton.iconTint = ColorStateList.valueOf(
                MaterialColors.getColor(
                    homeShuffleButton,
                    com.google.android.material.R.attr.colorOnTertiaryContainer
                )
            )
        }

        val shuffleLayoutManager = CarouselLayoutManager()
        shuffleCarouselRecyclerView.layoutManager = shuffleLayoutManager
        shuffleAdapter = SongCarouselAdapter(shuffleList)
        shuffleCarouselRecyclerView.adapter = shuffleAdapter

        val recentLayoutManager = CarouselLayoutManager()
        recentCarouselRecyclerView.layoutManager = recentLayoutManager
        recentAdapter = SongCarouselAdapter(recentList)
        recentCarouselRecyclerView.adapter = recentAdapter

        loadingPrompt = rootView.findViewById(R.id.loading_prompt_list)

        homeShuffleButton.setOnClickListener {
            if (libraryViewModel.librarySongList.isNotEmpty()) {
                if (!isListShuffleEnabled) {
                    replacePlaylist(
                        libraryViewModel.librarySongList.toMutableList(),
                        (0 until libraryViewModel.librarySongList.size).random()
                    )
                    fullSheetShuffleButton?.isChecked = true
                    fullSheetLoopButton?.isChecked = true
                } else {
                    fullSheetShuffleButton?.isChecked = true
                    val playlist = mutableListOf<Song>()
                    playlist.addAll(libraryViewModel.librarySongList)
                    val shuffleSong = playlist.random()

                    val originalPlaylist = playlistViewModel.originalPlaylist
                    originalPlaylist.clear()
                    originalPlaylist.addAll(playlist)
                    playlist.shuffle()
                    playlist.remove(shuffleSong)
                    playlist.add(0, shuffleSong)
                    replacePlaylist(playlist, 0)
                }
            }
        }

        homeHistoryButton.setOnClickListener {
            customFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeHistoryFragment())
                .addToBackStack(null)
                .commit()
        }

        homePlaylistButton.setOnClickListener {
            customFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomePlaylistFragment())
                .addToBackStack(null)
                .commit()
        }

        topAppBar.setNavigationOnClickListener {
            // Allow open drawer if only initialization have been completed.
            if (isInitialized) {
                MainActivity.switchDrawer()
            }
        }

        shuffleRefreshButton.setOnClickListener {
            refreshList()
        }

        var isShow = true
        var scrollRange = -1

        appBarLayout.addOnOffsetChangedListener { barLayout, verticalOffset ->
            if (scrollRange == -1) {
                scrollRange = barLayout?.totalScrollRange!!
            }
            if (scrollRange + verticalOffset == 0) {
                collapsingToolbar.title = getString(R.string.app_name)
                isShow = true
            } else if (isShow) {
                collapsingToolbar.title =
                        getString(R.string.home_greetings)
                isShow = false
            }
        }

        return rootView
    }

    override fun onResume() {
        super.onResume()
        // Set the current fragment to library
        MainActivity.switchNavigationViewIndex(2)
    }

    companion object {
        private val shuffleList: MutableList<Song> = mutableListOf()
        private val recentList: MutableList<Song> = mutableListOf()
        private var isInitialized: Boolean = true
        private lateinit var loadingPrompt: MaterialCardView
        private lateinit var shuffleAdapter: SongCarouselAdapter
        private lateinit var recentAdapter: SongCarouselAdapter

        /**
         * This is used for outer class to switch [loadingPrompt].
         * e.g. When loading from disk completed.
         *
         * @param operation
         * @throws IllegalArgumentException
         */
        fun switchPrompt(operation: Int) {
            if (::loadingPrompt.isInitialized) {
                when (operation) {
                    0 -> {
                        loadingPrompt.visibility = VISIBLE
                        ObjectAnimator.ofFloat(loadingPrompt, "alpha", 0f, 1f)
                            .setDuration(200)
                            .start()
                        isInitialized = false
                    }

                    1 -> {
                        ObjectAnimator.ofFloat(loadingPrompt, "alpha", 1f, 0f)
                            .setDuration(200)
                            .start()
                        val handler = Handler(Looper.getMainLooper())
                        val runnable = Runnable {
                            loadingPrompt.visibility = GONE
                        }
                        handler.postDelayed(runnable, 200)
                        isInitialized = true
                        initializeList()
                    }

                    else -> throw IllegalArgumentException()
                }
            }
        }

        private fun initializeList() {
            if (shuffleList.isEmpty() && libraryViewModel.librarySongList.isNotEmpty()) {
                shuffleList.add(libraryViewModel.librarySongList.random())
                shuffleList.add(libraryViewModel.librarySongList.random())
                shuffleList.add(libraryViewModel.librarySongList.random())
                shuffleList.add(libraryViewModel.librarySongList.random())
                shuffleList.add(libraryViewModel.librarySongList.random())
                shuffleAdapter.notifyItemRangeChanged(0, 5)
            }
            if (libraryViewModel.libraryNewestAddedList.isNotEmpty() && recentList.isEmpty()) {
                recentList.addAll(0, libraryViewModel.libraryNewestAddedList)
                recentAdapter.notifyItemRangeChanged(0, 10)
            }
        }

        /**
         * [refreshList] refreshes shuffleList.
         * It is used for the shuffle button.
         */
        fun refreshList() {
            if (shuffleList.isNotEmpty()) {
                shuffleList.clear()
            }
            if (libraryViewModel.librarySongList.isNotEmpty()) {
                shuffleList.add(libraryViewModel.librarySongList.random())
                shuffleList.add(libraryViewModel.librarySongList.random())
                shuffleList.add(libraryViewModel.librarySongList.random())
                shuffleList.add(libraryViewModel.librarySongList.random())
                shuffleList.add(libraryViewModel.librarySongList.random())
                shuffleAdapter.notifyItemRangeChanged(0, 5)
            }
        }
    }
}
