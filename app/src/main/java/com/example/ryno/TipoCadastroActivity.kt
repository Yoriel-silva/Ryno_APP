package com.example.ryno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class TipoCadastroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tipo_cadastro)

        val btnProfessor = findViewById<Button>(R.id.btnProfessor)
        val btnAluno = findViewById<Button>(R.id.btnAluno)

        btnProfessor.setOnClickListener {
            startActivity(Intent(this, CadastroProfessorActivity::class.java))
        }

        btnAluno.setOnClickListener {
            startActivity(Intent(this, CadastroAlunoActivity::class.java))
        }
    }
}