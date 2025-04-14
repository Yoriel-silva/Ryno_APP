package com.example.ryno

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.Normalizer

class PerfilProfessorActivity : AppCompatActivity() {

    private lateinit var imgPerfil: ImageView
    private lateinit var btnSalvar: Button
    private lateinit var btnDeslogar: Button

    private lateinit var edtNome: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtTelefone: EditText
    private lateinit var edtCref: EditText

    private lateinit var edtCidade: AutoCompleteTextView

    private val cidades = listOf(
        "São Paulo", "Salvador", "Fortaleza", "Belo Horizonte",
        "Rio de Janeiro", "Curitiba", "Brasília", "Manaus", "Recife", "Porto Alegre"
    )

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_professor)

        firebaseAuth = FirebaseAuth.getInstance()
        storageReference = FirebaseStorage.getInstance().reference

        imgPerfil = findViewById(R.id.imgPerfil)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnDeslogar = findViewById(R.id.btnDeslogar)

        edtNome = findViewById(R.id.edtNome)
        edtEmail = findViewById(R.id.edtEmail)
        edtTelefone = findViewById(R.id.edtTelefone)
        edtCref = findViewById(R.id.edtCref)

        edtCidade = findViewById(R.id.edtCidade)
        val adapter = CidadeAdapter(this, cidades)
        edtCidade.setAdapter(adapter)

        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            val firestoreRef = FirebaseFirestore.getInstance().collection("usuarios").document(userId)

            firestoreRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val nome = documentSnapshot.getString("nome")
                    val email = documentSnapshot.getString("email")
                    val telefone = documentSnapshot.getString("telefone")
                    val cref = documentSnapshot.getString("cref")

                    val cidade = documentSnapshot.getString("cidade")
                    val profileImageUrl = documentSnapshot.getString("profileImageUrl")

                    edtNome.setText(nome ?: "")
                    edtEmail.setText(email ?: "")
                    edtTelefone.setText(telefone ?: "")
                    edtCref.setText(cref ?: "")
                    edtCidade.setText(cidade ?: "")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Picasso.get().load(profileImageUrl).into(imgPerfil)
                    }
                } else {
                    Toast.makeText(this, "Usuário não encontrado", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar dados do perfil", Toast.LENGTH_SHORT).show()
            }
        }

        imgPerfil.setOnClickListener {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 101)
            }
        }

        btnSalvar.setOnClickListener {
            val nome = edtNome.text.toString()
            val email = edtEmail.text.toString()
            val telefone = edtTelefone.text.toString()
            val cref = edtCref.text.toString()
            val cidade = edtCidade.text.toString()

            val userMap = mapOf(
                "nome" to nome,
                "email" to email,
                "telefone" to telefone,
                "cref" to cref,
                "cidade" to cidade
            )

            val userId = firebaseAuth.currentUser?.uid
            val firestoreRef = FirebaseFirestore.getInstance().collection("usuarios").document(userId ?: "")

            firestoreRef.update(userMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao salvar dados", Toast.LENGTH_SHORT).show()
                }
        }

        btnDeslogar.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            Toast.makeText(this, "Permissão necessária para acessar a galeria.", Toast.LENGTH_SHORT).show()
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
            Picasso.get().load(imageUri).into(imgPerfil)

            imageUri?.let { uri ->
                val userId = firebaseAuth.currentUser?.uid ?: return@let
                val imageRef = storageReference.child("profile_pictures/$userId.jpg")

                imageRef.putFile(uri)
                    .addOnSuccessListener {
                        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
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

    private fun saveProfileImageUrlToDatabase(imageUrl: String) {
        val userId = firebaseAuth.currentUser?.uid
        val firestoreRef = FirebaseFirestore.getInstance().collection("usuarios").document(userId ?: "")

        val userMap = mapOf("profileImageUrl" to imageUrl)

        firestoreRef.update(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Imagem de perfil atualizada!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao salvar imagem de perfil", Toast.LENGTH_SHORT).show()
            }
    }
}
