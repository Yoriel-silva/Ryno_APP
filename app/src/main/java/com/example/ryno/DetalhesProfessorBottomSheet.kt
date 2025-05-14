package com.example.ryno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation

class DetalhesProfessorBottomSheet(private val professor: Professor) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.bottom_sheet_professor, container, false)

        view.findViewById<TextView>(R.id.txtNome).text = professor.nome
        view.findViewById<TextView>(R.id.txtEmail).text = "Email: ${professor.email}"
        view.findViewById<TextView>(R.id.txtTelefone).text = "Telefone: ${professor.telefone}"
        view.findViewById<TextView>(R.id.txtCidade).text = "Cidade: ${professor.cidade}"

        val crefTextView = view.findViewById<TextView>(R.id.txtCref)
        val crefLink = "https://www.confef.org.br"
        crefTextView.text = android.text.Html.fromHtml("<a href=\"$crefLink\">CREF: ${professor.cref}</a>")
        crefTextView.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        view.findViewById<TextView>(R.id.txtModalidades).text = "Modalidades: ${professor.modalidades.joinToString(", ")}"

        professor.profileImageUrl?.let {
            Picasso.get().load(it).transform(CropCircleTransformation()).into(view.findViewById(R.id.imgFoto) as ImageView)
        }

        return view
    }
}