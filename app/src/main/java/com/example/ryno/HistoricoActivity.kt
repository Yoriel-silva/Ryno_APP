package com.example.ryno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson

class HistoricoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProfessorAdapter

    private lateinit var btnVoltar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico)

        btnVoltar = findViewById(R.id.btnVoltar)

        recyclerView = findViewById(R.id.recyclerProfessoresRecentes)

        adapter = ProfessorAdapter(emptyList()) { professor ->
            val bottomSheet = DetalhesProfessorBottomSheet(professor)
            bottomSheet.show(supportFragmentManager, "DetalhesProfessor")
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val professoresRecentes = obterProfessoresRecentes()
        adapter.atualizarLista(professoresRecentes)

        btnVoltar.setOnClickListener {
            val intent = Intent(this, PerfilAlunoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun obterProfessoresRecentes(): List<Professor> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "default"
        val sharedPref = getSharedPreferences("professores_recent_$userId", MODE_PRIVATE)
        val listaJson = sharedPref.getString("lista", "[]")
        return Gson().fromJson(listaJson, Array<Professor>::class.java)?.toList() ?: emptyList()
    }
}