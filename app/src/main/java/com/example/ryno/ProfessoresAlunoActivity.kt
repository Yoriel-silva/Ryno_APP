package com.example.ryno

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ProfessoresAlunoActivity : AppCompatActivity() {

    private lateinit var spinnerModalidade: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProfessorAdapter
    private val listaCompleta = mutableListOf<Professor>()

    private val modalidades = arrayOf("Todas", "Futebol", "Basquete", "Vôlei", "Natação")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_professores)

        spinnerModalidade = findViewById(R.id.spinnerModalidade)
        recyclerView = findViewById(R.id.recyclerProfessores)

        adapter = ProfessorAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modalidades)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModalidade.adapter = spinnerAdapter

        spinnerModalidade.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filtrarProfessores(modalidades[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        carregarProfessores()
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
                    listaCompleta.add(professor)
                }
                adapter.atualizarLista(listaCompleta)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar professores", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filtrarProfessores(modalidade: String) {
        if (modalidade == "Todas") {
            adapter.atualizarLista(listaCompleta)
        } else {
            val filtrados = listaCompleta.filter { it.modalidades.contains(modalidade) }
            adapter.atualizarLista(filtrados)
        }
    }
}