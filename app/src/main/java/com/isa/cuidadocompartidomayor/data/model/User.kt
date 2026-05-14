package com.isa.cuidadocompartidomayor.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val userType: String = "", // "caregiver" | "elderly"
    val inviteCode: String = "", // código único para elderly
    val profileImageUrl: String = "",
    val phone: String = "",
    val emergencyContact: String = "",
    val address: String = "",
    val birthDate: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    constructor() : this("",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        0L,
        0L,
        true)
}
