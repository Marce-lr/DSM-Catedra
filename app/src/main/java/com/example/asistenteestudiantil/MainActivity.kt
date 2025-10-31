package com.example.asistenteestudiantil

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        
        // Verificar si el usuario ya está autenticado
        verificarAutenticacion()
    }
    
    private fun verificarAutenticacion() {
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            // Usuario autenticado, ir a actividades
            irAActividades()
        } else {
            // Usuario no autenticado, ir al login
            irALogin()
        }
    }
    
    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun irAActividades() {
        val intent = Intent(this, ActividadesActivity::class.java)
        startActivity(intent)
        finish()
    }
}