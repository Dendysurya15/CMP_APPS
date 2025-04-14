package com.cbi.mobile_plantation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppUtils
import java.io.File

data class UploadCMPItem(
    val id: Int,
    val title: String,
    val fullPath: String,
    val partNumber: Int,
    val totalParts: Int,
    val baseFilename: String
)

class UploadProgressCMPDataAdapter(
    private val uploadItems: List<UploadCMPItem>,
) : RecyclerView.Adapter<UploadProgressCMPDataAdapter.UploadViewHolder>() {

    private val uploadProgressMap = mutableMapOf<Int, Int>()
    private val uploadStatusMap = mutableMapOf<Int, String>()
    private val uploadErrorMap = mutableMapOf<Int, String>()
    private val uploadedBytesMap = mutableMapOf<Int, Long>() // Track uploaded bytes
    private val fileSizeMap = mutableMapOf<Int, Long>() // Cache file sizes to avoid recalculating

    class UploadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNameProgress: TextView = itemView.findViewById(R.id.tv_name_progress)
        val progressBarUpload: ProgressBar = itemView.findViewById(R.id.progressBarUpload)
        val percentage: TextView = itemView.findViewById(R.id.percentageProgressBarCard)
            val statusProgress: TextView = itemView.findViewById(R.id.status_progress)
        val iconStatus: ImageView = itemView.findViewById(R.id.icon_status_progress)
        val loadingCircular: ProgressBar = itemView.findViewById(R.id.progress_circular_loading)
    }

    init {
        // Initialize file size map
        uploadItems.forEach { item ->
            try {
                val file = File(item.fullPath)
                if (file.exists()) {
                    fileSizeMap[item.id] = file.length()
                } else {
                    fileSizeMap[item.id] = 0L
                }
            } catch (e: Exception) {
                fileSizeMap[item.id] = 0L
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UploadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_progress_download, parent, false)
        return UploadViewHolder(view)
    }

    override fun onBindViewHolder(holder: UploadViewHolder, position: Int) {
        val item = uploadItems[position]
        val fileSize = fileSizeMap[item.id] ?: 0L
        val uploadedBytes = uploadedBytesMap[item.id] ?: 0L

        // Set file name with size
        holder.tvNameProgress.text = "${item.title} (${formatFileSize(fileSize)})"

        val progress = uploadProgressMap[item.id] ?: 0
        val status = uploadStatusMap[item.id] ?: AppUtils.UploadStatusUtils.WAITING
        val errorMessage = uploadErrorMap[item.id]

        // Set percentage display
        holder.percentage.text = if (status == AppUtils.UploadStatusUtils.FAILED || progress == 100) "100%" else "$progress%"
        holder.progressBarUpload.progress = if (status == AppUtils.UploadStatusUtils.FAILED) 100 else progress

        // Set status text
        holder.statusProgress.text = when {
            status == AppUtils.UploadStatusUtils.UPLOADING -> "${formatFileSize(uploadedBytes)} / ${formatFileSize(fileSize)}"
            status == AppUtils.UploadStatusUtils.FAILED && !errorMessage.isNullOrEmpty() -> errorMessage
            else -> status
        }

        // Set visibility of UI elements based on status
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

        // Calculate uploaded bytes based on progress percentage
        val fileSize = fileSizeMap[id] ?: 0L
        val uploadedBytes = (fileSize * progress) / 100
        uploadedBytesMap[id] = uploadedBytes

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


    // New method to directly update uploaded bytes
    fun updateUploadedBytes(id: Int, bytes: Long) {
        uploadedBytesMap[id] = bytes
        notifyItemChanged(uploadItems.indexOfFirst { it.id == id })
    }

    // Helper methods for tracking total progress
    fun getTotalFileSize(): Long {
        return fileSizeMap.values.sum()
    }

    fun getTotalUploadedBytes(): Long {
        return uploadedBytesMap.values.sum()
    }

    fun getOverallProgress(): Int {
        val totalBytes = getTotalFileSize()
        if (totalBytes <= 0) return 0

        val uploadedBytes = getTotalUploadedBytes()
        return ((uploadedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
    }

    // Helper method to format file size
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }

    // Add this method to UploadProgressCMPDataAdapter
    fun resetState() {
        uploadProgressMap.clear()
        uploadStatusMap.clear()
        uploadErrorMap.clear()
        uploadedBytesMap.clear()

        // Initialize file size map again
        uploadItems.forEach { item ->
            try {
                val file = File(item.fullPath)
                if (file.exists()) {
                    fileSizeMap[item.id] = file.length()
                } else {
                    fileSizeMap[item.id] = 0L
                }
            } catch (e: Exception) {
                fileSizeMap[item.id] = 0L
            }
        }

        notifyDataSetChanged()
    }
}
