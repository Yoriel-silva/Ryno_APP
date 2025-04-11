package com.example.ryno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CadastroAlunoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_aluno)

        // Referência aos elementos da interface
        val nomeEditText = findViewById<EditText>(R.id.NomeAluno)
        val emailEditText = findViewById<EditText>(R.id.EmailAluno)
        val telefoneEditText = findViewById<EditText>(R.id.TelefoneAluno)
        val senhaEditText = findViewById<EditText>(R.id.SenhaAluno)
        val checkBox = findViewById<CheckBox>(R.id.checkBox)
        val botaoCriar = findViewById<Button>(R.id.Btn_CriarAluno)

        botaoCriar.setOnClickListener {
            val nome = nomeEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val telefone = telefoneEditText.text.toString().trim()
            val senha = senhaEditText.text.toString().trim()

            // Verificações básicas
            if (nome.isEmpty() || email.isEmpty() || telefone.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!checkBox.isChecked) {
                Toast.makeText(this, "Você precisa aceitar os termos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Aqui você pode salvar os dados no Firebase, banco local, etc.
            Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()

            // Redirecionar para a home do aluno (vamos criar essa activity depois)
            val intent = Intent(this, HomeAlunoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}