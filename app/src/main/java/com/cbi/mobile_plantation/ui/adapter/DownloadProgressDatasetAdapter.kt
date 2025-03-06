package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
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

data class DownloadItem(
    val dataset: String,
    val progress: Int = 0,
    var isCompleted: Boolean = false,
    var isLoading: Boolean = false,
    var isExtracting: Boolean = false,
    var isExtractionCompleted: Boolean = false,
    var isStoring: Boolean = false,  // Add this
    var isStoringCompleted: Boolean = false,  // Add this
    var isUpToDate: Boolean = false,
    var error: String? = null,
    var message : String?  = null,
)

class DownloadProgressDatasetAdapter : RecyclerView.Adapter<DownloadProgressDatasetAdapter.ViewHolder>() {
    private var items = mutableListOf<DownloadItem>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameProgress: TextView = itemView.findViewById(R.id.tv_name_progress)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarUpload)
        val percentage: TextView = itemView.findViewById(R.id.percentageProgressBarCard)
        val statusProgress: TextView = itemView.findViewById(R.id.status_progress)
        val iconStatus: ImageView = itemView.findViewById(R.id.icon_status_progress)
        val loadingCircular: ProgressBar = itemView.findViewById(R.id.progress_circular_loading)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_progress_download, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.apply {
            nameProgress.text = item.dataset
            progressBar.progress = item.progress
            percentage.text = "${item.progress}%"

            when {

                item.error != null -> {
                    statusProgress.visibility = View.VISIBLE
                    statusProgress.text = item.error
                    statusProgress.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorRedDark))

                    iconStatus.visibility = View.VISIBLE
                    iconStatus.setImageResource(es.dmoral.toasty.R.drawable.ic_error_outline_white_24dp)
                    iconStatus.setColorFilter(ContextCompat.getColor(itemView.context, R.color.colorRedDark))
                    loadingCircular.visibility = View.GONE
                }
                item.isUpToDate -> {  // Add this condition
                    statusProgress.visibility = View.VISIBLE
                    statusProgress.text = item.message
                    statusProgress.setTextColor(ContextCompat.getColor(itemView.context, R.color.greendarkerbutton))
                    iconStatus.visibility = View.VISIBLE
                    iconStatus.setImageResource(R.drawable.baseline_check_24)
                    iconStatus.setColorFilter(ContextCompat.getColor(itemView.context, R.color.greendarkerbutton))
                    loadingCircular.visibility = View.GONE
                    progressBar.isIndeterminate = false
                }
                item.isStoring -> {
                    statusProgress.visibility = View.VISIBLE
                    statusProgress.text = item.message
                    statusProgress.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                    iconStatus.visibility = View.GONE
                    loadingCircular.visibility = View.VISIBLE
                    progressBar.isIndeterminate = true
                }
                item.isStoringCompleted -> {
                    statusProgress.visibility = View.VISIBLE

                    if(item.dataset == AppUtils.DatasetNames.updateSyncLocalData){
                        statusProgress.text = item.message
                        if (item.message!!.contains(".zip")){
                            statusProgress.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorRedDark))
                        }else{
                            statusProgress.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                        }
                    }else{
                        statusProgress.text = "${item.dataset} successfully stored"
                        statusProgress.maxLines = 5
                        statusProgress.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                    }
                    iconStatus.visibility = View.VISIBLE
                    iconStatus.setImageResource(R.drawable.baseline_check_24)
                    iconStatus.setColorFilter(ContextCompat.getColor(itemView.context, R.color.greendarkerbutton))
                    loadingCircular.visibility = View.GONE
                    progressBar.isIndeterminate = false
                }
                item.isExtracting -> {
                    statusProgress.visibility = View.VISIBLE
                    statusProgress.text = "${item.message}"
                    statusProgress.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                    iconStatus.visibility = View.GONE
                    loadingCircular.visibility = View.VISIBLE
                    progressBar.isIndeterminate = true
                }
                item.isExtractionCompleted -> {
                    statusProgress.visibility = View.VISIBLE
                    statusProgress.text = "Extraction complete"
                    iconStatus.visibility = View.VISIBLE
                    iconStatus.setImageResource(R.drawable.baseline_check_24)
                    iconStatus.setColorFilter(ContextCompat.getColor(itemView.context, R.color.greendarkerbutton))
                    loadingCircular.visibility = View.GONE
                    progressBar.isIndeterminate = false
                }
                item.isCompleted -> {
                    statusProgress.visibility = View.VISIBLE
                    statusProgress.text = "Download complete"
                    iconStatus.visibility = View.VISIBLE
                    iconStatus.setImageResource(R.drawable.baseline_check_24)
                    iconStatus.setColorFilter(ContextCompat.getColor(itemView.context, R.color.greendarkerbutton))
                    loadingCircular.visibility = View.GONE
                    progressBar.isIndeterminate = false
                }
                item.isLoading -> {
                    statusProgress.visibility = View.VISIBLE
                    if(item.dataset == AppUtils.DatasetNames.updateSyncLocalData){
                        statusProgress.text = "Sedang sinkronisasi data..."
                    }else{
                        statusProgress.text = "Downloading: ${item.progress}%"
                    }

                    loadingCircular.visibility = View.VISIBLE
                    progressBar.isIndeterminate = false
                }
                else -> {
                    statusProgress.visibility = View.VISIBLE
                    statusProgress.text = "Pending"
                    iconStatus.visibility = View.GONE
                    loadingCircular.visibility = View.GONE
                    progressBar.isIndeterminate = false
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<DownloadItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateProgress(dataset: String, progress: Int) {
        val index = items.indexOfFirst { it.dataset == dataset }
        if (index != -1) {
            items[index] = items[index].copy(progress = progress)
            notifyItemChanged(index)
        }
    }
}