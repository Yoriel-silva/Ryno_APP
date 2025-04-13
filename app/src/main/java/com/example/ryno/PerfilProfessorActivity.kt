package com.example.ryno

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

class PerfilProfessorActivity : AppCompatActivity() {

    private lateinit var imgPerfil: ImageView
    private lateinit var btnSalvar: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_professor)  // Seu layout XML

        // Inicialize o Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        storageReference = FirebaseStorage.getInstance().reference

        imgPerfil = findViewById(R.id.imgPerfil)
        btnSalvar = findViewById(R.id.btnSalvar)

        // Definir um ouvinte para o clique da imagem de perfil
        imgPerfil.setOnClickListener {
            // Verifica a permissão de leitura do armazenamento em tempo de execução
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 101)
            }
        }

        // Salvar alterações (não relacionado à imagem, mas já configurado no seu layout)
        btnSalvar.setOnClickListener {
            // Aqui você pode salvar as outras informações do perfil, além da imagem
            Toast.makeText(this, "Alterações salvas!", Toast.LENGTH_SHORT).show()
        }
    }

    // Quando a permissão de armazenamento for concedida
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            Toast.makeText(this, "Permissão necessária para acessar a galeria.", Toast.LENGTH_SHORT).show()
        }
    }

    // Metodo para abrir a galeria de imagens
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 100) // Código 100 para identificar a requisição da galeria
    }

    // Metodo para tratar o resultado da seleção da imagem
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data

            // Exibir a imagem na ImageView com Picasso
            Picasso.get().load(imageUri).into(imgPerfil)

            // Enviar a imagem para o Firebase Storage
            imageUri?.let { uri ->
                val userId = firebaseAuth.currentUser?.uid ?: return@let
                val imageRef = storageReference.child("profile_pictures/$userId.jpg")

                imageRef.putFile(uri)
                    .addOnSuccessListener {
                        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            // Salvar o URL da imagem no Firebase Database ou onde precisar
                            val imageUrl = downloadUri.toString()
                            saveProfileImageUrlToDatabase(imageUrl)
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao fazer upload da imagem", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // Salvar o URL da imagem no Firebase Database
    private fun saveProfileImageUrlToDatabase(imageUrl: String) {
        val userId = firebaseAuth.currentUser?.uid
        val databaseRef = FirebaseDatabase.getInstance().reference.child("users").child(userId ?: "")
        val userMap = mapOf("profileImageUrl" to imageUrl)

        databaseRef.updateChildren(userMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Imagem de perfil atualizada!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Erro ao salvar imagem de perfil", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

