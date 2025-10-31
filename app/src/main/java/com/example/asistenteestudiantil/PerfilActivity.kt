package com.example.asistenteestudiantil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.asistenteestudiantil.databinding.ActivityPerfilBinding
import com.example.asistenteestudiantil.databinding.DialogCambiarContrasenaBinding
import com.example.asistenteestudiantil.models.Usuario
import com.example.asistenteestudiantil.utils.Validaciones
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.android.material.bottomnavigation.BottomNavigationView

class PerfilActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPerfilBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var usuarioActual: Usuario? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        verificarSesion()
        cargarDatosUsuario()
        configurarListeners()

		// Bottom navigation
		findViewById<BottomNavigationView>(R.id.bottomNav).apply {
			selectedItemId = R.id.nav_perfil
			setOnItemSelectedListener { item ->
				when (item.itemId) {
					R.id.nav_perfil -> true
					R.id.nav_actividades -> {
						startActivity(Intent(this@PerfilActivity, ActividadesActivity::class.java))
						finish()
						true
					}
					R.id.nav_apuntes -> { startActivity(Intent(this@PerfilActivity, ApuntesActivity::class.java)); true }
					else -> false
				}
			}
		}
    }
    
    private fun verificarSesion() {
        if (auth.currentUser == null) {
            irALogin()
        }
    }
    
    private fun configurarListeners() {
        binding.btnGuardar.setOnClickListener {
            validarYGuardarCambios()
        }
        
        binding.btnCambiarContrasena.setOnClickListener {
            mostrarDialogCambiarContrasena()
        }
        
        binding.btnCerrarSesion.setOnClickListener {
            mostrarDialogCerrarSesion()
        }
    }
    
    private fun cargarDatosUsuario() {
        val user = auth.currentUser ?: return
        
        mostrarCargando(true)
        
        db.collection("usuarios")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                mostrarCargando(false)
                
                if (document.exists()) {
                    usuarioActual = Usuario(
                        uid = document.getString("uid") ?: "",
                        nombre = document.getString("nombre") ?: "",
                        correo = document.getString("correo") ?: "",
                        universidad = document.getString("universidad") ?: "",
                        carrera = document.getString("carrera") ?: "",
                        fechaCreacion = document.getLong("fechaCreacion") ?: 0
                    )
                    
                    mostrarDatosEnFormulario(usuarioActual!!)
                } else {
                    Toast.makeText(
                        this,
                        "No se encontraron datos del usuario",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                mostrarCargando(false)
                Toast.makeText(
                    this,
                    "Error al cargar datos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    
    private fun mostrarDatosEnFormulario(usuario: Usuario) {
        binding.etNombre.setText(usuario.nombre)
        binding.etCorreo.setText(usuario.correo)
        binding.etUniversidad.setText(usuario.universidad)
        binding.etCarrera.setText(usuario.carrera)
    }
    
    private fun validarYGuardarCambios() {
        val nombre = binding.etNombre.text.toString().trim()
        val universidad = binding.etUniversidad.text.toString().trim()
        val carrera = binding.etCarrera.text.toString().trim()
        
        // Limpiar errores previos
        binding.tilNombre.error = null
        binding.tilUniversidad.error = null
        binding.tilCarrera.error = null
        
        var hayError = false
        
        // Validar nombre
        val (nombreValido, errorNombre) = Validaciones.validarNombre(nombre)
        if (!nombreValido) {
            binding.tilNombre.error = errorNombre
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
        
        if (!hayError) {
            guardarCambios(nombre, universidad, carrera)
        }
    }
    
    private fun guardarCambios(nombre: String, universidad: String, carrera: String) {
        val user = auth.currentUser ?: return
        
        mostrarCargando(true)
        
        val datosActualizados = mapOf(
            "nombre" to nombre,
            "universidad" to universidad,
            "carrera" to carrera
        )
        
        db.collection("usuarios")
            .document(user.uid)
            .set(datosActualizados, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    getString(R.string.perfil_actualizado),
                    Toast.LENGTH_SHORT
                ).show()
                // Actualizar usuario actual
                usuarioActual = usuarioActual?.copy(
                    nombre = nombre,
                    universidad = universidad,
                    carrera = carrera
                )
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    getString(R.string.perfil_error) + ": ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnCompleteListener {
                // Failsafe para no quedar en loading aunque haya error inesperado
                mostrarCargando(false)
            }
    }
    
    private fun mostrarDialogCambiarContrasena() {
        val dialogBinding = DialogCambiarContrasenaBinding.inflate(LayoutInflater.from(this))
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.cambiar_contrasena_boton, null)
            .setNegativeButton(R.string.cancelar, null)
            .create()
        
        dialog.setOnShowListener {
            val btnPositivo = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnPositivo.setOnClickListener {
                val contrasenaActual = dialogBinding.etContrasenaActual.text.toString()
                val nuevaContrasena = dialogBinding.etNuevaContrasena.text.toString()
                val confirmarNuevaContrasena = dialogBinding.etConfirmarNuevaContrasena.text.toString()
                
                if (validarCambioContrasena(
                        dialogBinding,
                        contrasenaActual,
                        nuevaContrasena,
                        confirmarNuevaContrasena
                    )
                ) {
                    cambiarContrasena(contrasenaActual, nuevaContrasena, dialog)
                }
            }
        }
        
        dialog.show()
    }
    
    private fun validarCambioContrasena(
        dialogBinding: DialogCambiarContrasenaBinding,
        contrasenaActual: String,
        nuevaContrasena: String,
        confirmarNuevaContrasena: String
    ): Boolean {
        // Limpiar errores previos
        dialogBinding.tilContrasenaActual.error = null
        dialogBinding.tilNuevaContrasena.error = null
        dialogBinding.tilConfirmarNuevaContrasena.error = null
        
        var hayError = false
        
        // Validar contraseña actual
        if (contrasenaActual.isEmpty()) {
            dialogBinding.tilContrasenaActual.error = "La contraseña actual es requerida"
            hayError = true
        }
        
        // Validar nueva contraseña
        val (contrasenaValida, errorContrasena) = Validaciones.validarContrasena(nuevaContrasena)
        if (!contrasenaValida) {
            dialogBinding.tilNuevaContrasena.error = errorContrasena
            hayError = true
        }
        
        // Validar confirmación
        val (confirmacionValida, errorConfirmacion) = 
            Validaciones.validarConfirmacionContrasena(nuevaContrasena, confirmarNuevaContrasena)
        if (!confirmacionValida) {
            dialogBinding.tilConfirmarNuevaContrasena.error = errorConfirmacion
            hayError = true
        }
        
        // Validar que la nueva contraseña sea diferente
        if (contrasenaActual == nuevaContrasena) {
            dialogBinding.tilNuevaContrasena.error = 
                "La nueva contraseña debe ser diferente a la actual"
            hayError = true
        }
        
        return !hayError
    }
    
    private fun cambiarContrasena(
        contrasenaActual: String,
        nuevaContrasena: String,
        dialog: AlertDialog
    ) {
        val user = auth.currentUser ?: return
        val correo = user.email ?: return
        
        mostrarCargando(true)
        
        // Reautenticar usuario
        val credential = EmailAuthProvider.getCredential(correo, contrasenaActual)
        
        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Cambiar contraseña
                user.updatePassword(nuevaContrasena)
                    .addOnSuccessListener {
                        mostrarCargando(false)
                        Toast.makeText(
                            this,
                            getString(R.string.cambiar_contrasena_exito),
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        mostrarCargando(false)
                        Toast.makeText(
                            this,
                            getString(R.string.cambiar_contrasena_error) + ": ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                mostrarCargando(false)
                Toast.makeText(
                    this,
                    "La contraseña actual es incorrecta",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    
    private fun mostrarDialogCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro que deseas cerrar sesión?")
            .setPositiveButton(R.string.aceptar) { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }
    
    private fun cerrarSesion() {
        auth.signOut()
        irALogin()
    }
    
    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnGuardar.isEnabled = !mostrar
        binding.btnCambiarContrasena.isEnabled = !mostrar
        binding.btnCerrarSesion.isEnabled = !mostrar
        binding.etNombre.isEnabled = !mostrar
        binding.etUniversidad.isEnabled = !mostrar
        binding.etCarrera.isEnabled = !mostrar
    }
    
    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

