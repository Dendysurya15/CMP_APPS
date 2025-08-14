package com.cbi.mobile_plantation.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cbi.mobile_plantation.ui.fragment.FormAncakFragment

class FormAncakPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val totalPages: Int,
    private val featureName: String? = null  // <- Add this parameter
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = totalPages

    override fun createFragment(position: Int): Fragment {
        val pageNumber = position + 1
        // Pass both pageNumber AND featureName to the fragment
        return FormAncakFragment.newInstance(pageNumber, featureName)
    }
}