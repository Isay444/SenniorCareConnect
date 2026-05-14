package com.isa.cuidadocompartidomayor.data.model

data class Relationship (
    val id: String = "",
    val elderlyId: String = "",
    val elderlyName: String = "",
    val elderlyEmail: String = "",
    val caregiverId: String = "",
    val caregiverName: String = "",
    val caregiverEmail: String = "",
    val status: String = "pending", // "pending" | "active" | "rejected"
    val role: String = "primary", // "primary" | "secondary" | "professional" -> Cambiar en un futuro a "Formal" | "Informal"
    val permissions: List<String> = listOf("read"), // ["read", "write", "emergency"] -> Considerar quitarlos"
    val createdAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null,
    val notes: String = "",
    val elderlyProfileImageUrl: String = ""
){
    constructor() : this("",
        "",
        "",
        "",
        "",
        "",
        "",
        "pending",
        "primary",
        listOf("read"),
        0L,
        null,
        "",
        "")
}