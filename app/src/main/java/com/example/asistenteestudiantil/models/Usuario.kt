package com.example.asistenteestudiantil.models

data class Usuario(
    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val universidad: String = "",
    val carrera: String = "",
    val fechaCreacion: Long = System.currentTimeMillis()
) {
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", "", "", "", 0)
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "nombre" to nombre,
            "correo" to correo,
            "universidad" to universidad,
            "carrera" to carrera,
            "fechaCreacion" to fechaCreacion
        )
    }
}

