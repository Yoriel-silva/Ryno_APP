package com.example.ryno

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson

class HistoricoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProfessorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico)

        recyclerView = findViewById(R.id.recyclerProfessoresRecentes)

        adapter = ProfessorAdapter(emptyList()) { professor ->
            val bottomSheet = DetalhesProfessorBottomSheet(professor)
            bottomSheet.show(supportFragmentManager, "DetalhesProfessor")
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val professoresRecentes = obterProfessoresRecentes()
        adapter.atualizarLista(professoresRecentes)
    }

    private fun obterProfessoresRecentes(): List<Professor> {
        val sharedPref = getSharedPreferences("professores_recent", MODE_PRIVATE)
        val listaJson = sharedPref.getString("lista", "[]")
        return Gson().fromJson(listaJson, Array<Professor>::class.java)?.toList() ?: emptyList()
    }
}