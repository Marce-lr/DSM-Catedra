package com.example.asistenteestudiantil.models

data class Course(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val professor: String = "",
    val location: String = "",
    val color: Int = 0,
    val credits: Int = 0,
    val semester: String = "",
    val schedules: List<Schedule> = emptyList(),
    val classDays: List<Int> = emptyList(),
    val globalPercentage: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)