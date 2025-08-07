package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
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

        @SuppressLint("SetTextI18n")
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
                        td2.text = item.inspeksi.created_date
                        td3.text = item.inspeksi.jml_pkk_inspeksi.toString()

                        // Create two-line status display
                        val pemulihan = "PEMULIHAN"
                        val upload = "UPLOAD"

                        // Determine PEMULIHAN status - check if inspeksi_putaran == 2
                        val pemulihanIcon = if (item.inspeksi.inspeksi_putaran == 2) "✓" else "✗"

                        // Determine UPLOAD status
                        val uploadIcon = if (item.inspeksi.status_upload == "0") "✗" else "✓"

                        // Create two-line text with icons
                        val statusText = SpannableStringBuilder().apply {
                            // PEMULIHAN line
                            append(pemulihan)
                            append(" ")
                            val pemulihanStart = length
                            append(pemulihanIcon)
                            val pemulihanEnd = length

                            // Set color for PEMULIHAN icon
                            val pemulihanColor = if (item.inspeksi.inspeksi_putaran == 2) {
                                ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                            } else {
                                ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                            }

                            setSpan(
                                ForegroundColorSpan(pemulihanColor),
                                pemulihanStart,
                                pemulihanEnd,
                                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            append("\n")

                            // UPLOAD line
                            append(upload)
                            append(" ")
                            val uploadStart = length
                            append(uploadIcon)
                            val uploadEnd = length

                            // Set color for UPLOAD icon
                            val uploadColor = if (item.inspeksi.status_upload == "0") {
                                ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                            } else {
                                ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                            }

                            setSpan(
                                ForegroundColorSpan(uploadColor),
                                uploadStart,
                                uploadEnd,
                                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }

                        td4.text = statusText
                        td4.textSize = 12f // Make text smaller
                        td4.gravity = Gravity.CENTER_VERTICAL
                        td4.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0) // Remove drawable icons
                    }
                    else -> {
                        // Default behavior for other features
                        td1.text = "${item.tph?.blok_kode ?: ""}-${item.tph?.nomor ?: ""}"

                        td2.text = item.inspeksi.jml_pkk_inspeksi.toString()

                        val startTime = formatTime(item.inspeksi.created_date)
                        td3.text = "$startTime"

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