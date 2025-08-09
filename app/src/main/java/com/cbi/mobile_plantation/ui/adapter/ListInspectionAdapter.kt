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
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.databinding.TableItemRowBinding
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListInspectionAdapter(
    private val featureName: String,
    private val onInspectionItemClick: (InspectionWithDetailRelations) -> Unit,
    private val onPanenItemClick: ((Any) -> Unit)? = null // Add for panen data
) : RecyclerView.Adapter<ListInspectionAdapter.BaseViewHolder>() {

    private var inspectionPaths: List<InspectionWithDetailRelations> = emptyList()
    private var panenData: List<Any> = emptyList() // Add for panen data
    private var currentArchiveState: Int = 0 // Track archive state

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<InspectionWithDetailRelations>) {
        inspectionPaths = data
        panenData = emptyList()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setPanenData(data: List<Any>) {
        panenData = data
        inspectionPaths = emptyList()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(data: List<Any>) {
        when {
            featureName == AppUtils.ListFeatureNames.TransferInspeksiPanen -> {
                panenData = data
                inspectionPaths = emptyList()
            }
            else -> {
                if (data.isNotEmpty() && data.first() is InspectionWithDetailRelations) {
                    inspectionPaths = data.filterIsInstance<InspectionWithDetailRelations>()
                    panenData = emptyList()
                }
            }
        }
        notifyDataSetChanged()
    }

    fun updateArchiveState(state: Int) {
        currentArchiveState = state
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val binding = TableItemRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return when (featureName) {
            AppUtils.ListFeatureNames.TransferInspeksiPanen -> {
                PanenDataViewHolder(binding)
            }
            else -> {
                InspectionDataViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        when (holder) {
            is InspectionDataViewHolder -> {
                if (position < inspectionPaths.size) {
                    holder.bind(inspectionPaths[position])
                }
            }
            is PanenDataViewHolder -> {
                if (position < panenData.size) {
                    holder.bind(panenData[position])
                }
            }
        }
    }

    override fun getItemCount(): Int = when (featureName) {
        AppUtils.ListFeatureNames.TransferInspeksiPanen -> panenData.size
        else -> inspectionPaths.size
    }

    abstract class BaseViewHolder(binding: TableItemRowBinding) : RecyclerView.ViewHolder(binding.root)

    inner class InspectionDataViewHolder(
        private val binding: TableItemRowBinding
    ) : BaseViewHolder(binding) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < inspectionPaths.size) {
                    onInspectionItemClick(inspectionPaths[position])
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

                        val pemulihanIcon = if (item.inspeksi.inspeksi_putaran == 2) "✓" else "✗"
                        val uploadIcon = if (item.inspeksi.status_upload == "0") "✗" else "✓"

                        val statusText = SpannableStringBuilder().apply {
                            append(pemulihan)
                            append(" ")
                            val pemulihanStart = length
                            append(pemulihanIcon)
                            val pemulihanEnd = length

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

                            append(upload)
                            append(" ")
                            val uploadStart = length
                            append(uploadIcon)
                            val uploadEnd = length

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
                        td4.textSize = 12f
                        td4.gravity = Gravity.CENTER_VERTICAL
                        td4.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    }
                    else -> {
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

    inner class PanenDataViewHolder(
        private val binding: TableItemRowBinding
    ) : BaseViewHolder(binding) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < panenData.size) {
                    onPanenItemClick?.invoke(panenData[position])
                }
            }
        }

        fun bind(item: Any) {
            binding.apply {
                flCheckBoxItemTph.visibility = View.GONE
                td1.visibility = View.VISIBLE
                td2.visibility = View.VISIBLE
                td3.visibility = View.VISIBLE
                td4.visibility = View.GONE
                td5.visibility = View.GONE

                // Cast to PanenEntityWithRelations
                if (item is PanenEntityWithRelations) {
                    val panen = item.panen
                    val tph = item.tph

                    // td1: BLOK-TPH
                    td1.text = "${tph?.blok_kode ?: ""}-${tph?.nomor ?: ""}"

                    // td2: Created Date
                    td2.text = formatDateTime(panen.date_created)

                    // td3: Tipe/Ancak
                    val tipeAncak = when (panen.jenis_panen) {
                        0 -> "NORMAL"
                        1 -> "CUT&CARRY"
                        else -> "TIDAK DIKETAHUI"
                    }
                    td3.text = "$tipeAncak\nANCAK ${panen.ancak}"

                }
            }
        }

        private fun formatDateTime(dateTime: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy\n HH:mm:ss", Locale("id", "ID"))
                val date = inputFormat.parse(dateTime)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                // Fallback if parsing fails
                try {
                    if (dateTime.contains(" ")) {
                        val datePart = dateTime.split(" ")[0] // Get just the date part
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                        val date = inputFormat.parse(datePart)
                        outputFormat.format(date ?: Date())
                    } else {
                        dateTime
                    }
                } catch (ex: Exception) {
                    dateTime
                }
            }
        }
    }
}