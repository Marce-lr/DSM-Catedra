package com.example.asistenteestudiantil.models

enum class ActivityType { TAREA, EXAMEN, EVALUACION }
enum class ActivityPriority { BAJA, MEDIA, ALTA }
enum class ActivityStatus { PENDIENTE, EN_PROGRESO, COMPLETADA }

data class AcademicActivity(
	val id: String = "",
	val subjectId: String = "", // Para compatibilidad con Subject antiguo
	val courseId: String = "", // Nueva relación con Course
	val title: String = "",
	val description: String = "",
	val type: ActivityType = ActivityType.TAREA,
	val priority: ActivityPriority = ActivityPriority.MEDIA,
	val status: ActivityStatus = ActivityStatus.PENDIENTE,
	val dueDateMillis: Long = 0L,
	val weightPercent: Double = 0.0,
	val scoreObtained: Double? = null,
	val createdAt: Long = System.currentTimeMillis(),
	val updatedAt: Long = System.currentTimeMillis()
) {
	fun contributionToGlobal(): Double {
		if (status != ActivityStatus.COMPLETADA) return 0.0
		val score = scoreObtained ?: return 0.0
		return (score.coerceIn(0.0, 10.0) / 10.0) * weightPercent
	}
}


