package com.example.ryno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class ModalidadeAlunoActivity : AppCompatActivity() {
    private val modalidades = arrayOf("Futebol", "Basquete", "Vôlei", "Natação")
    private val checkBoxList = mutableListOf<CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modalidade_aluno)

        val containerCheckboxes = findViewById<LinearLayout>(R.id.containerCheckboxesFiltro)
        val btnAplicar = findViewById<Button>(R.id.btnAplicarFiltros)

        val preSelecionadas = intent.getStringArrayListExtra("modalidadesSelecionadas") ?: arrayListOf()

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

            val intent = Intent()
            intent.putStringArrayListExtra("modalidadesSelecionadas", ArrayList(selecionadas))

            setResult(RESULT_OK, intent)
            finish()
        }
    }
}