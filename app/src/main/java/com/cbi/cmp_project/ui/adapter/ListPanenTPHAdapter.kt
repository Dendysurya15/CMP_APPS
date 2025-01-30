package com.cbi.cmp_project.ui.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.databinding.TableItemRowBinding
import com.cbi.cmp_project.utils.AppLogger
import java.text.SimpleDateFormat
import java.util.Locale

class ListPanenTPHAdapter : RecyclerView.Adapter<ListPanenTPHAdapter.ListPanenTPHViewHolder>() {
    private var tphList = mutableListOf<Map<String, Any>>()
    private var currentArchiveState: Int = 0
    private var areCheckboxesEnabled = true  // Add this property
    private val selectedItems = mutableSetOf<Int>()  // Track selected positions
    private var selectAllState = false

    private var onSelectionChangeListener: ((Int) -> Unit)? = null

    class ListPanenTPHViewHolder(private val binding: TableItemRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: Map<String, Any>, isSelected: Boolean, archiveState: Int, onCheckedChange: (Boolean) -> Unit, checkboxEnabled: Boolean) {



            AppLogger.d(data.toString())
            val dateStr = data["date_created"] as String
            try {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = formatter.parse(dateStr)
                val indonesiaFormatter = SimpleDateFormat("dd MMM yyyy\nHH:mm", Locale("id"))
                binding.tvItemTgl.text = indonesiaFormatter.format(date)
            } catch (e: Exception) {
                binding.tvItemTgl.text = dateStr
            }

            // Set TPH ID
            binding.tvItemTPH.text = data["tph_id"] as? String ?: "-"

            // Handle block information
            val lat = (data["lat"] as? Double)?.toString() ?: "-"
            val lon = (data["lon"] as? Double)?.toString() ?: "-"
            binding.tvItemBlok.text = "$lat\n$lon"

            if (archiveState == 1) {
                binding.checkBoxPanen.visibility = View.GONE
                binding.numListTerupload.visibility = View.VISIBLE
                binding.numListTerupload.text = "${adapterPosition + 1}."
            } else {
                binding.checkBoxPanen.visibility = View.VISIBLE
                binding.numListTerupload.visibility = View.GONE
                binding.checkBoxPanen.isChecked = isSelected
                binding.checkBoxPanen.isEnabled = checkboxEnabled
                binding.checkBoxPanen.setOnCheckedChangeListener { _, isChecked ->
                    if (checkboxEnabled) {  // Only trigger if enabled
                        onCheckedChange(isChecked)
                    }
                }
            }
        }
    }
    fun isAllSelected(): Boolean {
        return selectAllState
    }

    fun getSelectedItems(): List<Map<String, Any>> {
        return selectedItems.mapNotNull { position -> tphList.getOrNull(position) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListPanenTPHViewHolder {
        val binding = TableItemRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ListPanenTPHViewHolder(binding)
    }


    fun selectAll(select: Boolean) {
        selectAllState = select
        selectedItems.clear()
        if (select) {
            for (i in tphList.indices) {
                if (currentArchiveState != 1) {
                    selectedItems.add(i)
                }
            }
        }
        areCheckboxesEnabled = !select  // Disable checkboxes when selecting all
        Handler(Looper.getMainLooper()).post {
            notifyDataSetChanged()
            onSelectionChangeListener?.invoke(selectedItems.size)
        }
    }

    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ListPanenTPHViewHolder, position: Int) {
        holder.bind(tphList[position], selectedItems.contains(position), currentArchiveState, { isChecked ->
            if (isChecked) {
                selectedItems.add(position)
            } else {
                selectedItems.remove(position)
                selectAllState = false
            }
            onSelectionChangeListener?.invoke(selectedItems.size)
        }, areCheckboxesEnabled)  // Pass the enabled state
    }

    fun updateArchiveState(state: Int) {
        currentArchiveState = state
        notifyDataSetChanged()
    }

    override fun getItemCount() = tphList.size


    private var onSelectionChangedListener: ((Int) -> Unit)? = null

    fun setOnSelectionChangedListener(listener: (Int) -> Unit) {
        onSelectionChangeListener = listener
    }

    private fun notifySelectedItemsChanged() {
        onSelectionChangedListener?.invoke(selectedItems.size)
    }

    fun clearAll() {
        selectedItems.clear()
        tphList.clear()
        selectAllState = false
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(0)
    }

    // Make sure updateData also resets selection state
    fun updateData(newData: List<Map<String, Any>>) {
        tphList.clear()
        selectedItems.clear()
        selectAllState = false
        tphList.addAll(newData)
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(0)
    }
}