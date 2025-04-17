package com.example.ryno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ModalidadeAlunoActivity : AppCompatActivity() {
    private val modalidades = arrayOf("Futebol", "Basquete", "Vôlei", "Natação")
    private val checkBoxList = mutableListOf<CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modalidade_aluno)

        val containerCheckboxes = findViewById<LinearLayout>(R.id.containerCheckboxesFiltro)
        val btnAplicar = findViewById<Button>(R.id.btnAplicarFiltros)
        val btnPerfil = findViewById<Button>(R.id.btnPerfil)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
        val sharedPref = getSharedPreferences("filtro_prefs_$userId", MODE_PRIVATE)
        val preSelecionadas = sharedPref.getStringSet("modalidadesSelecionadas", emptySet()) ?: emptySet()

        modalidades.forEach { modalidade ->
            val checkBox = CheckBox(this).apply {
                text = modalidade
                isChecked = preSelecionadas.contains(modalidade)}
            checkBoxList.add(checkBox)
            containerCheckboxes.addView(checkBox)
        }

        btnAplicar.setOnClickListener {
            val selecionadas = checkBoxList
                .filter { it.isChecked }
                .map { it.text.toString() }

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
            val sharedPref = getSharedPreferences("filtro_prefs_$userId", MODE_PRIVATE)
            sharedPref.edit().putStringSet("modalidadesSelecionadas", selecionadas.toSet()).apply()

            val intent = Intent(this, ProfessoresAlunoActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnPerfil.setOnClickListener {
            val selecionadas = checkBoxList
                .filter { it.isChecked }
                .map { it.text.toString() }

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
            val sharedPref = getSharedPreferences("filtro_prefs_$userId", MODE_PRIVATE)
            sharedPref.edit().putStringSet("modalidadesSelecionadas", selecionadas.toSet()).apply()

            val intent = Intent(this, PerfilAlunoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}