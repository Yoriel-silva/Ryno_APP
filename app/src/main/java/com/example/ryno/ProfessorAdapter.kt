package com.example.ryno

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation

class ProfessorAdapter(
    private var lista: List<Professor>,
    private val onClick: (Professor) -> Unit
) : RecyclerView.Adapter<ProfessorAdapter.ProfessorViewHolder>() {

    class ProfessorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nome: TextView = view.findViewById(R.id.txtNome)
        val cidade: TextView = view.findViewById(R.id.txtCidade)
        val modalidades: TextView = view.findViewById(R.id.txtModalidades)
        val imagem: ImageView = view.findViewById(R.id.imgFoto)
        val distancia: TextView = view.findViewById(R.id.txtDistancia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfessorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_professor, parent, false)
        return ProfessorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfessorViewHolder, position: Int) {
        val professor = lista[position]
        holder.nome.text = professor.nome
        holder.cidade.text = professor.cidade
        holder.modalidades.text = professor.modalidades.joinToString(", ")
        holder.distancia.text = professor.distanciaKm?.let {
            "Distância: %.2f km".format(it)
        } ?: "Distância desconhecida"

        professor.profileImageUrl?.let {
            Picasso.get().load(it).transform(CropCircleTransformation()).into(holder.imagem)
        }

        holder.itemView.setOnClickListener {
            onClick(professor)
        }
    }

    override fun getItemCount(): Int = lista.size

    fun atualizarLista(novaLista: List<Professor>) {
        lista = novaLista
        notifyDataSetChanged() // 🔄 Garante que tudo seja reprocessado com localização atual
    }
}
