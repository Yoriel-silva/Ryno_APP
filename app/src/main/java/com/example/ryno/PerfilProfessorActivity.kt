package com.example.ryno

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.Normalizer
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import java.io.File
import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.media.ExifInterface
import android.os.Environment
import java.io.FileOutputStream
import org.xmlpull.v1.XmlPullParser
import android.util.Xml

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

    private val modalidades = arrayOf("Musculação", "Lutas", "Natação", "Yoga")
    private val modalidadesSelecionadas = BooleanArray(modalidades.size)
    private val modalidadesEscolhidas = mutableListOf<String>()

    private lateinit var edtCidade: AutoCompleteTextView

    private lateinit var cidades: List<String>

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var cloudinary: MediaManager

    private val REQUEST_CODE_CAMERA = 101
    private val REQUEST_CODE_GALLERY = 100
    private var imageUri: Uri? = null

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

        val cidades = carregarCidadesDoXml()

        edtCidade = findViewById(R.id.edtCidade)
        val adapter = CidadeAdapter(this, cidades)
        edtCidade.setAdapter(adapter)

        carregarDadosDoProfessor()

        imgPerfil.isEnabled = false

        imgPerfil.setOnClickListener {
            if (imgPerfil.isEnabled) {
                escolherFonteImagem()
            }
        }

        cloudinary = com.cloudinary.android.MediaManager.get()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 200)
        }

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

            if (nome.isEmpty() || email.isEmpty() || telefone.isEmpty() || cref.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!nome.matches(Regex("^[A-Za-zÀ-ÿ\\s]+\$"))) {
                Toast.makeText(this, "O nome não deve conter números ou símbolos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
                Toast.makeText(this, "Insira um e-mail valido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!telefone.matches(Regex("^\\d{11}$"))) {
                Toast.makeText(this, "Telefone deve conter 11 dígitos numéricos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!cref.matches(Regex("^\\d{6,11}$"))) {
                Toast.makeText(this, "CREF deve conter entre 6 e 11 dígitos numéricos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (modalidadesEscolhidas.isEmpty()) {
                Toast.makeText(this, "Selecione ao menos uma modalidade", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cidade.isEmpty()) {
                Toast.makeText(this, "Selecione uma cidade", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!cidades.contains(cidade)) {
                Toast.makeText(this, "Cidade inválida. Escolha uma da lista.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = firebaseAuth.currentUser?.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("usuarios").whereNotEqualTo("uid", userId).get().addOnSuccessListener { documents ->
                var duplicado = false

                for (document in documents) {
                    if (document.getString("cref") == cref) {
                        Toast.makeText(
                            this,
                            "CREF já cadastrado por outro usuário",
                            Toast.LENGTH_SHORT
                        ).show()
                        duplicado = true
                        break
                    }
                    if (document.getString("telefone") == telefone) {
                        Toast.makeText(
                            this,
                            "Telefone já cadastrado por outro usuário",
                            Toast.LENGTH_SHORT
                        ).show()
                        duplicado = true
                        break
                    }
                    if (document.getString("email") == email) {
                        Toast.makeText(
                            this,
                            "E-mail já cadastrado por outro usuário",
                            Toast.LENGTH_SHORT
                        ).show()
                        duplicado = true
                        break
                    }
                }

                if (duplicado) return@addOnSuccessListener

                val userMap = mapOf(
                    "nome" to nome,
                    "email" to email,
                    "telefone" to telefone,
                    "cref" to cref,
                    "cidade" to cidade,
                    "modalidades" to modalidadesEscolhidas
                )

                db.collection("usuarios").document(userId ?: "").update(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show()

                        // Só aqui você troca os campos e botões
                        edtNome.isEnabled = false
                        edtEmail.isEnabled = false
                        edtTelefone.isEnabled = false
                        edtCref.isEnabled = false
                        edtCidade.isEnabled = false
                        edtModalidades.isEnabled = false

                        imgPerfil.isEnabled = false
                        imgEditar.visibility = View.INVISIBLE
                        btnSalvar.visibility = Button.INVISIBLE
                        btnEditar.visibility = Button.VISIBLE
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

        when (requestCode) {
            101 -> { // Permissão para acessar a galeria
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Permissão necessária para acessar a galeria.", Toast.LENGTH_SHORT).show()
                }
            }
            200 -> {
                if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "Permissões necessárias não concedidas", Toast.LENGTH_SHORT).show()
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

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_GALLERY -> {
                    val uri = data?.data
                    uri?.let {
                        exibirImagemEEnviar(it)
                    }
                }
                REQUEST_CODE_CAMERA -> {
                    imageUri?.let {
                        exibirImagemEEnviar(it)
                    }
                }
            }
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
                        val modalidadesArray = arrayOf("Musculação", "Lutas", "Natação", "Yoga")

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

    private fun escolherFonteImagem() {
        val opcoes = arrayOf("Câmera", "Galeria")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Escolher imagem de perfil")
        builder.setItems(opcoes) { _, which ->
            when (which) {
                0 -> abrirCamera()
                1 -> openGallery()
            }
        }
        builder.show()
    }

    private fun abrirCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            val photoFile = criarArquivoImagem()
            imageUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA)
        } else {
            Toast.makeText(this, "Câmera não disponível", Toast.LENGTH_SHORT).show()
        }
    }

    private fun criarArquivoImagem(): File {
        val nomeArquivo = "perfil_${System.currentTimeMillis()}.jpg"
        val diretorio = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "perfil_professor")
        if (!diretorio.exists()) diretorio.mkdirs()
        return File(diretorio, nomeArquivo)
    }

    private fun exibirImagemEEnviar(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val rotatedBitmap = corrigirRotacaoImagem(uri, bitmap)
        val circularBitmap = getCircularBitmap(rotatedBitmap)
        imgPerfil.setImageBitmap(circularBitmap)

        // Converte o bitmap circular em um arquivo temporário
        val file = bitmapToFile(circularBitmap, this)

        MediaManager.get().upload(file.path)
            .option("folder", "perfil_professor")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Toast.makeText(this@PerfilProfessorActivity, "Fazendo upload da imagem...", Toast.LENGTH_SHORT).show()
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as? String
                    if (!imageUrl.isNullOrEmpty()) {
                        saveProfileImageUrlToDatabase(imageUrl)
                        Picasso.get().load(imageUrl).transform(CropCircleTransformation()).into(imgPerfil)
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Toast.makeText(this@PerfilProfessorActivity, "Erro: ${error.description}", Toast.LENGTH_SHORT).show()
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
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

    private fun corrigirRotacaoImagem(uri: Uri, bitmap: Bitmap): Bitmap {
        val input = contentResolver.openInputStream(uri)
        val exif = input?.let { ExifInterface(it) }
        val orientacao = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL

        val angulo = when (orientacao) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        val matrix = Matrix()
        matrix.postRotate(angulo)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        val squared = Bitmap.createBitmap(bitmap, x, y, size, size)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        squared.recycle()
        return output
    }
    fun bitmapToFile(bitmap: Bitmap, context: Context): File {
        val file = File(context.cacheDir, "imagem_temp.png")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return file
    }

    private fun carregarCidadesDoXml(): List<String> {
        val cidades = mutableListOf<String>()
        val parser = Xml.newPullParser()
        val inputStream = resources.openRawResource(R.raw.cidades)
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var dentroDeCidade = false
        var nomeCidade = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName.equals("CIDADE", ignoreCase = true)) {
                        dentroDeCidade = true
                    } else if (dentroDeCidade && tagName.equals("NOME", ignoreCase = true)) {
                        nomeCidade = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName.equals("CIDADE", ignoreCase = true)) {
                        if (nomeCidade.isNotBlank()) cidades.add(nomeCidade)
                        nomeCidade = ""
                        dentroDeCidade = false
                    }
                }
            }
            eventType = parser.next()
        }

        return cidades
    }
}