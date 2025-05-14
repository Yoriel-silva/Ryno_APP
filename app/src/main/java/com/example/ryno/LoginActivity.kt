package com.example.ryno

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_CODE_LOCATION = 1001 // Defina um código único para a permissão de localização

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializa o FirebaseAuth e o Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Verifica se o usuário já está logado no Firebase
        val user = auth.currentUser
        if (user != null) {
            // O usuário está logado
            //pegar localização
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            // Verificar permissão de localização
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
            } else {
                obterLocalizacao() // Caso já tenha permissão, obter a localização imediatamente
            }

            //redireciona para a página correspondente
            val userType = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("userType", null)
            if (userType != null) {
                when (userType) {
                    "aluno" -> startActivity(Intent(this, ProfessoresAlunoActivity::class.java))
                    "professor" -> startActivity(Intent(this, PerfilProfessorActivity::class.java))
                }
                finish()
            } else {
                // Caso o tipo de usuário não tenha sido salvo, faça logout e peça o login novamente
                //auth.signOut()
                Toast.makeText(this, "Erro ao carregar o tipo de usuário. Faça login novamente.", Toast.LENGTH_SHORT).show()
                //finish()
            }
        }

        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        val tvCriarConta = findViewById<TextView>(R.id.tvCriarConta)

        val texto = "Não tem uma conta? Criar Conta"
        val spannableString = SpannableString(texto)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@LoginActivity, TipoCadastroActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = Color.parseColor("#6200EE")
            }
        }

        val startIndex = texto.indexOf("Criar Conta")
        val endIndex = startIndex + "Criar Conta".length
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        tvCriarConta.text = spannableString
        tvCriarConta.movementMethod = LinkMovementMethod.getInstance()

        buttonLogin.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        val RedefinirSenhaTextView: TextView = findViewById(R.id.RedefinirSenha)

        RedefinirSenhaTextView.setOnClickListener {
            val intent = Intent(this, RedefinirSenhaActivity::class.java)
            startActivity(intent)
                finish()
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

                    // Definindo a preferência de login
                    sharedPrefs.edit().putBoolean("isLoggedIn", true).apply()

                    // Agora vamos pegar o tipo de usuário do Firestore
                    val userId = user?.uid
                    if (userId != null) {
                        // Consulta ao Firestore para pegar o tipo de usuário (campo 'tipo')
                        firestore.collection("usuarios").document(userId)
                            .get().addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val userType = document.getString("tipo") // Pega o campo 'tipo'

                                    // Salva o tipo de usuário no SharedPreferences
                                    sharedPrefs.edit().putString("userType", userType).apply()

                                    // Redireciona para a página correspondente
                                    when (userType) {
                                        "aluno" -> startActivity(Intent(this, ProfessoresAlunoActivity::class.java))
                                        "professor" -> startActivity(Intent(this, PerfilProfessorActivity::class.java))
                                    }
                                    finish()
                                } else {
                                    Toast.makeText(this, "Usuário não encontrado no Firestore.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Erro ao obter o tipo de usuário: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                else {
                    Toast.makeText(baseContext, "Falha na autenticação. Tente novamente.", Toast.LENGTH_SHORT).show()
                    task.exception?.let { exception ->
                        Log.e("Login", "Erro de autenticação: ${exception.message}")}
                }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1001 -> { // Permissão para acessar a localização
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    obterLocalizacao()
                } else {
                    Toast.makeText(this, "Permissão de localização necessária", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun obterLocalizacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permissão não foi concedida, então não tenta acessar a localização
            Toast.makeText(this, "Permissão de localização não concedida.", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                salvarLocalizacaoNoFirestore(location.latitude, location.longitude)
            } else {
                Toast.makeText(this, "Não foi possível obter a localização", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun salvarLocalizacaoNoFirestore(latitude: Double, longitude: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseFirestore.getInstance()

        val localizacao = hashMapOf(
            "latitude" to latitude,
            "longitude" to longitude
        ) as Map<String, Any>

        if (userId != null) {
            db.collection("usuarios").document(userId)
                .update("localizacao", localizacao)
                .addOnSuccessListener {
                    Log.d("localização", "Localização salva com sucesso!")
                }
                .addOnFailureListener {
                    Log.e("localização", "Erro ao salvar localização", it)
                }
        }
    }
}
