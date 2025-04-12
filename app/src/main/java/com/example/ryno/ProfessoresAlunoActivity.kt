package com.example.ryno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ProfessoresAlunoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_professores_aluno)


        // Codigo botão login
        val buttonLogin = findViewById<Button>(R.id.buttonConfirmar) // 1. Referencia o botão pelo ID

        buttonLogin.setOnClickListener {
            val intent = Intent(this, PerfilAlunoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}