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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.transition.MaterialSharedAxis
import org.akanework.symphonica.BuildConfig
import org.akanework.symphonica.MainActivity.Companion.isGlideCacheEnabled
import org.akanework.symphonica.MainActivity.Companion.switchDrawer
import org.akanework.symphonica.MainActivity.Companion.switchNavigationViewIndex
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication

class SettingsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition =
            MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true).setDuration(500)
        reenterTransition =
            MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false).setDuration(500)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment.
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)

        // Define the topAppBar behavior.
        val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
        val versionTag = rootView.findViewById<TextView>(R.id.version_tag)
        val cacheSwitch = rootView.findViewById<MaterialSwitch>(R.id.cache_reading_switch)

        cacheSwitch.isChecked = isGlideCacheEnabled

        cacheSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
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

        topAppBar.setNavigationOnClickListener {
            switchDrawer()
        }

        versionTag.text = getString(
            R.string.settings_version_format,
            BuildConfig.VERSION_NAME,
            BuildConfig.GIT_HASH
        )

        return rootView
    }

    override fun onResume() {
        super.onResume()
        switchNavigationViewIndex(1)
    }
}