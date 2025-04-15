package com.example.ryno

import java.io.Serializable

data class Professor(
    val nome: String = "",
    val email: String = "",
    val telefone: String = "",
    val cidade: String = "",
    val cref: String = "",
    val modalidades: List<String> = emptyList(),
    val profileImageUrl: String? = null
) : Serializable