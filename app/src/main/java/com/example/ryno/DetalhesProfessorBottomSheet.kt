package com.example.ryno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso

class DetalhesProfessorBottomSheet(private val professor: Professor) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.bottom_sheet_professor, container, false)

        view.findViewById<TextView>(R.id.txtNome).text = professor.nome
        view.findViewById<TextView>(R.id.txtEmail).text = "Email: ${professor.email}"
        view.findViewById<TextView>(R.id.txtTelefone).text = "Telefone: ${professor.telefone}"
        view.findViewById<TextView>(R.id.txtCidade).text = "Cidade: ${professor.cidade}"
        view.findViewById<TextView>(R.id.txtCref).text = "CREF: ${professor.cref}"
        view.findViewById<TextView>(R.id.txtModalidades).text = "Modalidades: ${professor.modalidades.joinToString(", ")}"

        professor.profileImageUrl?.let {
            Picasso.get().load(it).into(view.findViewById(R.id.imgFoto) as ImageView)
        }

        return view
    }
}