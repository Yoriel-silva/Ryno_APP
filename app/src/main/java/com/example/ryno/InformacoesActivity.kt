package com.example.ryno

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.Normalizer
import jp.wasabeef.picasso.transformations.CropCircleTransformation

class InformacoesActivity : AppCompatActivity() {

    private lateinit var imgPerfil: ImageView
    private lateinit var imgEditar : ImageView
    private lateinit var btnSalvar: Button
    private lateinit var btnEditar: Button
    private lateinit var btnDeslogar: Button

    private lateinit var btnVoltar: Button

    private lateinit var edtNome: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtTelefone: EditText

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var cloudinary: MediaManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_informacoes)

        firebaseAuth = FirebaseAuth.getInstance()

        imgPerfil = findViewById(R.id.imgPerfil)
        imgEditar = findViewById(R.id.imgEditar)

        btnSalvar = findViewById(R.id.btnSalvar)
        btnEditar = findViewById(R.id.btnEditar)
        btnDeslogar = findViewById(R.id.btnDeslogar)
        btnVoltar = findViewById(R.id.btnVoltar)

        edtNome = findViewById(R.id.edtNome)
        edtEmail = findViewById(R.id.edtEmail)
        edtTelefone = findViewById(R.id.edtTelefone)

        carregarDadosDoAluno()

        imgPerfil.isEnabled = false
        imgPerfil.setOnClickListener {
            if (imgPerfil.isEnabled) {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, 100)
            }
        }

        cloudinary = com.cloudinary.android.MediaManager.get()

        btnEditar.setOnClickListener {
            // Tornar campos editáveis
            edtNome.isEnabled = true
            edtEmail.isEnabled = true
            edtTelefone.isEnabled = true

            imgPerfil.isEnabled = true

            imgEditar.visibility = View.VISIBLE

            // Troca os botões
            btnEditar.visibility = Button.INVISIBLE
            btnSalvar.visibility = Button.VISIBLE
        }

        btnSalvar.setOnClickListener {
            // Coleta os dados dos campos EditText
            val nome = edtNome.text.toString()
            val email = edtEmail.text.toString()
            val telefone = edtTelefone.text.toString()

            // Cria o mapa com os dados para salvar no Firestore
            val userMap = mapOf(
                "nome" to nome,
                "email" to email,
                "telefone" to telefone,
            )

                // Pega o ID do usuário atual
            val userId = firebaseAuth.currentUser?.uid
            val firestoreRef = FirebaseFirestore.getInstance().collection("usuarios").document(userId ?: "")

                // Atualiza os dados do usuário no Firestore
            firestoreRef.update(userMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao salvar dados", Toast.LENGTH_SHORT).show()
                }

            // Depois de salvar, voltar para o modo não editável
            edtNome.isEnabled = false
            edtEmail.isEnabled = false
            edtTelefone.isEnabled = false

            imgPerfil.isEnabled = false

            imgEditar.visibility = View.INVISIBLE

            // Troca os botões novamente
            btnSalvar.visibility = Button.INVISIBLE
            btnEditar.visibility = Button.VISIBLE
        }

        btnDeslogar.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnVoltar.setOnClickListener {
            val intent = Intent(this, PerfilAlunoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            101 -> { // Permissão para acessar a galeria
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Permissão necessária para acessar a galeria.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            imageUri?.let { uri ->
                // Exibir a imagem localmente enquanto o upload é feito
                Picasso.get().load(uri).transform(CropCircleTransformation()).into(imgPerfil)

                // Upload da imagem para o Cloudinary
                MediaManager.get().upload(uri)
                    .option("folder", "perfil_professor") // Especificando a pasta no Cloudinary
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            // Mostrar um Toast ou algum tipo de feedback visual
                            Toast.makeText(this@InformacoesActivity, "Fazendo upload da imagem...", Toast.LENGTH_SHORT).show()
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            // Aqui você pode mostrar o progresso se quiser
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            // Recupera o URL da imagem após o upload
                            val imageUrl = resultData["secure_url"] as? String
                            if (!imageUrl.isNullOrEmpty()) {
                                // Salvar a URL da imagem no Firestore
                                saveProfileImageUrlToDatabase(imageUrl)
                                // Exibir a imagem no ImageView
                                Picasso.get().load(imageUrl).transform(CropCircleTransformation()).into(imgPerfil)
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            // Caso ocorra algum erro no upload
                            Toast.makeText(this@InformacoesActivity, "Erro ao enviar imagem: ${error.description}", Toast.LENGTH_SHORT).show()
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            // Caso seja necessário reagendar o upload (isso pode ser útil em casos de falhas temporárias)
                        }
                    })
                    .dispatch()
            }
        }
    }

    private fun saveProfileImageUrlToDatabase(imageUrl: String) {
        // Pega o ID do usuário logado
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val firestoreRef = FirebaseFirestore.getInstance().collection("usuarios").document(userId)

            // Atualiza o Firestore com a URL da imagem
            val userMap = mapOf("profileImageUrl" to imageUrl)

            firestoreRef.update(userMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Imagem de perfil atualizada!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao salvar imagem de perfil", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun carregarDadosDoAluno() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            val firestoreRef = FirebaseFirestore.getInstance().collection("usuarios").document(userId)

            firestoreRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val nome = documentSnapshot.getString("nome")
                    val email = documentSnapshot.getString("email")
                    val telefone = documentSnapshot.getString("telefone")

                    edtNome.setText(nome ?: "")
                    edtEmail.setText(email ?: "")
                    edtTelefone.setText(telefone ?: "")

                    val profileImageUrl = documentSnapshot.getString("profileImageUrl")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Picasso.get().load(profileImageUrl).transform(CropCircleTransformation()).into(imgPerfil)
                    }



                }
                else {
                    Toast.makeText(this, "Usuário não encontrado", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar dados do perfil", Toast.LENGTH_SHORT).show()
            }
        }
    }
}