package com.cbi.mobile_plantation.ui.adapter

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import java.io.File

data class UploadCMPItem(
    val id: Int,
    val title: String,
    val fullPath: String,
    val baseFilename: String,
    val data :String,
    val type:String,
    val tableIds: String? = null,
    val databaseTable : String,
)

class UploadProgressCMPDataAdapter(
    private val uploadItems: List<UploadCMPItem>,
) : RecyclerView.Adapter<UploadProgressCMPDataAdapter.UploadViewHolder>() {
    private var dataList = uploadItems.toMutableList()
    private val uploadProgressMap = mutableMapOf<Int, Int>()
    private val uploadStatusMap = mutableMapOf<Int, String>()
    private val uploadErrorMap = mutableMapOf<Int, String>()
    private val uploadedBytesMap = mutableMapOf<Int, Long>() // Track uploaded bytes
    private val fileSizeMap = mutableMapOf<Int, Long>() // Cache file sizes to avoid recalculating

    // Add these for dot animation
    private val animators = mutableMapOf<Int, ValueAnimator>()
    private val dotCountMap = mutableMapOf<Int, Int>()

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
        dataList.forEach { item ->
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

    fun getItems(): List<UploadCMPItem> {
        return dataList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UploadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_progress_download, parent, false)
        return UploadViewHolder(view)
    }


    fun setFileSize(itemId: Int, size: Long) {
        fileSizeMap[itemId] = size
        notifyDataSetChanged()
    }

    fun logFileSizeMap(): String {
        return fileSizeMap.entries.joinToString(", ") {
            "id=${it.key}: ${formatFileSize(it.value)}"
        }
    }

    fun setItems(items: List<UploadCMPItem>) {
        dataList.clear()
        dataList.addAll(items)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: UploadViewHolder, position: Int) {
        val item = dataList[position]
        val fileSize = fileSizeMap[item.id] ?: 0L
        // Your existing binding code
        if (item.title.contains("Master")) {
            holder.tvNameProgress.text = item.title
        } else {
            // Check if the title contains any of the standard dataset names
            val containsStandardDataset = AppUtils.DatasetNames::class.java.declaredFields
                .filter { it.type == String::class.java }
                .map { it.get(AppUtils.DatasetNames) as String }
                .any { datasetName -> item.title.contains(datasetName) }

            if (containsStandardDataset) {
                holder.tvNameProgress.text = item.title
            } else {
                holder.tvNameProgress.text = "${item.title} (${formatFileSize(fileSize)})"
            }
        }


        val progress = uploadProgressMap[item.id] ?: 0
        val status = uploadStatusMap[item.id] ?: AppUtils.UploadStatusUtils.WAITING
        val errorMessage = uploadErrorMap[item.id]

        // Set percentage display
        holder.percentage.text = if (status == AppUtils.UploadStatusUtils.FAILED || progress == 100) "100%" else "$progress%"
        holder.progressBarUpload.progress = if (status == AppUtils.UploadStatusUtils.FAILED) 100 else progress

        // Handle status text based on upload status
        when (status) {
            AppUtils.UploadStatusUtils.WAITING -> {
                stopDotsAnimation(item.id)
                if ((item.title.contains(AppUtils.DatasetNames.sinkronisasiRestan) || item.title.contains(AppUtils.DatasetNames.sinkronisasiDataPanen)) || item.title.contains(AppUtils.DatasetNames.sinkronisasiFollowUpInspeksi) && item.data.isNotEmpty()) {
                    holder.statusProgress.text = item.data
                } else {
                    holder.statusProgress.text = status
                }
            }
            AppUtils.UploadStatusUtils.UPLOADING -> {
                // Show error message if available while uploading
                if (!errorMessage.isNullOrEmpty()) {
                    // Show "Sedang Upload" + error message
                    startDotsAnimation(item.id, holder) // Keep the animation running
                    holder.statusProgress.text = "Sedang Upload - $errorMessage"

                    // Set red color for error messages
                    if (errorMessage.contains("✗") || errorMessage.contains("error") || errorMessage.contains("failed")) {
                        holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorRedDark))
                    } else {
                        // Green color for success messages
                        holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton))
                    }
                } else {
                    // No error message, show normal uploading with dots
                    startDotsAnimation(item.id, holder)
                    holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
                }
            }

            AppUtils.UploadStatusUtils.DOWNLOADING -> {
                startDotsAnimation(item.id, holder)
            }
            AppUtils.UploadStatusUtils.DOWNLOADED -> {
                stopDotsAnimation(item.id)
                holder.statusProgress.text = status
            }
            AppUtils.UploadStatusUtils.UPDATED -> {
                stopDotsAnimation(item.id)
                holder.statusProgress.text = "${item.title} - ${status}"
            }
            AppUtils.UploadStatusUtils.DONE_CHECK -> {
                stopDotsAnimation(item.id)
                holder.statusProgress.text = "${item.title} - ${status}"
            }
            AppUtils.UploadStatusUtils.UPTODATE -> {
                stopDotsAnimation(item.id)
                holder.statusProgress.text = "${item.title} - ${status}"
            }
            AppUtils.UploadStatusUtils.SUCCESS -> {
                stopDotsAnimation(item.id)
                holder.statusProgress.text = status
            }
            AppUtils.UploadStatusUtils.FAILED -> {
                stopDotsAnimation(item.id)
                holder.statusProgress.text = errorMessage ?: status
            }
        }

        when (status) {
            AppUtils.UploadStatusUtils.WAITING -> {
                holder.iconStatus.visibility = View.INVISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
                holder.statusProgress.visibility = View.VISIBLE
                if (item.data.contains("Error:", ignoreCase = true)) {
                    holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorRedDark))
                } else {
                    holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
                }
            }
            AppUtils.UploadStatusUtils.UPLOADING -> {
                holder.iconStatus.visibility = View.INVISIBLE
                holder.loadingCircular.visibility = View.VISIBLE
                holder.statusProgress.visibility = View.VISIBLE

            }
            AppUtils.UploadStatusUtils.DOWNLOADING -> {
                holder.iconStatus.visibility = View.INVISIBLE
                holder.loadingCircular.visibility = View.VISIBLE
                holder.statusProgress.visibility = View.VISIBLE
                holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
            }
            AppUtils.UploadStatusUtils.DOWNLOADED -> {
                holder.iconStatus.setImageResource(R.drawable.baseline_check_24)
                holder.iconStatus.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton))
                holder.iconStatus.visibility = View.VISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
                holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton))
            }
            AppUtils.UploadStatusUtils.UPDATED -> {
                holder.iconStatus.setImageResource(R.drawable.baseline_check_24)
                holder.iconStatus.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton))
                holder.iconStatus.visibility = View.VISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
                holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton))
            }
            AppUtils.UploadStatusUtils.UPTODATE -> {
                holder.iconStatus.setImageResource(R.drawable.baseline_do_not_disturb_24)
                holder.iconStatus.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.graytextdark))
                holder.iconStatus.visibility = View.VISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
                holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
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

    private fun startDotsAnimation(id: Int, holder: UploadViewHolder) {
        // Cancel existing animation if any
        animators[id]?.cancel()

        // Create a new animator
        val animator = ValueAnimator.ofInt(0, 4).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART

            addUpdateListener { animation ->
                val dotCount = animation.animatedValue as Int
                dotCountMap[id] = dotCount
                updateStatusWithDots(id, holder)
            }
        }

        // Store and start the animator
        animators[id] = animator
        animator.start()
    }

    private fun stopDotsAnimation(id: Int) {
        animators[id]?.cancel()
        animators.remove(id)
        dotCountMap.remove(id)
    }

    private fun updateStatusWithDots(id: Int, holder: UploadViewHolder) {
        val dotCount = dotCountMap[id] ?: 0
        val currentStatus = uploadStatusMap[id] ?: AppUtils.UploadStatusUtils.WAITING
        val errorMessage = uploadErrorMap[id]

        val dots = when (dotCount) {
            0 -> ""
            1 -> "."
            2 -> ".."
            3 -> "..."
            4 -> "...."
            else -> "....."
        }

        // Show status with dots, and error message if available
        if (!errorMessage.isNullOrEmpty()) {
            holder.statusProgress.text = "$currentStatus$dots\n$errorMessage"
            // Set color based on error type
            if (errorMessage.contains("✗") || errorMessage.contains("error") || errorMessage.contains("failed")) {
                holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorRedDark))
            } else {
                holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton))
            }
        } else {
            holder.statusProgress.text = "$currentStatus$dots"
            // Normal uploading - black color
            holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
        }
    }


    override fun getItemCount(): Int = dataList.size

    // Update methods
    fun updateProgress(id: Int, progress: Int) {
        uploadProgressMap[id] = progress

        // Calculate uploaded bytes based on progress percentage
        val fileSize = fileSizeMap[id] ?: 0L
        val uploadedBytes = (fileSize * progress) / 100
        uploadedBytesMap[id] = uploadedBytes

        // Get the position of the item in the list
        val position = dataList.indexOfFirst { it.id == id }
        if (position != -1) {
            // Get the ViewHolder if it's visible
            val holder = (dataList[position].id == id) as? UploadViewHolder
            if (holder != null) {
                // Update the status text with dots if it's in UPLOADING state
                val status = uploadStatusMap[id] ?: AppUtils.UploadStatusUtils.WAITING
                if (status == AppUtils.UploadStatusUtils.UPLOADING) {
                    updateStatusWithDots(id, holder)
                }
            }

            notifyItemChanged(position)
        }
    }

    fun updateStatus(id: Int, status: String) {
        uploadStatusMap[id] = status
        notifyItemChanged(dataList.indexOfFirst { it.id == id })
    }

    fun updateError(id: Int, error: String) {
        uploadErrorMap[id] = error
        notifyItemChanged(dataList.indexOfFirst { it.id == id })
    }

    // New method to directly update uploaded bytes
    fun updateUploadedBytes(id: Int, bytes: Long) {
        uploadedBytesMap[id] = bytes
        notifyItemChanged(dataList.indexOfFirst { it.id == id })
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
        // Cancel all animations
        animators.values.forEach { it.cancel() }
        animators.clear()
        dotCountMap.clear()

        uploadProgressMap.clear()
        uploadStatusMap.clear()
        uploadErrorMap.clear()
        uploadedBytesMap.clear()

        // We're NOT clearing fileSizeMap here to preserve the sizes
        // fileSizeMap.clear() - remove this line if it exists

        notifyDataSetChanged()
    }

    // Add this method to access file sizes by ID
    fun getFileSizeById(id: Int): Long {
        return fileSizeMap[id] ?: 0L
    }

    fun updateItems(newItems: List<UploadCMPItem>) {
        // Save the current file sizes before clearing the list
        val savedFileSizes = fileSizeMap.toMap()

        // Update the data list
        dataList.clear()
        dataList.addAll(newItems)

        // Restore file sizes for items that still exist (by ID)
        newItems.forEach { item ->
            if (savedFileSizes.containsKey(item.id)) {
                fileSizeMap[item.id] = savedFileSizes[item.id] ?: 0L
            }
        }

        notifyDataSetChanged()
    }
    // Cleanup method to be called when the RecyclerView is detached
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Cancel all animations to prevent memory leaks
        animators.values.forEach { it.cancel() }
        animators.clear()
        dotCountMap.clear()
    }
}
