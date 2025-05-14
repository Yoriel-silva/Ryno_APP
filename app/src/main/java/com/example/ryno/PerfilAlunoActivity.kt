package com.example.ryno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class PerfilAlunoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_aluno)


        val buttonHistorico = findViewById<Button>(R.id.btnHistorico)
        val buttonInformações = findViewById<Button>(R.id.btnInformacoes)

        val buttonModalidade = findViewById<ImageButton>(R.id.btnModalidades)
        val buttonProfessores = findViewById<ImageButton>(R.id.btnProfessores)

        buttonHistorico.setOnClickListener {
            val intent = Intent(this, HistoricoActivity::class.java)
            startActivity(intent)
            finish()
        }
        buttonInformações.setOnClickListener {
            val intent = Intent(this, InformacoesActivity::class.java)
            startActivity(intent)
            finish()
        }

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