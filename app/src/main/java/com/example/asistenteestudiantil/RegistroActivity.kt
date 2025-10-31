package com.example.asistenteestudiantil

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.asistenteestudiantil.databinding.ActivityRegistroBinding
import com.example.asistenteestudiantil.models.Usuario
import com.example.asistenteestudiantil.utils.Validaciones
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistroActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegistroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        configurarListeners()
    }
    
    private fun configurarListeners() {
        binding.btnRegistro.setOnClickListener {
            validarYRegistrar()
        }
        
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
    
    private fun validarYRegistrar() {
        val nombre = binding.etNombre.text.toString().trim()
        val correo = binding.etCorreo.text.toString().trim()
        val universidad = binding.etUniversidad.text.toString().trim()
        val carrera = binding.etCarrera.text.toString().trim()
        val contrasena = binding.etContrasena.text.toString()
        val confirmarContrasena = binding.etConfirmarContrasena.text.toString()
        
        // Limpiar errores previos
        binding.tilNombre.error = null
        binding.tilCorreo.error = null
        binding.tilUniversidad.error = null
        binding.tilCarrera.error = null
        binding.tilContrasena.error = null
        binding.tilConfirmarContrasena.error = null
        
        var hayError = false
        
        // Validar nombre
        val (nombreValido, errorNombre) = Validaciones.validarNombre(nombre)
        if (!nombreValido) {
            binding.tilNombre.error = errorNombre
            hayError = true
        }
        
        // Validar correo
        val (correoValido, errorCorreo) = Validaciones.validarCorreo(correo)
        if (!correoValido) {
            binding.tilCorreo.error = errorCorreo
            hayError = true
        }
        
        // Validar universidad
        val (universidadValida, errorUniversidad) = Validaciones.validarUniversidad(universidad)
        if (!universidadValida) {
            binding.tilUniversidad.error = errorUniversidad
            hayError = true
        }
        
        // Validar carrera
        val (carreraValida, errorCarrera) = Validaciones.validarCarrera(carrera)
        if (!carreraValida) {
            binding.tilCarrera.error = errorCarrera
            hayError = true
        }
        
        // Validar contraseña
        val (contrasenaValida, errorContrasena) = Validaciones.validarContrasena(contrasena)
        if (!contrasenaValida) {
            binding.tilContrasena.error = errorContrasena
            hayError = true
        }
        
        // Validar confirmación de contraseña
        val (confirmacionValida, errorConfirmacion) = 
            Validaciones.validarConfirmacionContrasena(contrasena, confirmarContrasena)
        if (!confirmacionValida) {
            binding.tilConfirmarContrasena.error = errorConfirmacion
            hayError = true
        }
        
        if (!hayError) {
            registrarUsuario(nombre, correo, universidad, carrera, contrasena)
        }
    }
    
    private fun registrarUsuario(
        nombre: String,
        correo: String,
        universidad: String,
        carrera: String,
        contrasena: String
    ) {
        mostrarCargando(true)
        
        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registro exitoso, guardar datos adicionales en Firestore
                    val user = auth.currentUser
                    if (user != null) {
                        guardarDatosUsuario(user.uid, nombre, correo, universidad, carrera)
                    }
                } else {
                    mostrarCargando(false)
                    val mensajeError = when {
                        task.exception?.message?.contains("email address is already in use") == true ->
                            "Este correo ya está registrado"
                        task.exception?.message?.contains("network error") == true ->
                            getString(R.string.error_conexion)
                        else -> getString(R.string.registro_error) + ": ${task.exception?.message}"
                    }
                    Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun guardarDatosUsuario(
        uid: String,
        nombre: String,
        correo: String,
        universidad: String,
        carrera: String
    ) {
        val usuario = Usuario(
            uid = uid,
            nombre = nombre,
            correo = correo,
            universidad = universidad,
            carrera = carrera
        )
        
        db.collection("usuarios")
            .document(uid)
            .set(usuario.toMap())
            .addOnSuccessListener {
                mostrarCargando(false)
                Toast.makeText(
                    this,
                    getString(R.string.registro_exito),
                    Toast.LENGTH_SHORT
                ).show()
                irAPerfil()
            }
            .addOnFailureListener { e ->
                mostrarCargando(false)
                Toast.makeText(
                    this,
                    "Error al guardar datos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    
    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnRegistro.isEnabled = !mostrar
        binding.etNombre.isEnabled = !mostrar
        binding.etCorreo.isEnabled = !mostrar
        binding.etUniversidad.isEnabled = !mostrar
        binding.etCarrera.isEnabled = !mostrar
        binding.etContrasena.isEnabled = !mostrar
        binding.etConfirmarContrasena.isEnabled = !mostrar
        binding.tvLogin.isEnabled = !mostrar
    }
    
    private fun irAPerfil() {
        val intent = Intent(this, PerfilActivity::class.java)
        startActivity(intent)
        finish()
    }
}

