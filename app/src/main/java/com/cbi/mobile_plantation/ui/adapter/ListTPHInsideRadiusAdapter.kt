package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.view.panenTBS.FeaturePanenTBSActivity
import com.cbi.mobile_plantation.utils.AppUtils
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
        val tphHasBeenSelected:TextView = itemView.findViewById(R.id.tphHasBeenSelected)
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

        // Check if this TPH has been selected before and how many times
        if (tphItem.isAlreadySelected) {
            holder.tphInfoTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton))

            // Show different messages based on selection count
            if (tphItem.selectionCount > AppUtils.MAX_SELECTIONS_PER_TPH - 1) {
                // TPH has reached maximum selections
                holder.tphHasBeenSelected.text = "TPH sudah terpilih ${tphItem.selectionCount} kali (maksimal)!"
                holder.tphHasBeenSelected.visibility = View.VISIBLE

                // Disable radio button
                holder.radioButton.isEnabled = false
                holder.radioButton.alpha = 0.5f // Make it look disabled
            } else {
                // TPH has been selected but can be selected again
                holder.tphHasBeenSelected.text = "TPH sudah terpilih ${tphItem.selectionCount} kali!"
                holder.tphHasBeenSelected.visibility = View.VISIBLE

                // Radio button remains enabled
                holder.radioButton.isEnabled = true
                holder.radioButton.alpha = 1.0f
            }
        } else {
            // TPH has not been selected before
            holder.tphHasBeenSelected.visibility = View.GONE
            holder.radioButton.isEnabled = true
            holder.radioButton.alpha = 1.0f
        }

        // Only enable click listener if the TPH can be selected again
        if (tphItem.canBeSelectedAgain) {
            holder.radioButton.setOnClickListener {
                selectedPosition = position
                notifyDataSetChanged() // Refresh selection

                // Notify the activity about the selected item
                listener.onTPHSelected(tphItem)
            }

            // Make the entire item clickable as well
            holder.itemView.setOnClickListener {
                holder.radioButton.performClick()
            }
        } else {
            // Remove click listeners if TPH cannot be selected again
            holder.radioButton.setOnClickListener(null)
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount() = tphList.size
}


