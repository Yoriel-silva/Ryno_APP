package com.example.ryno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class TipoCadastroGoogleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tipo_cadastro_google)

        val btnProfessor = findViewById<Button>(R.id.btnProfessor)
        val btnAluno = findViewById<Button>(R.id.btnAluno)

        btnProfessor.setOnClickListener {
            val intent = Intent(this, CadastroProfessorGoogleActivity::class.java)
            intent.putExtra("tipoUsuario", "professor")
            startActivity(intent)
            finish()
        }

        btnAluno.setOnClickListener {
            val intent = Intent(this, CadastroAlunoGoogleActivity::class.java)
            intent.putExtra("tipoUsuario", "aluno")
            startActivity(intent)
            finish()
        }
    }
}