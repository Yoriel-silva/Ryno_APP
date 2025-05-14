package com.example.ryno

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuthException

class CadastroAlunoGoogleActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_CODE_LOCATION = 1001 // Defina um código único para

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_aluno_google)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nomeEditText = findViewById<EditText>(R.id.NomeAluno)
        val telefoneEditText = findViewById<EditText>(R.id.TelefoneAluno)

        val termosTextView: TextView = findViewById(R.id.Termos)

        termosTextView.setOnClickListener {
            // Criando o Dialog personalizado
            val dialogView = layoutInflater.inflate(R.layout.dialog_termos, null)

            // Criando o AlertDialog
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            // Configurando o botão de fechar
            val closeButton: Button = dialogView.findViewById(R.id.btn_close)
            closeButton.setOnClickListener {
                dialog.dismiss() // Fechar o pop-up
            }

            // Exibir o Dialog
            dialog.show()
        }

        val checkBox = findViewById<CheckBox>(R.id.checkBox)
        val botaoCriar = findViewById<Button>(R.id.Btn_CriarAluno)

        botaoCriar.setOnClickListener {
            val nome = nomeEditText.text.toString().trim()
            val telefone = telefoneEditText.text.toString().trim()

            if (nome.isEmpty() || telefone.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!nome.matches(Regex("^[A-Za-zÀ-ÿ\\s]+\$"))) {
                Toast.makeText(this, "O nome não deve conter números ou símbolos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!telefone.matches(Regex("^\\d{11}$"))) {
                Toast.makeText(this, "Telefone deve conter 11 dígitos numéricos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!checkBox.isChecked) {
                Toast.makeText(this, "Você precisa aceitar os termos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("usuarios").whereEqualTo("telefone", telefone).get().addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        Toast.makeText(this, "Telefone já cadastrado", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                // Só cria o usuário se o telefone ainda não estiver cadastrado
                val userId = auth.currentUser?.uid ?: ""
                val user = FirebaseAuth.getInstance().currentUser
                val email = user?.email
                val alunoData = hashMapOf(
                    "uid" to userId,
                    "nome" to nome,
                    "email" to email,
                    "telefone" to telefone,
                    "tipo" to "aluno"
                )

                db.collection("usuarios").document(userId)
                    .set(alunoData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()

                        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        sharedPrefs.edit().putBoolean("isLoggedIn", true).apply()
                        sharedPrefs.edit().putString("userType", "aluno").apply()

                        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                            ) {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
                        } else {
                            obterLocalizacao()
                        }

                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Erro ao salvar dados: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener {
                Toast.makeText(this, "Erro ao verificar Telefone: ${it.message}", Toast.LENGTH_SHORT).show()
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

