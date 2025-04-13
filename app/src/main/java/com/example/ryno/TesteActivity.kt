package com.example.ryno

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class TesteActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teste)

        val testButton: Button = findViewById(R.id.testButton)

        // Inicializando o Firestore
        firestore = FirebaseFirestore.getInstance()

        testButton.setOnClickListener {
            // Testar conexão escrevendo um dado
            val testData = hashMapOf(
                "mensagem" to "Olá do Android!",
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("testes")
                .add(testData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Conexão com Firebase OK!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}