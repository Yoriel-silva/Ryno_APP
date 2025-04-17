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

    private var ultimasSelecionadas: List<String> = emptyList()

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
        val btnPerfil = findViewById<Button>(R.id.btnPerfil)

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
            finish()
        }

        btnPerfil.setOnClickListener {
            val intent = Intent(this, PerfilAlunoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == REQUEST_CODE_FILTRO && resultCode == Activity.RESULT_OK) {
//            val selecionadas = data?.getStringArrayListExtra("modalidadesSelecionadas") ?: return
//            Log.d("filtro", "Modalidades selecionadas no filtro: $selecionadas")
//            ultimasSelecionadas = selecionadas
//            filtrarProfessoresPorModalidades(selecionadas)
//        }
//        Log.d("filtro", "Chamou mais não validou.")
//    }

    override fun onResume() {
        super.onResume()
        carregarLocalizacaoAluno {
            carregarProfessores()
        }
    }

    private fun filtrarProfessoresPorModalidades() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
        val sharedPref = getSharedPreferences("filtro_prefs_$userId", MODE_PRIVATE)
        val selecionadas = sharedPref.getStringSet("modalidadesSelecionadas", emptySet())?.toList() ?: emptyList()
        if (selecionadas.isEmpty()) {
            Log.d("filtro", "Nenhuma modalidade selecionada. Exibindo lista completa.")
            adapter.atualizarLista(listaCompleta)
        } else {
            Log.d("filtro", "Selecionadas: $selecionadas")
            listaCompleta.forEach {
                Log.d("filtro", "${it.nome} => ${it.modalidades}")
            }
            val filtrados = listaCompleta.filter { professor ->
                Log.d("filtro", "Professor: ${professor.nome}")
                selecionadas.forEach { selecionada ->
                    val passou = professor.modalidades.any { it.trim() == selecionada.trim() }
                    Log.d("filtro", " - Modalidade '$selecionada' encontrada em ${professor.modalidades}? $passou")
                }

                val resultado = selecionadas.all { it.trim() in professor.modalidades.map { m -> m.trim() } }
                Log.d("filtro", "Professor ${professor.nome} passou? $resultado")
                resultado
            }

            Log.d("filtro", "Professores filtrados: ${filtrados.map { it.nome }}")
            adapter.atualizarLista(filtrados)
        }
    }

    private fun salvarProfessorVisualizado(professor: Professor) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val sharedPref = getSharedPreferences("professores_recent_$userId", MODE_PRIVATE)
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
        Log.e("Localizacao", "Função chamada", )
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            Log.e("Localizacao", "userId: ${userId}", )
            FirebaseFirestore.getInstance().collection("usuarios")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    Log.e("Localizacao", "Entrou" )
                    val localizacao = document.get("localizacao") as? Map<*, *>
                    if (localizacao != null) {
                        latitudeAluno = (localizacao["latitude"] as? Number)?.toDouble()
                        longitudeAluno = (localizacao["longitude"] as? Number)?.toDouble()

                        // Se a localização foi carregada com sucesso, chama onComplete
                        Log.e("Localizacao", "latitude carregada: ${latitudeAluno}", )
                        Log.e("Localizacao", "longitude carregada: ${longitudeAluno}")
                        Log.e("Localizacao", "localizacao carregada", )
                        onComplete()
                    } else {
                        Toast.makeText(this, "Localização não encontrada", Toast.LENGTH_SHORT).show()
                        Log.e("Localizacao", "localizacao não encontrada", )
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao carregar localização do aluno", Toast.LENGTH_SHORT).show()
                    Log.e("Localizacao", "erro ao carregar localizacao", )
                }
        } else {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            Log.e("Localizacao", "Usuário não logado", )
        }
    }

    private fun carregarProfessores() {
        // Verifica se a localização do aluno foi carregada antes de buscar os professores
        Log.e("Localizacao", "latitude carregada2: ${latitudeAluno}", )
        Log.e("Localizacao", "longitude carregada2: ${longitudeAluno}")
        if (latitudeAluno != null && longitudeAluno != null) {
            Log.e("Localizacao", "Entrou 2")
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
                    filtrarProfessoresPorModalidades()

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

                    //adapter.atualizarLista(listaCompleta)
                }
                .addOnFailureListener {
                    Log.e(TAG, "Erro ao carregar professores", it)
                    Toast.makeText(this, "Erro ao carregar professores", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Caso a localização ainda não tenha sido carregada
            Toast.makeText(this, "Carregando localização do aluno...", Toast.LENGTH_SHORT).show()
            Log.e("Localizacao", "latitude: ${latitudeAluno}", )
            Log.e("Localizacao", "longitude: ${longitudeAluno}", )

        }
    }
}
