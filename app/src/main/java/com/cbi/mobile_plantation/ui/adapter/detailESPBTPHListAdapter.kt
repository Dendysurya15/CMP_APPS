package com.cbi.mobile_plantation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppLogger

data class TPHItem(
    val tphId: String,           // panenWithRelations.panen.tph_id
    val dateCreated: String,     // panenWithRelations.panen.date_created
    val jjgJson: String,         // panenWithRelations.panen.jjg_json
    val tphNomor: String,        // panenWithRelations.tph.nomor
    var isChecked: Boolean = false,

    // Keep your existing fields for compatibility if needed
    val kpValue: String = "",
    val status: String = ""
)

class detailESPBListTPHAdapter(
    var tphList: MutableList<TPHItem>,
    private val onItemChecked: (TPHItem, Boolean) -> Unit
) : RecyclerView.Adapter<detailESPBListTPHAdapter.TPHViewHolder>() {

    inner class TPHViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxPanen)
        val tvTphId: TextView = itemView.findViewById(R.id.td1)
        val tvDate: TextView = itemView.findViewById(R.id.td2)
        val tvKpValue: TextView = itemView.findViewById(R.id.td3)
        val tvStatus: TextView = itemView.findViewById(R.id.td4)
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
        holder.tvTphId.visibility = View.VISIBLE
        holder.tvDate.visibility = View.VISIBLE
        holder.tvKpValue.visibility = View.VISIBLE
        holder.tvStatus.visibility = View.VISIBLE

        // Set data
        holder.checkBox.isChecked = tphItem.isChecked
        holder.tvTphId.text = tphItem.tphId
        holder.tvDate.text = tphItem.dateCreated
        holder.tvKpValue.text = tphItem.kpValue
        holder.tvStatus.text = tphItem.status

        AppLogger.d("Data set for position $position - TPH ID: ${tphItem.tphId}")

        // Handle checkbox click
        holder.checkBox.setOnClickListener {
            tphItem.isChecked = holder.checkBox.isChecked
            onItemChecked(tphItem, tphItem.isChecked)
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
}