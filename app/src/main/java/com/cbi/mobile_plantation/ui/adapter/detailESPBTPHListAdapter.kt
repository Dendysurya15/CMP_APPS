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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    val isFromESPB: Boolean = false,
    val nomorPemanen: String = "0"
)

// Complete adapter with visual indicator
class detailESPBListTPHAdapter(
    var tphList: MutableList<TPHItem>,
    private val onItemChecked: (TPHItem, Boolean) -> Unit
) : RecyclerView.Adapter<detailESPBListTPHAdapter.TPHViewHolder>() {

    // LiveData to track when all items are bound
    private val _isLoadingComplete = MutableLiveData<Boolean>()
    val isLoadingComplete: LiveData<Boolean> = _isLoadingComplete

    private var boundItemsCount = 0
    private val totalItemCount get() = itemCount

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
        return TPHViewHolder(view)
    }

    override fun onBindViewHolder(holder: TPHViewHolder, position: Int) {
        val tphItem = tphList[position]

        // Reduced logging for performance
        if (position % 10 == 0 || position == 0) {
            AppLogger.d("Binding item at position $position")
        }

        // Show relevant views and hide checkbox frame number
        holder.itemView.findViewById<TextView>(R.id.numListTerupload).visibility = View.GONE
        holder.checkBox.visibility = View.VISIBLE
        holder.tvBlokName.visibility = View.VISIBLE
        holder.tvNomorTPH.visibility = View.VISIBLE
        holder.tvTotalJjg.visibility = View.VISIBLE
        holder.tvJam.visibility = View.VISIBLE

        // Set data
        holder.checkBox.isChecked = tphItem.isChecked

        // Get the row container for background color
        val rowContainer = (holder.itemView as ViewGroup).getChildAt(0) as LinearLayout

        holder.tvBlokName.text = tphItem.blokKode
        if (tphItem.isFromESPB) {
            rowContainer.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.graylight))
        } else {
            rowContainer.setBackgroundResource(R.drawable.border_bottom_light)
        }

        holder.tvNomorTPH.text = tphItem.tphNomor
        holder.tvTotalJjg.text = tphItem.jjgJson

        // Format date
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

        // Handle checkbox click
        holder.checkBox.setOnClickListener {
            tphItem.isChecked = holder.checkBox.isChecked
            onItemChecked(tphItem, tphItem.isChecked)
        }

        holder.itemView.setOnClickListener {
            holder.checkBox.performClick()
        }

        // *** TRACK BINDING PROGRESS ***
        boundItemsCount++

        // Check if all items are bound
        if (boundItemsCount >= totalItemCount) {
            AppLogger.d("All items bound ($boundItemsCount/$totalItemCount) - posting loading complete")
            _isLoadingComplete.postValue(true)
        }

        // Log progress for large lists
        if (position % 10 == 0 || position == totalItemCount - 1) {
            AppLogger.d("Binding progress: ${boundItemsCount}/$totalItemCount")
        }
    }

    override fun getItemCount(): Int = tphList.size

    fun updateData(newList: List<TPHItem>) {
        // Reset counter when data changes
        boundItemsCount = 0
        _isLoadingComplete.postValue(false)

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

    // Reset loading state manually if needed
    fun resetLoadingState() {
        boundItemsCount = 0
        _isLoadingComplete.postValue(false)
    }
}
