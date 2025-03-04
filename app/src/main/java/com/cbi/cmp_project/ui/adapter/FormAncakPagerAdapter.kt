package com.cbi.cmp_project.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cbi.cmp_project.ui.fragment.FormAncakFragment

class FormAncakPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val totalPages: Int
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = totalPages

    override fun createFragment(position: Int): Fragment {
        val fragment = FormAncakFragment.newInstance(position + 1)
        return fragment
    }
}