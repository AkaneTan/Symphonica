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

package org.akanework.symphonica.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.akanework.symphonica.ui.fragment.LibraryGridFragment
import org.akanework.symphonica.ui.fragment.LibraryListFragment

class NavFragmentPageAdapter(fragmentManager: FragmentActivity) :
    FragmentStateAdapter(fragmentManager) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LibraryListFragment()
            1 -> LibraryGridFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}