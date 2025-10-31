package com.example.asistenteestudiantil.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.example.asistenteestudiantil.models.AcademicActivity
import com.example.asistenteestudiantil.models.Subject
import com.example.asistenteestudiantil.models.Schedule
import com.example.asistenteestudiantil.models.Course

object FirebaseDb {
	private fun db(): FirebaseDatabase = FirebaseDatabase.getInstance()
	private fun uid(): String = FirebaseAuth.getInstance().currentUser?.uid
		?: throw IllegalStateException("Usuario no autenticado")

	fun userRef(): DatabaseReference = db().getReference("users").child(uid())

	// Referencias para SUBJECTS (simples - ya existentes)
	fun subjectsRef(): DatabaseReference = userRef().child("subjects")
	fun subjectRef(subjectId: String): DatabaseReference = subjectsRef().child(subjectId)

	// Referencias para COURSES (completos - nuevos)
	fun coursesRef(): DatabaseReference = userRef().child("courses")
	fun courseRef(courseId: String): DatabaseReference = coursesRef().child(courseId)

	// Referencias para Actividades (se mantienen con subjects)
	fun activitiesRef(subjectId: String): DatabaseReference = subjectRef(subjectId).child("activities")

	// Referencias para Horarios (se asocian con courses)
	fun schedulesRef(): DatabaseReference = userRef().child("schedules")
	fun scheduleRef(scheduleId: String): DatabaseReference = schedulesRef().child(scheduleId)

	// CRUD para SUBJECTS (simples - ya existentes)
	fun createOrUpdateSubject(subject: Subject) {
		val id = if (subject.id.isBlank()) subjectsRef().push().key!! else subject.id
		subjectsRef().child(id).setValue(subject.copy(id = id))
	}

	fun deleteSubject(subjectId: String) {
		subjectRef(subjectId).removeValue()
	}

	// CRUD para COURSES (completos - nuevos)
	fun createOrUpdateCourse(course: Course) {
		val id = if (course.id.isBlank()) coursesRef().push().key!! else course.id
		coursesRef().child(id).setValue(course.copy(id = id))
	}

	fun deleteCourse(courseId: String) {
		courseRef(courseId).removeValue()
	}

	// CRUD para Actividades (se mantienen igual)
	fun createOrUpdateActivity(activity: AcademicActivity) {
		val id = if (activity.id.isBlank()) activitiesRef(activity.subjectId).push().key!! else activity.id
		activitiesRef(activity.subjectId).child(id).setValue(activity.copy(id = id, updatedAt = System.currentTimeMillis()))
		recalculateSubjectPercentage(activity.subjectId)
	}

	fun deleteActivity(subjectId: String, activityId: String) {
		activitiesRef(subjectId).child(activityId).removeValue()
		recalculateSubjectPercentage(subjectId)
	}

	// CRUD para Horarios (se asocian con courses)
	fun createOrUpdateSchedule(schedule: Schedule) {
		val id = if (schedule.id.isBlank()) schedulesRef().push().key!! else schedule.id
		schedulesRef().child(id).setValue(schedule.copy(id = id, updatedAt = System.currentTimeMillis()))
	}

	fun deleteSchedule(scheduleId: String) {
		scheduleRef(scheduleId).removeValue()
	}

	// Métodos de cálculo (se mantienen para subjects)
	fun recalculateSubjectPercentage(subjectId: String, onComplete: ((Double) -> Unit)? = null) {
		activitiesRef(subjectId).get().addOnSuccessListener { snapshot ->
			var total = 0.0
			snapshot.children.forEach { child ->
				val act = child.getValue(AcademicActivity::class.java)
				if (act != null) total += act.contributionToGlobal()
			}
			subjectRef(subjectId).child("globalPercentage").setValue(total)
			onComplete?.let { it(total) }
		}
	}

	// Métodos de mapeo
	fun mapSubjects(snapshot: DataSnapshot): List<Subject> =
		snapshot.children.mapNotNull { it.getValue(Subject::class.java) }

	fun mapCourses(snapshot: DataSnapshot): List<Course> =
		snapshot.children.mapNotNull { it.getValue(Course::class.java) }

	fun mapActivities(snapshot: DataSnapshot): List<AcademicActivity> =
		snapshot.children.mapNotNull { it.getValue(AcademicActivity::class.java) }

	fun mapSchedules(snapshot: DataSnapshot): List<Schedule> =
		snapshot.children.mapNotNull { it.getValue(Schedule::class.java) }
}