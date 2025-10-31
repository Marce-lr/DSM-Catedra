package com.example.asistenteestudiantil.utils

import android.util.Patterns

object Validaciones {
    
    fun validarCorreo(correo: String): Pair<Boolean, String> {
        return when {
            correo.isEmpty() -> Pair(false, "El correo es requerido")
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> 
                Pair(false, "El correo no es válido")
            else -> Pair(true, "")
        }
    }
    
    fun validarContrasena(contrasena: String): Pair<Boolean, String> {
        return when {
            contrasena.isEmpty() -> Pair(false, "La contraseña es requerida")
            contrasena.length < 6 -> 
                Pair(false, "La contraseña debe tener al menos 6 caracteres")
            !contrasena.any { it.isUpperCase() } -> 
                Pair(false, "La contraseña debe contener al menos una mayúscula")
            !contrasena.any { it.isLowerCase() } -> 
                Pair(false, "La contraseña debe contener al menos una minúscula")
            !contrasena.any { it.isDigit() } -> 
                Pair(false, "La contraseña debe contener al menos un número")
            else -> Pair(true, "")
        }
    }
    
    fun validarNombre(nombre: String): Pair<Boolean, String> {
        return when {
            nombre.isEmpty() -> Pair(false, "El nombre es requerido")
            nombre.length < 3 -> Pair(false, "El nombre debe tener al menos 3 caracteres")
            !nombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) -> 
                Pair(false, "El nombre solo debe contener letras")
            else -> Pair(true, "")
        }
    }
    
    fun validarUniversidad(universidad: String): Pair<Boolean, String> {
        return when {
            universidad.isEmpty() -> Pair(false, "La universidad es requerida")
            universidad.length < 3 -> 
                Pair(false, "El nombre de la universidad debe tener al menos 3 caracteres")
            else -> Pair(true, "")
        }
    }
    
    fun validarCarrera(carrera: String): Pair<Boolean, String> {
        return when {
            carrera.isEmpty() -> Pair(false, "La carrera es requerida")
            carrera.length < 3 -> 
                Pair(false, "El nombre de la carrera debe tener al menos 3 caracteres")
            else -> Pair(true, "")
        }
    }
    
    fun validarConfirmacionContrasena(
        contrasena: String, 
        confirmacion: String
    ): Pair<Boolean, String> {
        return when {
            confirmacion.isEmpty() -> 
                Pair(false, "Debe confirmar la contraseña")
            contrasena != confirmacion -> 
                Pair(false, "Las contraseñas no coinciden")
            else -> Pair(true, "")
        }
    }
}

