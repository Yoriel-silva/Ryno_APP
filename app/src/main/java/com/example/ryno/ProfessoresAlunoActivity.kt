package com.example.ryno

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ProfessoresAlunoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProfessorAdapter
    private val listaCompleta = mutableListOf<Professor>()

    companion object {
        const val REQUEST_CODE_FILTRO = 101
        const val TAG = "ProfessoresAluno"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_professores_aluno)

        recyclerView = findViewById(R.id.recyclerProfessores)
        val btnAbrirFiltros = findViewById<Button>(R.id.btnAbrirFiltros)

        adapter = ProfessorAdapter(emptyList()) { professor ->
            salvarProfessorVisualizado(professor)
            val bottomSheet = DetalhesProfessorBottomSheet(professor)
            bottomSheet.show(supportFragmentManager, "DetalhesProfessor")
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        carregarProfessores()

        btnAbrirFiltros.setOnClickListener {
            val intent = Intent(this, ModalidadeAlunoActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_FILTRO)
        }

        val btnVerRecentes = findViewById<Button>(R.id.btnVerRecentes)
        btnVerRecentes.setOnClickListener {
            startActivity(Intent(this, HistoricoActivity::class.java))
        }
    }

    private fun carregarProfessores() {
        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .whereEqualTo("tipo", "professor")
            .get()
            .addOnSuccessListener { result ->
                listaCompleta.clear()
                for (document in result) {
                    val professor = document.toObject(Professor::class.java)
                    Log.d(TAG, "Professor carregado: ${professor.nome}, modalidades: ${professor.modalidades}")
                    listaCompleta.add(professor)
                }
                adapter.atualizarLista(listaCompleta)
            }
            .addOnFailureListener {
                Log.e(TAG, "Erro ao carregar professores", it)
                Toast.makeText(this, "Erro ao carregar professores", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_FILTRO && resultCode == Activity.RESULT_OK) {
            val selecionadas = data?.getStringArrayListExtra("modalidadesSelecionadas") ?: return
            Log.d(TAG, "Modalidades selecionadas no filtro: $selecionadas")
            filtrarProfessoresPorModalidades(selecionadas)
        }
    }

    private fun filtrarProfessoresPorModalidades(selecionadas: List<String>) {
        if (selecionadas.isEmpty()) {
            Log.d(TAG, "Nenhuma modalidade selecionada. Exibindo lista completa.")
            adapter.atualizarLista(listaCompleta)
        } else {
            val filtrados = listaCompleta.filter { professor ->
                Log.d(TAG, "Verificando professor: ${professor.nome}, modalidades: ${professor.modalidades}")
                selecionadas.all { it in professor.modalidades }
            }

            Log.d(TAG, "Professores filtrados: ${filtrados.map { it.nome }}")
            adapter.atualizarLista(filtrados)
        }
    }

    private fun salvarProfessorVisualizado(professor: Professor) {
        val sharedPref = getSharedPreferences("professores_recent", MODE_PRIVATE)
        val listaJson = sharedPref.getString("lista", "[]")

        val gson = com.google.gson.Gson()
        val listaSalva = gson.fromJson(listaJson, Array<Professor>::class.java)?.toMutableList() ?: mutableListOf()

        // Remove se já existir
        listaSalva.removeAll { it.email == professor.email }

        // Adiciona no início da lista
        listaSalva.add(0, professor)

        // Garante que só tenha 5
        val novaLista = listaSalva.take(5)

        // Salva de novo no SharedPreferences
        val jsonAtualizado = gson.toJson(novaLista)
        sharedPref.edit().putString("lista", jsonAtualizado).apply()

        Log.d(TAG, "Professor salvo como visualizado: ${professor.email}")
    }
}
