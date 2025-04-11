package com.example.ryno


import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CadastroProfessorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_professor)

        val nome = findViewById<EditText>(R.id.NomeProfessor)
        val email = findViewById<EditText>(R.id.EmailProfessor)
        val telefone = findViewById<EditText>(R.id.TelefoneProfessor)
        val senha = findViewById<EditText>(R.id.SenhaProfessor)
        val cref = findViewById<EditText>(R.id.CREF)
        val checkbox = findViewById<CheckBox>(R.id.checkBoxConcordo)
        val botaoCriar = findViewById<Button>(R.id.Btn_CriarProfessor)

        botaoCriar.setOnClickListener {
            if (!checkbox.isChecked) {
                Toast.makeText(this, "Você deve aceitar os termos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nomeTxt = nome.text.toString()
            val emailTxt = email.text.toString()
            val telefoneTxt = telefone.text.toString()
            val senhaTxt = senha.text.toString()
            val crefTxt = cref.text.toString()

            // Aqui você poderá futuramente salvar os dados no Firebase ou outro banco
            Toast.makeText(this, "Professor $nomeTxt cadastrado com sucesso!", Toast.LENGTH_LONG).show()
        }
    }
}