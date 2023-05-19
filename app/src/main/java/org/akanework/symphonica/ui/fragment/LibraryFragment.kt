package org.akanework.symphonica.ui.fragment

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.size
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.akanework.symphonica.MainActivity
import org.akanework.symphonica.MainActivity.Companion.songList
import org.akanework.symphonica.MainActivity.Companion.switchNavigationView
import org.akanework.symphonica.R
import org.akanework.symphonica.logic.data.loadDataFromCache
import org.akanework.symphonica.logic.data.loadDataFromDisk
import org.akanework.symphonica.logic.data.reloadRecyclerView
import org.akanework.symphonica.ui.adapter.NavFragmentPageAdapter
import kotlin.concurrent.thread

class LibraryFragment : Fragment() {

    private var isDrawerOpen = false
    private lateinit var fragmentPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true).setDuration(500)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false).setDuration(500)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_library, container, false)

        // Inflate the layout for this fragment
        fragmentPager = rootView.findViewById(R.id.fragmentSwitch)
        val libraryTabLayout: TabLayout = rootView.findViewById(R.id.library_tablayout)
        val topAppBar: MaterialToolbar = rootView.findViewById(R.id.topAppBar)
        val animator: ObjectAnimator = ObjectAnimator.ofFloat(rootView, "translationX", 0f, 500f)

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

        fragmentPager.adapter = NavFragmentPageAdapter(requireActivity())
        TabLayoutMediator(libraryTabLayout, fragmentPager) { tab, position ->
            // 设置每个选项卡的文本
            tab.text = when (position) {
                0 -> "List"
                1 -> "Album"
                else -> "Unknown"
            }
        }.attach()
        return rootView
    }

}