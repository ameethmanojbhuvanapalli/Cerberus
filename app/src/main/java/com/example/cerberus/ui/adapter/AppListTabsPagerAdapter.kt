package com.example.cerberus.ui.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.cerberus.ui.fragment.AppListFragment

class AppListTabsPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 2
    override fun createFragment(position: Int): Fragment {
        return AppListFragment.newInstance(isLocked = (position == 0))
    }
}