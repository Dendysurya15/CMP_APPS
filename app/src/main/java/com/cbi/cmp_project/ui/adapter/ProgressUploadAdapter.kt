package com.cbi.cmp_project.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R

class ProgressUploadAdapter : RecyclerView.Adapter<ProgressUploadAdapter.ViewHolder>() {

    private val progressList = listOf(10, 30, 50, 70, 90, 100, 20, 40, 25, 90, 100, 20, 40, 25) // Mock progress data

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_progress_upload, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val progress = progressList[position]
        holder.bind(progress)
    }

    override fun getItemCount(): Int = progressList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarUpload)
        private val percentageText: TextView = itemView.findViewById(R.id.percentageProgressBarCard)

        fun bind(progress: Int) {
            progressBar.progress = progress
            percentageText.text = "$progress%"
        }
    }
}
