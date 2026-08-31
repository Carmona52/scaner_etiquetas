package com.example.etiquetas.adapters.etiquetas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.etiquetas.database.methods.EtiquetaGuardada
import com.example.etiquetas.databinding.ItemEtiquetaBinding
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.etiquetas.R

class EtiquetasAdapter(
    private val onEditar: (EtiquetaGuardada) -> Unit
) : ListAdapter<EtiquetaGuardada, EtiquetasAdapter.EtiquetaViewHolder>(
    EtiquetaDiffCallback()
) {

    inner class EtiquetaViewHolder(
        private val binding: ItemEtiquetaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(etiqueta: EtiquetaGuardada) {

            binding.txtClaveProducto.text = etiqueta.claveProducto ?: ""
            binding.txtDescripcion.text = etiqueta.descripcionArticulo ?: ""
            binding.txtPiezas.text = etiqueta.piezas ?: ""
            binding.txtKilos.text = etiqueta.kilos ?: ""
            binding.txtLote.text = etiqueta.lote ?: ""
            binding.txtFecha.text = etiqueta.fecha ?: ""
            binding.txtHora.text = etiqueta.hora ?: ""
            binding.txtClaveProducto.setBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (etiqueta.claveEditada)
                        android.R.color.holo_orange_light
                    else
                        android.R.color.transparent
                )
            )
            binding.txtPiezas.setBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (etiqueta.piezasEditadas)
                        android.R.color.holo_orange_light
                    else
                        android.R.color.transparent
                )
            )
            binding.txtKilos.setBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (etiqueta.kilosEditados)
                        android.R.color.holo_orange_light
                    else
                        android.R.color.transparent
                )
            )
            binding.txtLote.setBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (etiqueta.loteEditado)
                        android.R.color.holo_orange_light
                    else
                        android.R.color.transparent
                )
            )
            binding.btnEditar.setOnClickListener {
                onEditar(etiqueta)
            }

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EtiquetaViewHolder {

        val binding = ItemEtiquetaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return EtiquetaViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: EtiquetaViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    class EtiquetaDiffCallback :
        DiffUtil.ItemCallback<EtiquetaGuardada>() {

        override fun areItemsTheSame(
            oldItem: EtiquetaGuardada,
            newItem: EtiquetaGuardada
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: EtiquetaGuardada,
            newItem: EtiquetaGuardada
        ): Boolean {
            return oldItem == newItem
        }
    }
}