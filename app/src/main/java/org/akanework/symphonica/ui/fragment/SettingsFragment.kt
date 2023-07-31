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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import org.akanework.symphonica.BuildConfig
import org.akanework.symphonica.MainActivity.Companion.isAkaneVisible
import org.akanework.symphonica.MainActivity.Companion.isColorfulButtonEnabled
import org.akanework.symphonica.MainActivity.Companion.isEasterEggDiscovered
import org.akanework.symphonica.MainActivity.Companion.isForceDarkModeEnabled
import org.akanework.symphonica.MainActivity.Companion.isForceLoadingEnabled
import org.akanework.symphonica.MainActivity.Companion.isGlideCacheEnabled
import org.akanework.symphonica.MainActivity.Companion.isLibraryShuffleButtonEnabled
import org.akanework.symphonica.MainActivity.Companion.isListShuffleEnabled
import org.akanework.symphonica.MainActivity.Companion.switchDrawer
import org.akanework.symphonica.MainActivity.Companion.switchNavigationViewIndex
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.logic.util.broadcastSquigglyUpdate

/**
 * [SettingsFragment] is the fragment that is used
 * for settings.
 */
class SettingsFragment : Fragment() {
    private var logoClickedTimes = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment.
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)

        if (isAkaneVisible) {
            rootView.findViewById<ImageView>(R.id.akane).visibility = VISIBLE
        }

        // Define the topAppBar behavior.
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        val versionTag = rootView.findViewById<TextView>(R.id.version_tag)
        val contributors = rootView.findViewById<TextView>(R.id.contributors)
        val cacheSwitch = rootView.findViewById<MaterialSwitch>(R.id.cache_reading_switch)
        val colorfulButtonSwitch = rootView.findViewById<MaterialSwitch>(R.id.home_colorful_button)
        val reorderSwitch = rootView.findViewById<MaterialSwitch>(R.id.reading_order_switch)
        val darkModeSwitch = rootView.findViewById<MaterialSwitch>(R.id.force_dark_mode_switch)
        val symphonicaIcon = rootView.findViewById<ImageView>(R.id.symphonica_icon)
        val akanePreference = rootView.findViewById<FrameLayout>(R.id.akane_preference)
        val akaneDisplaySwitch = rootView.findViewById<MaterialSwitch>(R.id.akane_display_settings)
        val libraryShuffleButtonSwitch =
            rootView.findViewById<MaterialSwitch>(R.id.library_shuffle_button_switch)
        val enableListShuffleSwitch =
            rootView.findViewById<MaterialSwitch>(R.id.enable_list_shuffle)

        val contributorString = getString(R.string.settings_contributors_content) + ' ' +
                getString(R.string.settings_contributors_content_alphabet)
        contributors.text = contributorString

        cacheSwitch.isChecked = isGlideCacheEnabled
        colorfulButtonSwitch.isChecked = isColorfulButtonEnabled
        reorderSwitch.isChecked = isForceLoadingEnabled
        darkModeSwitch.isChecked = isForceDarkModeEnabled
        enableListShuffleSwitch.isChecked = isListShuffleEnabled
        akaneDisplaySwitch.isChecked = isAkaneVisible
        libraryShuffleButtonSwitch.isChecked = isLibraryShuffleButtonEnabled

        if (isEasterEggDiscovered) {
            akanePreference.visibility = VISIBLE
        }

        cacheSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor =
                SymphonicaApplication.context.getSharedPreferences("data", Context.MODE_PRIVATE)
                    .edit()
            isGlideCacheEnabled = if (isChecked) {
                editor.putBoolean("isGlideCacheEnabled", true)
                editor.apply()
                true
            } else {
                editor.putBoolean("isGlideCacheEnabled", false)
                editor.apply()
                false
            }
        }

        colorfulButtonSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor =
                SymphonicaApplication.context.getSharedPreferences("data", Context.MODE_PRIVATE)
                    .edit()
            isColorfulButtonEnabled = if (isChecked) {
                editor.putBoolean("isColorfulButtonEnabled", true)
                editor.apply()
                true
            } else {
                editor.putBoolean("isColorfulButtonEnabled", false)
                editor.apply()
                false
            }
        }

        reorderSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor =
                SymphonicaApplication.context.getSharedPreferences("data", Context.MODE_PRIVATE)
                    .edit()
            isForceLoadingEnabled = if (isChecked) {
                editor.putBoolean("isForceLoadingEnabled", true)
                editor.apply()
                true
            } else {
                editor.putBoolean("isForceLoadingEnabled", false)
                editor.apply()
                false
            }
        }

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor =
                SymphonicaApplication.context.getSharedPreferences("data", Context.MODE_PRIVATE)
                    .edit()
            isForceDarkModeEnabled = if (isChecked) {
                editor.putBoolean("isForceDarkModeEnabled", true)
                editor.apply()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                true
            } else {
                editor.putBoolean("isForceDarkModeEnabled", false)
                editor.apply()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                false
            }
        }

        enableListShuffleSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor =
                SymphonicaApplication.context.getSharedPreferences("data", Context.MODE_PRIVATE)
                    .edit()
            isListShuffleEnabled = if (isChecked) {
                editor.putBoolean("isListShuffleEnabled", true)
                editor.apply()
                true
            } else {
                editor.putBoolean("isListShuffleEnabled", false)
                editor.apply()
                false
            }
        }

        akaneDisplaySwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor =
                SymphonicaApplication.context.getSharedPreferences("data", Context.MODE_PRIVATE)
                    .edit()
            isAkaneVisible = if (isChecked) {
                editor.putBoolean("isAkaneVisible", true)
                editor.apply()
                requireActivity().findViewById<ImageView>(R.id.akane).visibility = VISIBLE
                true
            } else {
                editor.putBoolean("isAkaneVisible", false)
                editor.apply()
                requireActivity().findViewById<ImageView>(R.id.akane).visibility = GONE
                false
            }
        }

        libraryShuffleButtonSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor =
                SymphonicaApplication.context.getSharedPreferences("data", Context.MODE_PRIVATE)
                    .edit()
            isLibraryShuffleButtonEnabled = if (isChecked) {
                editor.putBoolean("isLibraryShuffleButtonEnabled", true)
                editor.apply()
                true
            } else {
                editor.putBoolean("isLibraryShuffleButtonEnabled", false)
                editor.apply()
                false
            }
        }

        topAppBar.setNavigationOnClickListener {
            switchDrawer()
        }

        symphonicaIcon.setOnClickListener {
            logoClickedTimes++
            if (logoClickedTimes == 10) {
                requireActivity().findViewById<ImageView>(R.id.akane).visibility = VISIBLE
                isEasterEggDiscovered = true
                val editor =
                    SymphonicaApplication.context.getSharedPreferences("data", Context.MODE_PRIVATE)
                        .edit()
                editor.putBoolean("isEasterEggDiscovered", true)
                editor.apply()
                isAkaneVisible = true
                akaneDisplaySwitch.isChecked = true
                akanePreference.visibility = VISIBLE
            }
        }

        versionTag.text = BuildConfig.VERSION_NAME

        return rootView
    }

    override fun onResume() {
        super.onResume()
        switchNavigationViewIndex(1)
    }
}
