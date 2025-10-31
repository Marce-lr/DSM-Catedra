package com.example.asistenteestudiantil.models

data class Note(
	val id: String = "",
	val subjectId: String = "",
	val name: String = "",
	val mimeType: String = "",
	val sizeBytes: Long = 0L,
	val storagePath: String = "",
	val downloadUrl: String = "",
	val createdAt: Long = System.currentTimeMillis()
)


