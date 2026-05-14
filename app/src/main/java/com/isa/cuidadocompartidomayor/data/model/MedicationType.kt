package com.isa.cuidadocompartidomayor.data.model

import com.isa.cuidadocompartidomayor.R

enum class MedicationType(val displayName: String, val iconRes: Int){
    PILL("Tableta", R.drawable.ic_pill),
    CAPSULE("Cápsula", R.drawable.ic_capsule),
    INJECTION("Inyección", R.drawable.ic_injection),
    GUMMIES("Gomitas", R.drawable.ic_gummies),
    SYRUP("Jarabe", R.drawable.ic_syrup),
    SPRAY("Spray", R.drawable.ic_spray),
    OINTMENT("Ungüento", R.drawable.ic_ointment),
    DROPS("Gotas", R.drawable.ic_drops),
    SUPPOSITORY("Supositorio", R.drawable.ic_suppository),
    CREAM("Crema", R.drawable.ic_cream),
    POWDER("Polvo", R.drawable.ic_powder),
    INHALER("Inhalador", R.drawable.ic_inhaler),
    AMPOULE("Ampolletas", R.drawable.ic_ampoule),
    HERB("Hierba", R.drawable.ic_herb);

    companion object {
        fun fromDisplayName(name: String): MedicationType {
            return entries.firstOrNull { it.displayName == name } ?: PILL
        }
    }
}
