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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.pow

class ProfessoresAlunoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProfessorAdapter
    private val listaCompleta = mutableListOf<Professor>()

    private var latitudeAluno: Double? = null
    private var longitudeAluno: Double? = null

    private lateinit var firebaseAuth: FirebaseAuth

    companion object {
        const val REQUEST_CODE_FILTRO = 101
        const val TAG = "ProfessoresAluno"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_professores_aluno)

        firebaseAuth = FirebaseAuth.getInstance()

        recyclerView = findViewById(R.id.recyclerProfessores)
        val btnAbrirFiltros = findViewById<Button>(R.id.btnAbrirFiltros)

        adapter = ProfessorAdapter(
            lista = emptyList(),
            onClick = { professor: Professor ->
                salvarProfessorVisualizado(professor)
                val bottomSheet = DetalhesProfessorBottomSheet(professor)
                bottomSheet.show(supportFragmentManager, "DetalhesProfessor")
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        carregarLocalizacaoAluno {
            carregarProfessores()
        }

        btnAbrirFiltros.setOnClickListener {
            val intent = Intent(this, ModalidadeAlunoActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_FILTRO)
        }

        val btnVerRecentes = findViewById<Button>(R.id.btnVerRecentes)
        btnVerRecentes.setOnClickListener {
            startActivity(Intent(this, HistoricoActivity::class.java))
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

    fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Raio da Terra em quilômetros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2).pow(2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c // Retorna a distância em quilômetros
    }

    private fun carregarLocalizacaoAluno(onComplete: () -> Unit) {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            FirebaseFirestore.getInstance().collection("usuarios")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val localizacao = document.get("localizacao") as? Map<*, *>
                    if (localizacao != null) {
                        latitudeAluno = (localizacao["latitude"] as? Number)?.toDouble()
                        longitudeAluno = (localizacao["longitude"] as? Number)?.toDouble()

                        // Se a localização foi carregada com sucesso, chama onComplete
                        onComplete()
                    } else {
                        Toast.makeText(this, "Localização não encontrada", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao carregar localização do aluno", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun carregarProfessores() {
        // Verifica se a localização do aluno foi carregada antes de buscar os professores
        if (latitudeAluno != null && longitudeAluno != null) {
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

                    listaCompleta.forEach { professor ->
                        val latProf = professor.localizacao["latitude"]
                        val lonProf = professor.localizacao["longitude"]
                        if (latProf != null && lonProf != null && latitudeAluno != null && longitudeAluno != null) {
                            val distancia = calcularDistancia(latitudeAluno!!, longitudeAluno!!, latProf, lonProf)
                            professor.distanciaKm = distancia
                        } else {
                            professor.distanciaKm = Double.MAX_VALUE
                        }
                    }

                    // Agora ordena
                    listaCompleta.sortBy { it.distanciaKm }

//                    Ordenar por distância
//                    listaCompleta.sortBy { professor ->
//                        val latProf = professor.localizacao["latitude"]
//                        val lonProf = professor.localizacao["longitude"]
//                        if (latProf != null && lonProf != null && latitudeAluno != null && longitudeAluno != null) {
//                            calcularDistancia(latitudeAluno!!, longitudeAluno!!, latProf, lonProf)
//                        } else {
//                            Double.MAX_VALUE // Sem localização => manda pro final da lista
//                        }
//                    }

                    adapter.atualizarLista(listaCompleta)
                }
                .addOnFailureListener {
                    Log.e(TAG, "Erro ao carregar professores", it)
                    Toast.makeText(this, "Erro ao carregar professores", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Caso a localização ainda não tenha sido carregada
            Toast.makeText(this, "Carregando localização do aluno...", Toast.LENGTH_SHORT).show()
        }
    }
}
