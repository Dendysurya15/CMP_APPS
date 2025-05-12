package com.cbi.mobile_plantation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppLogger

data class AbsensiDataList(
    val id: Int,
    val nama: String,
    val jabatan: String,
    val kemandoranId: Int, // Add this field
    var isChecked: Boolean = false
)

class AbsensiAdapter(
    private var originalItems: MutableList<AbsensiDataList> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // View type constants
    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    // Map to store kemandoran names
    private val kemandoranNames = mutableMapOf<Int, String>()

    // Flattened list that contains both headers and items
    private var adapterItems: List<AdapterItem> = listOf()

    // Sealed class to represent our adapter items (either Header or Item)
    private sealed class AdapterItem {
        data class Header(val kemandoranId: Int, val kemandoranName: String) : AdapterItem()
        data class Item(val absensiData: AbsensiDataList) : AdapterItem()
    }

    // ViewHolder for header
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvKemandoranName: TextView = itemView.findViewById(R.id.tvKemandoranName)
    }

    // ViewHolder for items
    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNama: TextView = itemView.findViewById(R.id.tvItemNama)
        val tvJabatan: TextView = itemView.findViewById(R.id.tvItemJabatan)
        val flCheckbox: CheckBox = itemView.findViewById(R.id.checkBoxAbsensi)
    }

    init {
        // Initial processing of the items
        updateAdapterItems()
    }

    // This function converts our original list into a flattened list with headers
    private fun updateAdapterItems() {
        val result = mutableListOf<AdapterItem>()

        // Group items by kemandoranId
        val groupedItems = originalItems.groupBy { it.kemandoranId }

        // For each kemandoran group, add a header followed by its items
        groupedItems.forEach { (kemandoranId, items) ->
            // Add header
            val headerName = kemandoranNames[kemandoranId] ?: "Kemandoran $kemandoranId"
            result.add(AdapterItem.Header(kemandoranId, headerName))

            // Add all items for this kemandoran
            items.forEach { item ->
                result.add(AdapterItem.Item(item))
            }
        }

        adapterItems = result
    }

    override fun getItemViewType(position: Int): Int {
        return when (adapterItems[position]) {
            is AdapterItem.Header -> VIEW_TYPE_HEADER
            is AdapterItem.Item -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.table_item_header_absensi, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.table_item_row_absensi, parent, false)
                ItemViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            when (val item = adapterItems[position]) {
                is AdapterItem.Header -> {
                    holder as HeaderViewHolder
                    holder.tvKemandoranName.text = item.kemandoranName

                    // Add top margin to all headers except the first one
                    val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
                    params.topMargin = if (position == 0) 0 else 8 // 16dp margin for non-first headers
                    holder.itemView.layoutParams = params
                }
                is AdapterItem.Item -> {
                    holder as ItemViewHolder
                    val absensiData = item.absensiData

                    // Bind data to ViewHolder
                    holder.tvNama.text = absensiData.nama
                    holder.tvJabatan.text = absensiData.jabatan
                    holder.flCheckbox.setOnCheckedChangeListener(null) // Remove previous listener
                    holder.flCheckbox.isChecked = absensiData.isChecked

                    holder.flCheckbox.setOnCheckedChangeListener { _, isChecked ->
                        // Update the isChecked value in our original list
                        val originalItem = originalItems.find { it.id == absensiData.id }
                        originalItem?.isChecked = isChecked
                        // Also update in our adapter item to keep both in sync
                        absensiData.isChecked = isChecked
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error in onBindViewHolder: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = adapterItems.size

    // Function to remove workers by kemandoranId
    fun removeWorkerById(kemandoranId: String) {
        AppLogger.d("Removing all karyawan for Kemandoran ID: $kemandoranId")

        val sizeBeforeRemoval = originalItems.size
        originalItems.removeAll { it.kemandoranId.toString() == kemandoranId }
        val removedCount = sizeBeforeRemoval - originalItems.size

        // Remove kemandoran name
        kemandoranNames.remove(kemandoranId.toIntOrNull())

        // Update our flattened list
        updateAdapterItems()

        AppLogger.d("Removed $removedCount karyawan for Kemandoran ID: $kemandoranId")
        notifyDataSetChanged()
    }

    // Function to update the list with new items
    fun updateList(newList: List<AbsensiDataList>, append: Boolean = true, kemandoranName: String? = null) {
        if (newList.isNotEmpty() && kemandoranName != null) {
            // Store the kemandoran name
            val kemandoranId = newList[0].kemandoranId
            kemandoranNames[kemandoranId] = kemandoranName
        }

        originalItems = if (append) {
            (originalItems + newList).distinctBy { it.id }.toMutableList()
        } else {
            newList.toMutableList()
        }

        // Sort by kemandoranId to group them
        originalItems.sortBy { it.kemandoranId }

        // Update our flattened list
        updateAdapterItems()

        notifyDataSetChanged()
    }

    // Function to clear the list
    fun clearList() {
        originalItems.clear()
        kemandoranNames.clear()
        updateAdapterItems()
        notifyDataSetChanged()
    }

    // Function to get all items
    fun getItems(): List<AbsensiDataList> {
        return originalItems
    }
}