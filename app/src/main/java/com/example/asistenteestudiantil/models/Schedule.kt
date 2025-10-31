package com.example.asistenteestudiantil.models

data class Schedule(
    val id: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val dayOfWeek: Int = 0,
    val startTime: String = "",
    val endTime: String = "",
    val room: String = "",
    val professor: String = "",
    val color: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getDayName(): String {
        return when (dayOfWeek) {
            1 -> "Lunes"
            2 -> "Martes"
            3 -> "Miércoles"
            4 -> "Jueves"
            5 -> "Viernes"
            6 -> "Sábado"
            7 -> "Domingo"
            else -> "Desconocido"
        }
    }
}