package com.example.ryno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.widget.SeekBar

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

        val txtDistancia = findViewById<TextView>(R.id.txtDistanciaValor)
        val txtDistanciaValor = findViewById<TextView>(R.id.txtDistanciaValor)

        val seekBar = findViewById<SeekBar>(R.id.seekBarDistancia)

        var valorSelecionado = seekBar.progress.toFloat() //

        // Pega valor salvo anteriormente
        val distanciaSalva = sharedPref.getFloat("distanciaSelecionada", 10f) // default = 10km
        seekBar.progress = distanciaSalva.toInt()
        txtDistanciaValor.text = "Distância máxima: $distanciaSalva km"

        txtDistancia.text = "Distância máxima: ${seekBar.progress} km"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val distancia = progress.toFloat()
                txtDistancia.text = "Distância máxima: ${distancia.toInt()} km" // Atualiza o valor da distância
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        modalidades.forEach { modalidade ->
            val inflater = layoutInflater
            val checkBoxLayout = inflater.inflate(R.layout.item_checkbox_custom, containerCheckboxes, false)

            val checkBox = checkBoxLayout.findViewById<CheckBox>(R.id.customCheckBox)

            val icon = checkBoxLayout.findViewById<ImageView>(R.id.iconModalidade)

            val name = checkBoxLayout.findViewById<TextView>(R.id.nomeModalidade)

            checkBox.tag = modalidade

            name.text = modalidade
            checkBox.isChecked = preSelecionadas.contains(modalidade)

            val iconeResId = when (modalidade) {
                "Futebol" -> R.drawable.ic_perfil
                "Basquete" -> R.drawable.ic_ryno
                "Vôlei" -> R.drawable.ic_modalidades
                "Natação" -> R.drawable.ic_perfil
                else -> R.drawable.ic_ryno
            }

            icon.setImageResource(iconeResId)

            checkBoxList.add(checkBox)
            containerCheckboxes.addView(checkBoxLayout)
        }

        btnAplicar.setOnClickListener {
            val selecionadas = checkBoxList
                .filter { it.isChecked }
                .map { it.tag.toString() }

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
            val sharedPref = getSharedPreferences("filtro_prefs_$userId", MODE_PRIVATE)
            sharedPref.edit()
                .putStringSet("modalidadesSelecionadas", selecionadas.toSet())
                .putFloat("distanciaSelecionada", seekBar.progress.toFloat())
                .apply()

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
            sharedPref.edit()
                .putStringSet("modalidadesSelecionadas", selecionadas.toSet())
                .putFloat("distanciaSelecionada", seekBar.progress.toFloat())
                .apply()

            val intent = Intent(this, PerfilAlunoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}