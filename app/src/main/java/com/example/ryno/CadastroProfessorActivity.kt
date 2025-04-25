package com.example.ryno

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer

class CadastroProfessorActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val cidades = listOf(
        "São Paulo", "Salvador", "Fortaleza", "Belo Horizonte",
        "Rio de Janeiro", "Curitiba", "Brasília", "Manaus", "Recife", "Porto Alegre"
    )

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_CODE_LOCATION = 1001 // Defina um código único para

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_professor)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nome = findViewById<EditText>(R.id.NomeProfessor)
        val email = findViewById<EditText>(R.id.EmailProfessor)
        val telefone = findViewById<EditText>(R.id.TelefoneProfessor)
        val senha = findViewById<EditText>(R.id.SenhaProfessor)
        val cref = findViewById<EditText>(R.id.CREF)
        val checkbox = findViewById<CheckBox>(R.id.checkBox)
        val botaoCriar = findViewById<Button>(R.id.Btn_CriarProfessor)

        val tvModalidades = findViewById<TextView>(R.id.modalidadeSelecionada)
        val modalidades = arrayOf("Futebol", "Basquete", "Vôlei", "Natação")
        val modalidadesSelecionadas = BooleanArray(modalidades.size)
        val modalidadesEscolhidas = mutableListOf<String>()

        val autoCidade = findViewById<AutoCompleteTextView>(R.id.autoCidade)
        val adapter = CidadeAdapter(this, cidades)
        autoCidade.setAdapter(adapter)

        tvModalidades.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Selecione as modalidades")
            builder.setMultiChoiceItems(modalidades, modalidadesSelecionadas) { _, which, isChecked ->
                modalidadesSelecionadas[which] = isChecked
            }
            builder.setPositiveButton("OK") { _, _ ->
                modalidadesEscolhidas.clear()
                for (i in modalidades.indices) {
                    if (modalidadesSelecionadas[i]) {
                        modalidadesEscolhidas.add(modalidades[i])
                    }
                }
                tvModalidades.text = if (modalidadesEscolhidas.isNotEmpty())
                    modalidadesEscolhidas.joinToString(", ")
                else
                    "Escolha uma modalidade"
            }
            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }

        botaoCriar.setOnClickListener {
            val nomeTxt = nome.text.toString().trim()
            val emailTxt = email.text.toString().trim()
            val telefoneTxt = telefone.text.toString().trim()
            val senhaTxt = senha.text.toString().trim()
            val crefTxt = cref.text.toString().trim()
            val cidade = autoCidade.text.toString().trim()

            if (nomeTxt.isEmpty() || emailTxt.isEmpty() || telefoneTxt.isEmpty() || senhaTxt.isEmpty() || crefTxt.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!checkbox.isChecked) {
                Toast.makeText(this, "Você deve aceitar os termos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(emailTxt, senhaTxt)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: ""
                        val professorData = hashMapOf(
                            "uid" to userId,
                            "nome" to nomeTxt,
                            "email" to emailTxt,
                            "telefone" to telefoneTxt,
                            "cref" to crefTxt,
                            "cidade" to cidade,
                            "modalidades" to modalidadesEscolhidas,
                            "tipo" to "professor"
                        )

                        db.collection("usuarios").document(userId)
                            .set(professorData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()

                                val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                                sharedPrefs.edit().putBoolean("isLoggedIn", true).apply()
                                val userType = "professor"
                                // Salva o tipo de usuário no SharedPreferences
                                sharedPrefs.edit().putString("userType", userType).apply()

                                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

                                // Verificar permissão de localização
                                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
                                } else {
                                    obterLocalizacao() // Caso já tenha permissão, obter a localização imediatamente
                                }

                                startActivity(Intent(this, LoginActivity::class.java)) // adicione aqui o destino
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Erro ao salvar dados: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Erro ao criar usuário: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        Log.e("Auth", "Erro ao criar usuário", task.exception)
                    }
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
}
