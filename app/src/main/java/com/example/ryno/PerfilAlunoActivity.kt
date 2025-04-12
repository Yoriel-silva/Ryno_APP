package com.example.ryno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class PerfilAlunoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_aluno)


        // Codigo botão login
        val buttonLogin = findViewById<Button>(R.id.buttonConfirmar) // 1. Referencia o botão pelo ID

        buttonLogin.setOnClickListener {
            val intent = Intent(this, ModalidadeAlunoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}