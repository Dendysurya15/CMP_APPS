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
import com.cbi.mobile_plantation.ui.viewModel.WeighBridgeViewModel


data class UploadItem(
    val id: Int,
    val ip: String,
    val num: Int,
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
    val no_espb: String,
    val uploader_info:String,
    val uploaded_at:String,
    val uploaded_by_id:Int,
    val file:String,
    val endpoint:String,
)


class UploadProgressAdapter(
    private val uploadItems: List<UploadItem>,
    private val viewModel: WeighBridgeViewModel
) : RecyclerView.Adapter<UploadProgressAdapter.UploadViewHolder>() {

    private val uploadProgressMap = mutableMapOf<Int, Int>()
    private val uploadStatusMap = mutableMapOf<Int, String>()
    private val uploadErrorMap = mutableMapOf<Int, String>()

    init {
        viewModel.uploadProgress.observeForever { progressMap ->
            uploadProgressMap.clear()
            uploadProgressMap.putAll(progressMap)
            notifyDataSetChanged()
        }

        viewModel.uploadStatusMap.observeForever { statusMap ->
            uploadStatusMap.clear()
            uploadStatusMap.putAll(statusMap)
            notifyDataSetChanged()
        }

        // Add an observer for error messages
        viewModel.uploadErrorMap.observeForever { errorMap ->
            uploadErrorMap.clear()
            uploadErrorMap.putAll(errorMap)
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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UploadViewHolder, position: Int) {
        val item = uploadItems[position]
        holder.tvNameProgress.text = "${item.endpoint} (${item.no_espb})"


        // Get progress, status and error
        val progress = uploadProgressMap[item.num] ?: 0
        val status = uploadStatusMap[item.num] ?: "Waiting"
        val errorMessage = uploadErrorMap[item.num]

        // Update UI based on progress and status
        holder.percentage.text = if (status == "Failed" || progress == 100) "100%" else "$progress%"
        holder.progressBarUpload.progress = if (status == "Failed") 100 else progress

        // Set status text (show error message if available for Failed status)
        holder.statusProgress.text = when {
            status == "Failed" && !errorMessage.isNullOrEmpty() -> errorMessage
            else -> status
        }

        when (status) {
            "Waiting" -> {
                holder.iconStatus.visibility = View.INVISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
                holder.statusProgress.visibility = View.VISIBLE
                holder.iconStatus.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.black))
            }
            "Uploading" -> {
                holder.iconStatus.visibility = View.INVISIBLE
                holder.loadingCircular.visibility = View.VISIBLE
                holder.statusProgress.visibility = View.VISIBLE
                holder.iconStatus.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.black))
            }
            "Success" -> {
                holder.iconStatus.setImageResource(R.drawable.baseline_check_24)
                holder.iconStatus.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton))
                holder.iconStatus.visibility = View.VISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
                holder.statusProgress.visibility = View.VISIBLE
                holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton))
            }
            "Failed" -> {
                holder.iconStatus.setImageResource(es.dmoral.toasty.R.drawable.ic_error_outline_white_24dp)
                holder.iconStatus.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.colorRedDark))
                holder.iconStatus.visibility = View.VISIBLE
                holder.loadingCircular.visibility = View.INVISIBLE
                holder.statusProgress.visibility = View.VISIBLE
                holder.statusProgress.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorRedDark))
            }
        }
    }

    override fun getItemCount(): Int = uploadItems.size
}

