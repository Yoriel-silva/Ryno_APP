package com.example.ryno

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.graphics.Color

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPrefs.getBoolean("isLoggedIn", false)
        val userType = sharedPrefs.getString("userType", "") // "aluno" ou "professor"
        if (isLoggedIn) {
            when (userType) {
                "aluno" -> {
                    startActivity(Intent(this, HomeAlunoActivity::class.java))
                }
                "professor" -> {
                    startActivity(Intent(this, HomeProfessorActivity::class.java))
                }
                else -> {
                    // Caso o tipo não esteja definido corretamente, limpa o login
                    sharedPrefs.edit().clear().apply()
                    setContentView(R.layout.activity_login)
                }
            }
            finish()
        } else {
            setContentView(R.layout.activity_login)
        }

        // Codigo botão login
        val buttonLogin = findViewById<Button>(R.id.buttonLogin) // 1. Referencia o botão pelo ID

        buttonLogin.setOnClickListener {
            // 2. Cria o Intent para abrir a tela HomeAlunoActivity
            val intent = Intent(this, HomeAlunoActivity::class.java)
            startActivity(intent)
            finish() // Opcional: finaliza a LoginActivity para não voltar com o botão de voltar
        }


        // Texto "Criar Conta" parcialmente clicável
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
                ds.color = Color.parseColor("#6200EE") // Ou qualquer cor
            }
        }

        val startIndex = texto.indexOf("Criar Conta")
        val endIndex = startIndex + "Criar Conta".length
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        tvCriarConta.text = spannableString
        tvCriarConta.movementMethod = LinkMovementMethod.getInstance()


    }
}