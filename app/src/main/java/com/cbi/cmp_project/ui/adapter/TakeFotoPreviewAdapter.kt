package com.cbi.cmp_project.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R

class TakeFotoPreviewAdapter(private val itemCount: Int) : RecyclerView.Adapter<TakeFotoPreviewAdapter.FotoViewHolder>() {

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivAddFotoUnit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.take_and_preview_foto_layout, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        // If you want to set different images or data to the ImageView for each item
        // holder.imageView.setImageResource(R.drawable.some_image)

        // Example: For each item you could have different logic
    }

    override fun getItemCount(): Int {
        return itemCount
    }
}