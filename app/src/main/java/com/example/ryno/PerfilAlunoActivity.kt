package com.example.ryno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class PerfilAlunoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_aluno)


        val buttonModalidade = findViewById<Button>(R.id.btnModalidades)
        val buttonProfessores = findViewById<Button>(R.id.btnProfessores)

        buttonModalidade.setOnClickListener {
            val intent = Intent(this, ModalidadeAlunoActivity::class.java)
            startActivity(intent)
            finish()
        }
        buttonProfessores.setOnClickListener {
            val intent = Intent(this, ProfessoresAlunoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}