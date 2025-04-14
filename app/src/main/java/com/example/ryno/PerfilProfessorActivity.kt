package com.example.ryno

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.Normalizer
import jp.wasabeef.picasso.transformations.CropCircleTransformation

class PerfilProfessorActivity : AppCompatActivity() {

    private lateinit var imgPerfil: ImageView
    private lateinit var imgEditar : ImageView
    private lateinit var btnSalvar: Button
    private lateinit var btnEditar: Button
    private lateinit var btnDeslogar: Button

    private lateinit var edtNome: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtTelefone: EditText
    private lateinit var edtCref: EditText

    private lateinit var edtModalidades: TextView

    private val modalidades = arrayOf("Futebol", "Basquete", "Vôlei", "Natação")
    private val modalidadesSelecionadas = BooleanArray(modalidades.size)
    private val modalidadesEscolhidas = mutableListOf<String>()

    private lateinit var edtCidade: AutoCompleteTextView

    private val cidades = listOf(
        "São Paulo", "Salvador", "Fortaleza", "Belo Horizonte",
        "Rio de Janeiro", "Curitiba", "Brasília", "Manaus", "Recife", "Porto Alegre"
    )

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var cloudinary: MediaManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_professor)

        firebaseAuth = FirebaseAuth.getInstance()

        imgPerfil = findViewById(R.id.imgPerfil)
        imgEditar = findViewById(R.id.imgEditar)

        btnSalvar = findViewById(R.id.btnSalvar)
        btnEditar = findViewById(R.id.btnEditar)
        btnDeslogar = findViewById(R.id.btnDeslogar)

        edtNome = findViewById(R.id.edtNome)
        edtEmail = findViewById(R.id.edtEmail)
        edtTelefone = findViewById(R.id.edtTelefone)
        edtCref = findViewById(R.id.edtCref)

        edtModalidades = findViewById(R.id.edtModalidade)

        edtModalidades.setOnClickListener {
            abrirDialogModalidades()
        }

        edtCidade = findViewById(R.id.edtCidade)
        val adapter = CidadeAdapter(this, cidades)
        edtCidade.setAdapter(adapter)

        carregarDadosDoProfessor()

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
            edtCref.isEnabled = true
            edtCidade.isEnabled = true
            edtModalidades.isEnabled = true

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
            val cref = edtCref.text.toString()
            val cidade = edtCidade.text.toString()

            // Coleta as modalidades selecionadas
            val modalidadesEscolhidas = mutableListOf<String>()
            for (i in modalidades.indices) {
                if (modalidadesSelecionadas[i]) {
                    modalidadesEscolhidas.add(modalidades[i])
                }
            }

            // Adicionando log para depurar as modalidades
            Log.d("Modalidades", "Modalidades Selecionadas: ${modalidadesSelecionadas.joinToString(", ")}")
            Log.d("Modalidades", "Modalidades Escolhidas: $modalidadesEscolhidas")

            // Cria o mapa com os dados para salvar no Firestore
            val userMap = mapOf(
                "nome" to nome,
                "email" to email,
                "telefone" to telefone,
                "cref" to cref,
                "cidade" to cidade,
                "modalidades" to modalidadesEscolhidas  // Salva as modalidades escolhidas
            )

            // Verifica se as modalidades estão sendo salvas corretamente
            if (modalidadesEscolhidas.isEmpty()) {
                Toast.makeText(this, "Por favor, selecione ao menos uma modalidade", Toast.LENGTH_SHORT).show()
            } else {
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
            }

            // Depois de salvar, voltar para o modo não editável
            edtNome.isEnabled = false
            edtEmail.isEnabled = false
            edtTelefone.isEnabled = false
            edtCref.isEnabled = false
            edtCidade.isEnabled = false
            edtModalidades.isEnabled = false

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
    }

    private fun abrirDialogModalidades() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Editar modalidades")

        // Passando o estado de modalidadesSelecionadas corretamente para o AlertDialog
        builder.setMultiChoiceItems(modalidades, modalidadesSelecionadas) { _, which, isChecked ->
            // Atualizando o estado da modalidade selecionada
            modalidadesSelecionadas[which] = isChecked
        }

        builder.setPositiveButton("OK") { _, _ ->
            // Após o clique em OK, atualiza as modalidades escolhidas
            modalidadesEscolhidas.clear()
            for (i in modalidades.indices) {
                if (modalidadesSelecionadas[i]) {
                    modalidadesEscolhidas.add(modalidades[i])
                }
            }

            // Log para verificar as modalidades escolhidas
            Log.d("Modalidades", "Modalidades Escolhidas após OK: ${modalidadesEscolhidas.joinToString(", ")}")

            // Atualiza o TextView com as modalidades escolhidas
            edtModalidades.text = if (modalidadesEscolhidas.isNotEmpty()) {
                modalidadesEscolhidas.joinToString(", ")
            } else {
                "Escolha uma modalidade"
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
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
            imageUri?.let { uri ->
                // Exibir a imagem localmente enquanto o upload é feito
                Picasso.get().load(uri).transform(CropCircleTransformation()).into(imgPerfil)

                // Upload da imagem para o Cloudinary
                MediaManager.get().upload(uri)
                    .option("folder", "perfil_professor") // Especificando a pasta no Cloudinary
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            // Mostrar um Toast ou algum tipo de feedback visual
                            Toast.makeText(this@PerfilProfessorActivity, "Fazendo upload da imagem...", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this@PerfilProfessorActivity, "Erro ao enviar imagem: ${error.description}", Toast.LENGTH_SHORT).show()
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

    private fun carregarDadosDoProfessor() {
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

                    // Tentando pegar as modalidades e adicionar um log para depuração
                    val modalidades = documentSnapshot.get("modalidades") as? List<*>
                    Log.d("Modalidades", "Modalidades do usuário: $modalidades")


                    edtNome.setText(nome ?: "")
                    edtEmail.setText(email ?: "")
                    edtTelefone.setText(telefone ?: "")
                    edtCref.setText(cref ?: "")
                    edtCidade.setText(cidade ?: "")

                    val profileImageUrl = documentSnapshot.getString("profileImageUrl")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Picasso.get().load(profileImageUrl).transform(CropCircleTransformation()).into(imgPerfil)
                    }

                    // Aqui, você precisa garantir que as modalidades sejam corretamente atribuídas
                    if (!modalidades.isNullOrEmpty()) {
                        // Converte a lista de modalidades para string para exibição
                        val modalidadesStr = modalidades.filterIsInstance<String>().joinToString(", ")
                        edtModalidades.setText(modalidadesStr)

                        // Preenche a variável global `modalidadesSelecionadas` corretamente
                        val modalidadesArray = arrayOf("Futebol", "Basquete", "Vôlei", "Natação")

                        // Atualizando diretamente a variável global `modalidadesSelecionadas`
                        for (i in modalidadesArray.indices) {
                            if (modalidades.contains(modalidadesArray[i])) {
                                modalidadesSelecionadas[i] = true
                            } else {
                                modalidadesSelecionadas[i] = false
                            }
                        }

                        // Ao clicar no TextView, abre o AlertDialog
                        edtModalidades.setOnClickListener {
                            abrirDialogModalidades()
                        }
                    }
                } else {
                    Toast.makeText(this, "Usuário não encontrado", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar dados do perfil", Toast.LENGTH_SHORT).show()
            }
        }
    }

}