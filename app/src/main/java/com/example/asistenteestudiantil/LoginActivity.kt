package com.example.asistenteestudiantil

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.asistenteestudiantil.databinding.ActivityLoginBinding
import com.example.asistenteestudiantil.utils.Validaciones
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        
        configurarListeners()
    }
    
    private fun configurarListeners() {
        binding.btnLogin.setOnClickListener {
            validarEIniciarSesion()
        }
        
        binding.tvRegistro.setOnClickListener {
            irARegistro()
        }
    }
    
    private fun validarEIniciarSesion() {
        val correo = binding.etCorreo.text.toString().trim()
        val contrasena = binding.etContrasena.text.toString()
        
        // Limpiar errores previos
        binding.tilCorreo.error = null
        binding.tilContrasena.error = null
        
        // Validar correo
        val (correoValido, errorCorreo) = Validaciones.validarCorreo(correo)
        if (!correoValido) {
            binding.tilCorreo.error = errorCorreo
            return
        }
        
        // Validar contraseña no vacía
        if (contrasena.isEmpty()) {
            binding.tilContrasena.error = "La contraseña es requerida"
            return
        }
        
        // Intentar iniciar sesión
        iniciarSesion(correo, contrasena)
    }
    
    private fun iniciarSesion(correo: String, contrasena: String) {
        mostrarCargando(true)
        
        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this) { task ->
                mostrarCargando(false)
                
                if (task.isSuccessful) {
                    // Login exitoso
                    val user = auth.currentUser
                    irAActividades()
                } else {
                    // Error en el login
                    val mensajeError = when (task.exception?.message) {
                        "The email address is badly formatted." -> 
                            "El formato del correo es inválido"
                        "The password is invalid or the user does not have a password." -> 
                            getString(R.string.login_error_credenciales)
                        "There is no user record corresponding to this identifier. The user may have been deleted." -> 
                            getString(R.string.login_error_credenciales)
                        else -> "Error al iniciar sesión: ${task.exception?.message}"
                    }
                    Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !mostrar
        binding.etCorreo.isEnabled = !mostrar
        binding.etContrasena.isEnabled = !mostrar
        binding.tvRegistro.isEnabled = !mostrar
    }
    
    private fun irARegistro() {
        val intent = Intent(this, RegistroActivity::class.java)
        startActivity(intent)
    }
    
    private fun irAActividades() {
        val intent = Intent(this, ActividadesActivity::class.java)
        startActivity(intent)
        finish()
    }
}

