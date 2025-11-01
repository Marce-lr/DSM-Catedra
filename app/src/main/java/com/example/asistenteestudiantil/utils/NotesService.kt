package com.example.asistenteestudiantil.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.asistenteestudiantil.models.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object NotesService {
	private fun uid(): String = FirebaseAuth.getInstance().currentUser?.uid
		?: throw IllegalStateException("Usuario no autenticado")
	private fun db(): DatabaseReference = FirebaseDatabase.getInstance().getReference("users").child(uid()).child("notes")

	fun notesRef(): DatabaseReference = db()

	/**
	 * Obtiene el directorio donde se guardarán los archivos de apuntes
	 */
	private fun getNotesDirectory(context: Context): File {
		val notesDir = File(context.filesDir, "notes")
		if (!notesDir.exists()) {
			notesDir.mkdirs()
		}
		return notesDir
	}

	/**
	 * Guarda un archivo localmente y guarda los metadatos en Firebase Database
	 */
	fun uploadNote(
		subjectId: String, 
		fileUri: Uri, 
		fileName: String, 
		mimeType: String, 
		sizeBytes: Long,
		contentResolver: ContentResolver,
		context: Context,
		onDone: (Result<Note>) -> Unit
	) {
		// Verificar que el usuario esté autenticado
		val currentUser = FirebaseAuth.getInstance().currentUser
		if (currentUser == null) {
			onDone(Result.failure(IllegalStateException("Usuario no autenticado. Por favor, inicia sesión.")))
			return
		}
		
		val noteId = db().push().key!!
		val notesDir = getNotesDirectory(context)
		val localFile = File(notesDir, "${noteId}_${fileName}")
		
		// Copiar el archivo al almacenamiento local
		try {
			contentResolver.openInputStream(fileUri)?.use { inputStream ->
				FileOutputStream(localFile).use { outputStream ->
					inputStream.copyTo(outputStream)
				}
			} ?: run {
				onDone(Result.failure(IllegalStateException("No se pudo abrir el archivo para leer")))
				return
			}
			
			// Crear el objeto Note con la ruta local
			val note = Note(
				id = noteId,
				subjectId = subjectId,
				name = fileName,
				mimeType = mimeType,
				sizeBytes = sizeBytes,
				localFilePath = localFile.absolutePath
			)
			
			// Guardar los metadatos en Firebase Database
			db().child(noteId).setValue(note)
				.addOnSuccessListener { onDone(Result.success(note)) }
				.addOnFailureListener { e ->
					// Si falla guardar en Firebase, eliminar el archivo local
					localFile.delete()
					onDone(Result.failure(e))
				}
		} catch (e: Exception) {
			// Si hay error, eliminar el archivo si se creó
			if (localFile.exists()) {
				localFile.delete()
			}
			onDone(Result.failure(e))
		}
	}

	/**
	 * Elimina un apunte (tanto el archivo local como los metadatos en Firebase)
	 */
	fun deleteNote(note: Note, context: Context, onDone: (Result<Unit>) -> Unit) {
		// Eliminar el archivo local
		if (note.localFilePath.isNotEmpty()) {
			val file = File(note.localFilePath)
			if (file.exists()) {
				file.delete()
			}
		}
		
		// Eliminar los metadatos de Firebase Database
		db().child(note.id).removeValue()
			.addOnSuccessListener { onDone(Result.success(Unit)) }
			.addOnFailureListener { onDone(Result.failure(it)) }
	}

	fun map(snapshot: DataSnapshot): List<Note> = snapshot.children.mapNotNull { it.getValue(Note::class.java) }
}


