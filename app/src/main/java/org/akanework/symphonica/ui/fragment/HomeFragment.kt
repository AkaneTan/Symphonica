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

package org.akanework.symphonica.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.R

/**
 * [HomeFragment] is homepage fragment.
 */
class HomeFragment : Fragment() {

    companion object {

        private lateinit var loadingPrompt: MaterialCardView

        private var isInitialized: Boolean = true

        /**
         * This is used for outer class to switch [loadingPrompt].
         * e.g. When loading from disk completed.
         */
        fun switchPrompt(operation: Int) {
            if (::loadingPrompt.isInitialized) {
                when (operation) {
                    0 -> {
                        loadingPrompt.visibility = View.VISIBLE
                        isInitialized = false
                    }
                    1 -> {
                        loadingPrompt.visibility = View.GONE
                        isInitialized = true
                    }
                    else -> {
                        throw IllegalArgumentException()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        val collapsingToolbar =
            rootView.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        val appBarLayout = rootView.findViewById<AppBarLayout>(R.id.appBarLayout)

        loadingPrompt = rootView.findViewById(R.id.loading_prompt_list)

        topAppBar.setNavigationOnClickListener {
            // Allow open drawer if only initialization have been completed.
            if (isInitialized) {
                MainActivity.switchDrawer()
            }
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
}