package org.akanework.symphonica.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.akanework.symphonica.ui.fragment.LibraryGridFragment
import org.akanework.symphonica.ui.fragment.LibraryListFragment

class NavFragmentPageAdapter(fragmentManager: FragmentManager?) :
    FragmentPagerAdapter(fragmentManager!!) {
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                LibraryListFragment()
            }

            1 -> {
                LibraryGridFragment()
            }

            else -> {
                LibraryListFragment()
            }
        }
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> {
                "List"
            }

            1 -> {
                "Album"
            }

            else -> {
                "List"
            }
        }
    }
}