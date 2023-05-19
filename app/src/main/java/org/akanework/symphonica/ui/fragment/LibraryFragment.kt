package org.akanework.symphonica.ui.fragment

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.delay
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.switchNavigationView
import org.akanework.symphonica.R
import org.akanework.symphonica.ui.adapter.NavFragmentPageAdapter
import kotlin.concurrent.thread

class LibraryFragment : Fragment() {

    private var isDrawerOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_library, container, false)

        // Inflate the layout for this fragment
        val fragmentPager: ViewPager = rootView.findViewById(R.id.fragmentSwitch)
        val navFragmentPageAdapter = NavFragmentPageAdapter(fragmentManager)
        val libraryTabLayout: TabLayout = rootView.findViewById(R.id.library_tablayout)
        val topAppBar: MaterialToolbar = rootView.findViewById(R.id.topAppBar)
        val animator: ObjectAnimator = ObjectAnimator.ofFloat(rootView, "translationX", 0f, 500f)

        fragmentPager.offscreenPageLimit = 3

        topAppBar.setNavigationOnClickListener {
            isDrawerOpen = if (!isDrawerOpen) {
                switchNavigationView()
                animator.setDuration(400)
                animator.start()
                true
            } else {
                animator.reverse()
                val handler = Handler()
                val runnable = Runnable {
                    switchNavigationView()
                }
                handler.postDelayed(runnable, 400)
                false
            }
        }

        libraryTabLayout.setupWithViewPager(fragmentPager)

        fragmentPager.adapter = navFragmentPageAdapter
        return rootView
    }

}