package com.isa.cuidadocompartidomayor.ui.medications.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.isa.cuidadocompartidomayor.ui.medications.tabs.MedicationListFragment
import com.isa.cuidadocompartidomayor.ui.medications.tabs.MedicationLogsFragment

class MedicationsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MedicationListFragment()
            1 -> MedicationLogsFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}

