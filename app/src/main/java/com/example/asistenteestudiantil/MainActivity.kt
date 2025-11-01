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
            // Usuario autenticado, ir al dashboard
            irADashboard()
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
    
    private fun irADashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}