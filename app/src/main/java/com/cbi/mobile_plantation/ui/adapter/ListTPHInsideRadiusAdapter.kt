package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.view.panenTBS.FeaturePanenTBSActivity
import com.google.android.material.radiobutton.MaterialRadioButton

class ListTPHInsideRadiusAdapter(
    private val tphList: List<FeaturePanenTBSActivity.ScannedTPHSelectionItem>,
    private val listener: OnTPHSelectedListener
) : RecyclerView.Adapter<ListTPHInsideRadiusAdapter.ViewHolder>() {

    private var selectedPosition = -1

    // Updated interface
    interface OnTPHSelectedListener {
        fun onTPHSelected(selectedTPH: FeaturePanenTBSActivity.ScannedTPHSelectionItem)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tphInfoTextView: TextView = itemView.findViewById(R.id.tphInfoTextView)
        val radioButton: MaterialRadioButton = itemView.findViewById(R.id.rbScannedTPHInsideRadius)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_tph_inside_radius, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val tphItem = tphList[position]
        holder.tphInfoTextView.text = "TPH ${tphItem.number} - ${tphItem.blockCode} (${tphItem.distance.toInt()} m)"
        holder.radioButton.isChecked = position == selectedPosition

        holder.radioButton.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged() // Refresh selection

            // Notify the activity about the selected item
            listener.onTPHSelected(tphItem)
        }
    }

    override fun getItemCount() = tphList.size
}


