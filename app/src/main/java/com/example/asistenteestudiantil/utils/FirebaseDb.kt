package com.example.asistenteestudiantil.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.example.asistenteestudiantil.models.AcademicActivity
import com.example.asistenteestudiantil.models.Subject

object FirebaseDb {
	private fun db(): FirebaseDatabase = FirebaseDatabase.getInstance()
	private fun uid(): String = FirebaseAuth.getInstance().currentUser?.uid
		?: throw IllegalStateException("Usuario no autenticado")

	fun userRef(): DatabaseReference = db().getReference("users").child(uid())
	fun subjectsRef(): DatabaseReference = userRef().child("subjects")
	fun subjectRef(subjectId: String): DatabaseReference = subjectsRef().child(subjectId)
	fun activitiesRef(subjectId: String): DatabaseReference = subjectRef(subjectId).child("activities")

	fun createOrUpdateSubject(subject: Subject) {
		val id = if (subject.id.isBlank()) subjectsRef().push().key!! else subject.id
		subjectsRef().child(id).setValue(subject.copy(id = id))
	}

	fun deleteSubject(subjectId: String) {
		subjectRef(subjectId).removeValue()
	}

	fun createOrUpdateActivity(activity: AcademicActivity) {
		val id = if (activity.id.isBlank()) activitiesRef(activity.subjectId).push().key!! else activity.id
		activitiesRef(activity.subjectId).child(id).setValue(activity.copy(id = id, updatedAt = System.currentTimeMillis()))
		recalculateSubjectPercentage(activity.subjectId)
	}

	fun deleteActivity(subjectId: String, activityId: String) {
		activitiesRef(subjectId).child(activityId).removeValue()
		recalculateSubjectPercentage(subjectId)
	}

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

	fun mapSubjects(snapshot: DataSnapshot): List<Subject> = snapshot.children.mapNotNull { it.getValue(Subject::class.java) }
	fun mapActivities(snapshot: DataSnapshot): List<AcademicActivity> = snapshot.children.mapNotNull { it.getValue(AcademicActivity::class.java) }
}


