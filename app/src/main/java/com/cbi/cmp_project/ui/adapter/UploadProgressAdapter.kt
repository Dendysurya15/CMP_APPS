package com.cbi.cmp_project.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R



data class UploadItem(
    val id: Int,
    val deptPpro: Int,
    val divisiPpro: Int,
    val commodity: Int,
    val blokJjg: String,
    val nopol: String,
    val driver: String,
    val pemuatId: String,
    val transporterId: Int,
    val millId: Int,
    val createdById: Int,
    val createdAt: String,
    val noEspb: String
)


class UploadProgressAdapter(private val uploadItems: List<UploadItem>) : RecyclerView.Adapter<UploadProgressAdapter.UploadViewHolder>() {

    class UploadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNameProgress: TextView = itemView.findViewById(R.id.tv_name_progress)
        val progressBarUpload: ProgressBar = itemView.findViewById(R.id.progressBarUpload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UploadViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_progress_download, parent, false)
        return UploadViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UploadViewHolder, position: Int) {
        val item = uploadItems[position]
        holder.tvNameProgress.text = if (item.id == -1) {
            "(CMP) ${item.noEspb}" // Merged item
        } else {
            "(PPRO) ${item.noEspb}" // Individual item
        }
        holder.progressBarUpload.progress = 0  // Assuming upload is complete for display
    }

    override fun getItemCount(): Int = uploadItems.size
}
