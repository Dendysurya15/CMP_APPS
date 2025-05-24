package com.cbi.mobile_plantation.ui.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppLogger
import java.text.SimpleDateFormat
import java.util.Locale

// Updated TPHItem data class with isFromESPB field
data class TPHItem(
    val tphId: String,
    val dateCreated: String,
    val jjgJson: String,
    val tphNomor: String,
    var isChecked: Boolean = false,
    var blokKode: String,
    val isFromESPB: Boolean = false // Add this field
)

// Complete adapter with visual indicator
class detailESPBListTPHAdapter(
    var tphList: MutableList<TPHItem>,
    private val onItemChecked: (TPHItem, Boolean) -> Unit
) : RecyclerView.Adapter<detailESPBListTPHAdapter.TPHViewHolder>() {

    inner class TPHViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxPanen)
        val tvBlokName: TextView = itemView.findViewById(R.id.td1)
        val tvNomorTPH: TextView = itemView.findViewById(R.id.td2)
        val tvTotalJjg: TextView = itemView.findViewById(R.id.td3)
        val tvJam: TextView = itemView.findViewById(R.id.td4)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TPHViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_item_row, parent, false)
        AppLogger.d("Creating ViewHolder for position")
        return TPHViewHolder(view)
    }

    override fun onBindViewHolder(holder: TPHViewHolder, position: Int) {
        val tphItem = tphList[position]

        AppLogger.d("Binding item at position $position: $tphItem")

        // Show relevant views and hide checkbox frame number
        holder.itemView.findViewById<TextView>(R.id.numListTerupload).visibility = View.GONE
        holder.checkBox.visibility = View.VISIBLE
        holder.tvBlokName.visibility = View.VISIBLE
        holder.tvNomorTPH.visibility = View.VISIBLE
        holder.tvTotalJjg.visibility = View.VISIBLE
        holder.tvJam.visibility = View.VISIBLE

        holder.tvBlokName.text = tphItem.blokKode
        // Set data
        holder.checkBox.isChecked = tphItem.isChecked

        // Or find it by traversing the view hierarchy
        val rowLinearLayout = (holder.itemView as ViewGroup).getChildAt(0) as LinearLayout

        if (tphItem.isFromESPB) {
            // Set background on the LinearLayout that has the border_bottom_light
            rowLinearLayout.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.graylight))
        } else {
            // Reset to the original border drawable
            rowLinearLayout.setBackgroundResource(R.drawable.border_bottom_light)
        }

        holder.tvNomorTPH.text = tphItem.tphNomor
        holder.tvTotalJjg.text = tphItem.jjgJson

        // Format date to Indonesian format
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yy\nHH:mm", Locale("id", "ID"))

            val date = inputFormat.parse(tphItem.dateCreated)
            holder.tvJam.text = if (date != null) {
                outputFormat.format(date)
            } else {
                tphItem.dateCreated
            }
        } catch (e: Exception) {
            holder.tvJam.text = tphItem.dateCreated
        }

        AppLogger.d("Data set for position $position - TPH ID: ${tphItem.tphId}, isFromESPB: ${tphItem.isFromESPB}")

        // Handle checkbox click
        holder.checkBox.setOnClickListener {
            tphItem.isChecked = holder.checkBox.isChecked
            onItemChecked(tphItem, tphItem.isChecked)

            // Log when ESPB item is unchecked
            if (tphItem.isFromESPB && !tphItem.isChecked) {
                AppLogger.d("ESPB item ${tphItem.tphId} was unchecked!")
            }
        }

        // Handle row click to toggle checkbox
        holder.itemView.setOnClickListener {
            holder.checkBox.performClick()
        }
    }

    override fun getItemCount(): Int = tphList.size

    fun updateData(newList: List<TPHItem>) {
        tphList.clear()
        tphList.addAll(newList)
        notifyDataSetChanged()
    }

    fun getCheckedItems(): List<TPHItem> {
        return tphList.filter { it.isChecked }
    }

    fun getESPBItems(): List<TPHItem> {
        return tphList.filter { it.isFromESPB }
    }

    fun getAvailableItems(): List<TPHItem> {
        return tphList.filter { !it.isFromESPB }
    }
}
