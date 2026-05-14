package com.isa.cuidadocompartidomayor.ui.agenda.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.isa.cuidadocompartidomayor.ui.agenda.tabs.MedicalAppointmentsFragment
import com.isa.cuidadocompartidomayor.ui.agenda.tabs.NormalTasksFragment

class AgendaPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MedicalAppointmentsFragment()
            1 -> NormalTasksFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
