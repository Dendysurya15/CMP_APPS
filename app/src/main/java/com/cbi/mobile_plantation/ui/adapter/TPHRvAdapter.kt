package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.TphRvData
import com.cbi.mobile_plantation.utils.AppUtils

class TPHRvAdapter(
    private var items: List<TphRvData>,
    private val featureName: String? = null
) : RecyclerView.Adapter<TPHRvAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val td1: TextView = view.findViewById(R.id.td1)
        val td2: TextView = view.findViewById(R.id.td2)
        val td3: TextView = view.findViewById(R.id.td3)
        val td4: TextView = view.findViewById(R.id.td4)
        val flCheckBoxItemTph: FrameLayout = view.findViewById(R.id.flCheckBoxItemTph)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_item_row, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Customize display based on featureName
        when (featureName) {
            AppUtils.ListFeatureNames.ScanPanenMPanen -> {
                // Custom display for ScanPanenMPanen
                holder.td1.visibility = View.VISIBLE
                holder.td2.visibility = View.VISIBLE
                holder.td3.visibility = View.VISIBLE
                holder.td4.visibility = View.VISIBLE

                holder.td1.text = item.namaBlok // Blok-TPH
                holder.td2.text = item.noTPH    // NIK
                holder.td3.text = item.time     // Jam
                holder.td4.text = item.jjg      // Just show JJG count
            }
            AppUtils.ListFeatureNames.ScanTransferInspeksiPanen -> {
                // Custom display for ScanTransferInspeksiPanen - only 3 columns
                holder.td1.visibility = View.VISIBLE
                holder.td2.visibility = View.VISIBLE
                holder.td3.visibility = View.VISIBLE
                holder.td4.visibility = View.GONE // Hide 4th column

                holder.td1.text = "${item.namaBlok}-${item.noTPH}" // BLOK-TPH (E019A-70)
                holder.td2.text = AppUtils.formatToIndonesianDate(item.time)                      // TANGGAL (full datetime)
                holder.td3.text = "${item.tipePanen}/\nANCAK ${item.ancak}" // TIPE_PANEN/ANCAK (NORMAL/16)
            }
            else -> {
                // Default display (ScanHasilPanen)
                holder.td1.visibility = View.VISIBLE
                holder.td2.visibility = View.VISIBLE
                holder.td3.visibility = View.VISIBLE
                holder.td4.visibility = View.VISIBLE

                holder.td1.text = item.namaBlok
                holder.td2.text = "${item.noTPH}/${item.jjg}"
                holder.td3.text = item.time
                holder.td4.text = item.username
            }
        }

        holder.flCheckBoxItemTph.visibility = View.GONE
    }

    fun updateList(newList: List<TphRvData>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
}