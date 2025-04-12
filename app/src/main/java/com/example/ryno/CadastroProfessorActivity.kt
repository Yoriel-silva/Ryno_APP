package com.example.ryno

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.Normalizer

class CadastroProfessorActivity : AppCompatActivity() {

    private val cidades = listOf(
        "São Paulo", "Salvador", "Fortaleza", "Belo Horizonte",
        "Rio de Janeiro", "Curitiba", "Brasília", "Manaus", "Recife", "Porto Alegre"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_professor)

        val nome = findViewById<EditText>(R.id.NomeProfessor)
        val email = findViewById<EditText>(R.id.EmailProfessor)
        val telefone = findViewById<EditText>(R.id.TelefoneProfessor)
        val senha = findViewById<EditText>(R.id.SenhaProfessor)
        val cref = findViewById<EditText>(R.id.CREF)
        val checkbox = findViewById<CheckBox>(R.id.checkBox)
        val botaoCriar = findViewById<Button>(R.id.Btn_CriarProfessor)

        // Modalidades - múltipla escolha
        val tvModalidades = findViewById<TextView>(R.id.modalidadeSelecionada)
        val modalidades = arrayOf("Futebol", "Basquete", "Vôlei", "Natação")
        val modalidadesSelecionadas = BooleanArray(modalidades.size)
        val modalidadesEscolhidas = mutableListOf<String>()

        val autoCidade = findViewById<AutoCompleteTextView>(R.id.autoCidade)
        val adapter = CidadeAdapter(this, cidades)
        autoCidade.setAdapter(adapter)

        tvModalidades.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Selecione as modalidades")

            builder.setMultiChoiceItems(modalidades, modalidadesSelecionadas) { _, which, isChecked ->
                modalidadesSelecionadas[which] = isChecked
            }

            builder.setPositiveButton("OK") { _, _ ->
                modalidadesEscolhidas.clear()
                for (i in modalidades.indices) {
                    if (modalidadesSelecionadas[i]) {
                        modalidadesEscolhidas.add(modalidades[i])
                    }
                }

                if (modalidadesEscolhidas.isNotEmpty()) {
                    tvModalidades.text = modalidadesEscolhidas.joinToString(", ")
                } else {
                    tvModalidades.text = "Escolha uma modalidade"
                }
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }

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

            // Exemplo: salvar essas informações futuramente no banco
            Toast.makeText(
                this,
                "Professor $nomeTxt cadastrado com sucesso!\nModalidades: ${modalidadesEscolhidas.joinToString()}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    class CidadeAdapter(context: android.content.Context, private val cidades: List<String>) :
        ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, cidades), Filterable {

        private var resultados: List<String> = listOf()

        private fun normalizar(texto: String): String {
            return Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
                .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
        }

        override fun getCount(): Int = resultados.size

        override fun getItem(position: Int): String = resultados[position]

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val filtro = FilterResults()
                    if (constraint != null) {
                        val entrada = normalizar(constraint.toString())
                        resultados = cidades.filter {
                            normalizar(it).contains(entrada)
                        }.take(5)
                        filtro.values = resultados
                        filtro.count = resultados.size
                    }
                    return filtro
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    notifyDataSetChanged()
                }
            }
        }
    }

}
