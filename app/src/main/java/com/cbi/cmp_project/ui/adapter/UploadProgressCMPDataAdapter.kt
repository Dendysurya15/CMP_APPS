package com.cbi.cmp_project.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.ui.viewModel.UploadCMPViewModel
import com.cbi.cmp_project.ui.viewModel.WeighBridgeViewModel
import com.cbi.cmp_project.utils.AppUtils

data class UploadCMPItem(
    val id: Int,
    val title: String,
    val fullPath: String,
)

class UploadProgressCMPDataAdapter(
    private val uploadItems: List<UploadCMPItem>,
) : RecyclerView.Adapter<UploadProgressCMPDataAdapter.UploadViewHolder>() {

    private val uploadProgressMap = mutableMapOf<Int, Int>()
    private val uploadStatusMap = mutableMapOf<Int, String>()
    private val uploadErrorMap = mutableMapOf<Int, String>()

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
        holder.tvNameProgress.text = item.title

        val progress = uploadProgressMap[item.id] ?: 0
        val status = uploadStatusMap[item.id] ?: AppUtils.UploadStatusUtils.WAITING
        val errorMessage = uploadErrorMap[item.id]

        holder.percentage.text = if (status == AppUtils.UploadStatusUtils.FAILED || progress == 100) "100%" else "$progress%"
        holder.progressBarUpload.progress = if (status == AppUtils.UploadStatusUtils.FAILED) 100 else progress

        holder.statusProgress.text = when {
            status == AppUtils.UploadStatusUtils.FAILED && !errorMessage.isNullOrEmpty() -> errorMessage
            else -> status
        }

        when (status) {
            AppUtils.UploadStatusUtils.WAITING -> {
                holder.iconStatus.visibility = View.INVISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
                holder.statusProgress.visibility = View.VISIBLE
            }
            AppUtils.UploadStatusUtils.UPLOADING -> {
                holder.iconStatus.visibility = View.INVISIBLE
                holder.loadingCircular.visibility = View.VISIBLE
                holder.statusProgress.visibility = View.VISIBLE
            }
            AppUtils.UploadStatusUtils.SUCCESS -> {
                holder.iconStatus.setImageResource(R.drawable.baseline_check_24)
                holder.iconStatus.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton))
                holder.iconStatus.visibility = View.VISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
                holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton))
            }
            AppUtils.UploadStatusUtils.FAILED -> {
                holder.iconStatus.setImageResource(es.dmoral.toasty.R.drawable.ic_error_outline_white_24dp)
                holder.iconStatus.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.colorRedDark))
                holder.iconStatus.visibility = View.VISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
                holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorRedDark))
            }
        }
    }

    override fun getItemCount(): Int = uploadItems.size

    // Update methods
    fun updateProgress(id: Int, progress: Int) {
        uploadProgressMap[id] = progress
        notifyItemChanged(uploadItems.indexOfFirst { it.id == id })
    }

    fun updateStatus(id: Int, status: String) {
        uploadStatusMap[id] = status
        notifyItemChanged(uploadItems.indexOfFirst { it.id == id })
    }

    fun updateError(id: Int, error: String) {
        uploadErrorMap[id] = error
        notifyItemChanged(uploadItems.indexOfFirst { it.id == id })
    }
}
