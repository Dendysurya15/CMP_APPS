package com.cbi.cmp_project.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R

class ProgressUploadAdapter(
    private val progressList: MutableList<Int>,
    private val statusList: MutableList<String>,
    private val iconList: MutableList<Int>,
    private val fileNames: MutableList<String> // Add the fileNames list
) : RecyclerView.Adapter<ProgressUploadAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_progress_upload, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val progress = progressList[position]
        val status = statusList[position]
        val iconResId = iconList[position]
        val fileName = fileNames[position] // Get file name
        holder.bind(progress, status, iconResId, fileName)
    }

    override fun getItemCount(): Int = progressList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarUpload)
        private val percentageText: TextView = itemView.findViewById(R.id.percentageProgressBarCard)
        private val statusText: TextView = itemView.findViewById(R.id.status_progress)
        private val statusIcon: ImageView = itemView.findViewById(R.id.icon_status_progress)
        private val progressCircular: ProgressBar = itemView.findViewById(R.id.progress_circular_loading)
        private val fileNameText: TextView = itemView.findViewById(R.id.tv_name_progress)

        fun bind(progress: Int, status: String, iconResId: Int, fileName: String) {
            // Set filename
            fileNameText.text = fileName

            when {
                status == "Sedang Mengunduh" -> {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = progress
                    percentageText.text = "$progress%"
                    percentageText.visibility = View.VISIBLE
                    statusText.text = status
                    statusText.visibility = View.VISIBLE
                    progressCircular.visibility = View.VISIBLE
                    statusIcon.visibility = View.GONE
                }
                progress == 100 -> {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = progress
                    percentageText.text = "$progress%"
                    percentageText.visibility = View.VISIBLE
                    statusText.text = status
                    statusText.visibility = View.VISIBLE
                    progressCircular.visibility = View.GONE
                    statusIcon.visibility = View.VISIBLE
                    statusIcon.setImageResource(iconResId)

                    if (status.startsWith("Unduh Gagal")) {
                        statusText.setTextColor(itemView.context.getColor(R.color.colorRedDark))  // Set to red for failure
                        statusIcon.setColorFilter(itemView.context.getColor(R.color.colorRedDark))
                    }
                }
                else -> {
                    // Waiting state
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = 0
                    percentageText.text = "0%"
                    percentageText.visibility = View.VISIBLE
                    statusText.text = status
                    statusText.visibility = View.VISIBLE
                    progressCircular.visibility = View.GONE
                    statusIcon.visibility = View.GONE
                }
            }
        }
    }

    fun resetProgress(position: Int) {
        progressList[position] = 0
        statusList[position] = "Menunggu"
        iconList[position] = R.drawable.progress_circular
        notifyItemChanged(position)
    }

    // Method to update progress
    fun updateProgress(position: Int, progress: Int, status: String, iconResId: Int) {
        progressList[position] = progress
        statusList[position] = status
        iconList[position] = iconResId
        notifyItemChanged(position)
    }
}




