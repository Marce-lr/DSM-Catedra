package com.example.asistenteestudiantil.utils

import android.net.Uri
import com.example.asistenteestudiantil.models.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

object NotesService {
	private fun uid(): String = FirebaseAuth.getInstance().currentUser?.uid
		?: throw IllegalStateException("Usuario no autenticado")
	private fun db(): DatabaseReference = FirebaseDatabase.getInstance().getReference("users").child(uid()).child("notes")
    private fun storage(): FirebaseStorage = FirebaseStorage.getInstance("gs://asistenteestudiantildb.appspot.com")

	fun notesRef(): DatabaseReference = db()

	fun uploadNote(subjectId: String, fileUri: Uri, fileName: String, mimeType: String, sizeBytes: Long, onDone: (Result<Note>) -> Unit) {
		val noteId = db().push().key!!
		val storagePath = "users/${uid()}/notes/${noteId}/${fileName}"
		val ref = storage().getReference().child(storagePath)
		ref.putFile(fileUri).continueWithTask { task ->
			if (!task.isSuccessful) throw task.exception ?: RuntimeException("Upload failed")
			ref.downloadUrl
		}
			.addOnSuccessListener { downloadUri ->
				val note = Note(
					id = noteId,
					subjectId = subjectId,
					name = fileName,
					mimeType = mimeType,
					sizeBytes = sizeBytes,
					storagePath = storagePath,
					downloadUrl = downloadUri.toString()
				)
				db().child(noteId).setValue(note).addOnSuccessListener { onDone(Result.success(note)) }
					.addOnFailureListener { onDone(Result.failure(it)) }
			}
			.addOnFailureListener { onDone(Result.failure(it)) }
	}

	fun deleteNote(note: Note, onDone: (Result<Unit>) -> Unit) {
		storage().getReference().child(note.storagePath).delete()
			.addOnSuccessListener {
				db().child(note.id).removeValue().addOnSuccessListener { onDone(Result.success(Unit)) }
					.addOnFailureListener { onDone(Result.failure(it)) }
			}
			.addOnFailureListener { onDone(Result.failure(it)) }
	}

fun map(snapshot: DataSnapshot): List<Note> = snapshot.children.mapNotNull { it.getValue(Note::class.java) }
}


