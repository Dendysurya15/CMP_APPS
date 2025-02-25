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
import com.cbi.cmp_project.ui.viewModel.WeighBridgeViewModel


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
    val noSPB: String
)


class UploadProgressAdapter(
    private val uploadItems: List<UploadItem>,
    private val viewModel: WeighBridgeViewModel
) : RecyclerView.Adapter<UploadProgressAdapter.UploadViewHolder>() {

    private val uploadProgressMap = mutableMapOf<Int, Int>()
    private val uploadStatusMap = mutableMapOf<Int, String>() // Tracks item status

    init {
        viewModel.uploadProgress.observeForever { progressMap ->
            uploadProgressMap.putAll(progressMap)
            progressMap.forEach { (id, progress) ->
                uploadStatusMap[id] = when {
                    progress == 0 -> "Waiting"
                    progress in 1..99 -> "Uploading"
                    progress == 100 -> "Success"
                    else -> "Failed"
                }
            }
            notifyDataSetChanged()
        }
    }

    class UploadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNameProgress: TextView = itemView.findViewById(R.id.tv_name_progress)
        val progressBarUpload: ProgressBar = itemView.findViewById(R.id.progressBarUpload)
        val percentage: TextView = itemView.findViewById(R.id.percentageProgressBarCard)
        val statusProgress: TextView = itemView.findViewById(R.id.status_progress)
        val iconStatus: ImageView = itemView.findViewById(R.id.icon_status_progress)
        val loadingCircular: ProgressBar = itemView.findViewById(R.id.progress_circular_loading)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UploadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_progress_download, parent, false)
        return UploadViewHolder(view)
    }

    override fun onBindViewHolder(holder: UploadViewHolder, position: Int) {
        val item = uploadItems[position]
        holder.tvNameProgress.text = "(PPRO) ${item.noSPB}"

        // Set initial status to Waiting
        holder.statusProgress.visibility = View.VISIBLE
        val progress = uploadProgressMap[item.id] ?: 0
        val status = uploadStatusMap[item.id] ?: "Waiting"

        holder.percentage.text = "$progress%"
        holder.progressBarUpload.progress = progress
        holder.statusProgress.text = status

        // Update visual cues based on status
        when (status) {
            "Waiting" -> {
                holder.iconStatus.visibility = View.INVISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
            }
            "Uploading" -> {
                holder.iconStatus.visibility = View.INVISIBLE
                holder.loadingCircular.visibility = View.VISIBLE
            }
            "Success" -> {
                holder.iconStatus.setImageResource(R.drawable.baseline_check_24) // Assuming success icon drawable
                holder.iconStatus.visibility = View.VISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
            }
            "Failed" -> {
                holder.iconStatus.setImageResource(R.drawable.circle_exclamation_solid) // Assuming failed icon drawable
                holder.iconStatus.visibility = View.VISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
            }
        }
    }

    override fun getItemCount(): Int = uploadItems.size
}

