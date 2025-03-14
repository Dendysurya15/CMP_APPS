package com.cbi.mobile_plantation.ui.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cbi.mobile_plantation.R
import java.io.File

class PhotoAttachmentAdapterDetailTable(
    private var photos: List<String>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<PhotoAttachmentAdapterDetailTable.PhotoViewHolder>() {

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivAddFoto)
        val titleComment: TextView? = itemView.findViewById(R.id.titleComment)
        val etPhotoComment: TextView? = itemView.findViewById(R.id.etPhotoComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.take_and_preview_foto_layout, parent, false)
        return PhotoViewHolder(view)
    }

    // Always return 3 items, regardless of photos list size
    override fun getItemCount(): Int {
        return 3
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        // Hide comment sections
        holder.titleComment?.visibility = View.GONE
        holder.etPhotoComment?.visibility = View.GONE

        // Calculate a more precise width that accounts for margins and spacing
        val layoutParams = holder.itemView.layoutParams
        if (layoutParams != null) {
            val displayMetrics = holder.itemView.context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels

            // Account for container padding/margins
            val containerPadding = (32 * displayMetrics.density).toInt()
            val availableWidth = screenWidth - containerPadding

            // Divide by 3 exactly and account for item margins
            val itemMargin = (10 * displayMetrics.density).toInt()
            val itemWidth = (availableWidth / 3) - itemMargin

            layoutParams.width = itemWidth
            holder.itemView.layoutParams = layoutParams
        }

        // Check if we have a real photo for this position
        if (position < photos.size) {
            // We have a real photo, load it from file
            val photoFile = File(photos[position])

            if (photoFile.exists()) {
                // Load from file if it exists
                Glide.with(holder.itemView.context)
                    .load(photoFile)
                    .placeholder(R.drawable.baseline_add_a_photo_24)
                    .error(R.drawable.baseline_add_a_photo_24)
                    .centerCrop()
                    .into(holder.imageView)

                Log.d("PhotoAdapter", "Loading from file: ${photoFile.absolutePath}")
            } else {
                // File doesn't exist, show error placeholder
                Log.e("PhotoAdapter", "File doesn't exist: ${photoFile.absolutePath}")
                holder.imageView.setImageResource(R.drawable.baseline_add_a_photo_24)
            }

            holder.imageView.setOnClickListener {
                onItemClick(position)
            }
        } else {
            // No photo for this position, just show the placeholder
            holder.imageView.setImageResource(R.drawable.baseline_add_a_photo_24)
            holder.imageView.setOnClickListener(null) // Remove click listener
        }
    }
}