package com.isa.cuidadocompartidomayor.ui.diary.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.isa.cuidadocompartidomayor.ui.diary.tabs.MoodEntriesListFragment
import com.isa.cuidadocompartidomayor.ui.diary.tabs.TrendsStatsFragment
import com.isa.cuidadocompartidomayor.ui.diary.tabs.VitalSignsListFragment

/**
 * Adapter para el ViewPager2 del DiaryFragment
 * Maneja las 3 pestañas: Signos Vitales, Estados de Ánimo y Tendencias
 */
class DiaryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> VitalSignsListFragment()      // Pestaña 1: Signos Vitales
            1 -> MoodEntriesListFragment()     // Pestaña 2: Estados de Ánimo
            2 -> TrendsStatsFragment()         // Pestaña 3: Tendencias/Estadísticas
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    /**
     * Retorna el título de cada pestaña
     */
    fun getPageTitle(position: Int): String {
        return when (position) {
            0 -> "Signos Vitales"
            1 -> "Estados de Ánimo"
            2 -> "Tendencias"
            else -> ""
        }
    }
}
