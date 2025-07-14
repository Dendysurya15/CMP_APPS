package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.InspectionWithDetailRelations
import com.cbi.mobile_plantation.databinding.TableItemRowBinding
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListInspectionAdapter(
    private val featureName: String, // Add this parameter
    private val onItemClick: (InspectionWithDetailRelations) -> Unit
) : RecyclerView.Adapter<ListInspectionAdapter.InspectionDataViewHolder>() {

    private var inspectionPaths: List<InspectionWithDetailRelations> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<InspectionWithDetailRelations>) {
        inspectionPaths = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspectionDataViewHolder {
        val binding = TableItemRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InspectionDataViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InspectionDataViewHolder, position: Int) {
        holder.bind(inspectionPaths[position])
    }

    override fun getItemCount(): Int = inspectionPaths.size

    inner class InspectionDataViewHolder(
        private val binding: TableItemRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(inspectionPaths[position])
                }
            }
        }

        fun bind(item: InspectionWithDetailRelations) {
            binding.apply {

                flCheckBoxItemTph.visibility = View.GONE
                td1.visibility = View.VISIBLE
                td2.visibility = View.VISIBLE
                td3.visibility = View.VISIBLE
                td4.visibility = View.VISIBLE
                td5.visibility = View.GONE

                when (featureName) {
                    AppUtils.ListFeatureNames.ListFollowUpInspeksi -> {
                        td1.text = "${item.tph?.blok_kode ?: ""}-${item.tph?.nomor ?: ""}"

                        td2.text = item.inspeksi.created_date_start
                        td3.text = item.inspeksi.jml_pkk_inspeksi.toString()

                        val statusIcon = if (item.inspeksi.status_upload == "0") {
                            R.drawable.baseline_close_24
                        } else {
                            R.drawable.baseline_check_box_24
                        }

                        val iconColor = if (item.inspeksi.status_upload == "0") {
                            ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                        } else {
                            ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                        }

                        td4.text = ""
                        td4.gravity = Gravity.CENTER_VERTICAL
                        td4.setCompoundDrawablesWithIntrinsicBounds(0, statusIcon, 0, 0)
                        td4.compoundDrawables[1]?.setTint(iconColor)
                    }
                    else -> {
                        // Default behavior for other features
                        td1.text = "${item.tph?.blok_kode ?: ""}-${item.tph?.nomor ?: ""}"

                        td2.text = item.inspeksi.jml_pkk_inspeksi.toString()

                        val startTime = formatTime(item.inspeksi.created_date_start)
                        val endTime = formatTime(item.inspeksi.created_date_end)
                        td3.text = "$startTime\n$endTime"

                        val statusIcon = if (item.inspeksi.status_upload == "0") {
                            R.drawable.baseline_close_24
                        } else {
                            R.drawable.baseline_check_box_24
                        }

                        val iconColor = if (item.inspeksi.status_upload == "0") {
                            ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                        } else {
                            ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                        }

                        td4.text = ""
                        td4.gravity = Gravity.CENTER_VERTICAL
                        td4.setCompoundDrawablesWithIntrinsicBounds(0, statusIcon, 0, 0)
                        td4.compoundDrawables[1]?.setTint(iconColor)
                    }
                }
            }
        }

        private fun formatTime(dateTimeString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(dateTimeString)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                if (dateTimeString.contains(" ")) {
                    dateTimeString.split(" ").getOrNull(1)?.substring(0, 5) ?: "00:00"
                } else {
                    "00:00"
                }
            }
        }
    }
}