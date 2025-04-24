package com.example.ryno

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import java.io.FileOutputStream

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

    private val REQUEST_CODE_CAMERA = 101
    private val REQUEST_CODE_GALLERY = 100
    private var imageUri: Uri? = null

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
                escolherFonteImagem()
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
                    Toast.makeText(this@InformacoesActivity, "Fazendo upload da imagem...", Toast.LENGTH_SHORT).show()
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as? String
                    if (!imageUrl.isNullOrEmpty()) {
                        saveProfileImageUrlToDatabase(imageUrl)
                        Picasso.get().load(imageUrl).transform(CropCircleTransformation()).into(imgPerfil)
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Toast.makeText(this@InformacoesActivity, "Erro: ${error.description}", Toast.LENGTH_SHORT).show()
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