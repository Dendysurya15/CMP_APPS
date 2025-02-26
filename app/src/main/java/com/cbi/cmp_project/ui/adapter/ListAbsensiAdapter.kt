package com.cbi.cmp_project.ui.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.databinding.TableItemRowAbsensiBinding

class ListAbsensiAdapter: RecyclerView.Adapter<ListAbsensiAdapter.ListAbsensiViewHolder>() {

    enum class SortField {
        NAMA,
        JABATAN
    }

    private var currentSortField: SortField = SortField.NAMA
    private var namaKaryawanList = mutableListOf<Map<String, Any>>()
    private var filteredList = mutableListOf<Map<String, Any>>()
    private var currentArchiveState: Int = 0
    private var areCheckboxesEnabled = true
    private val selectedItems = mutableSetOf<Int>()
    private var isSortAscending: Boolean? = null // null means no sorting applied
    private var selectAllState = false

    private var onSelectionChangeListener: ((Int) -> Unit)? = null

    data class ExtractedData(
        val namaText: String,
        val jabatanText: String,
        val searchableText: String,
    )

    fun extractData(item: Map<String, Any>): ExtractedData {
        val namaKaryawan = item["nama_karyawan"] as? String?: "-"
        val jabatanKaryawan = item["jabatan_karyawan"] as? String?: "-"

        val namaText = "$namaKaryawan"
        val jabatanText = "$jabatanKaryawan"
        val searchableText = "$namaKaryawan $jabatanKaryawan"

        return ExtractedData(namaText, jabatanText, searchableText)
    }

    fun filterData(query: String) {
        filteredList = if (query.isEmpty()) {
            namaKaryawanList.toMutableList()
        } else {
            namaKaryawanList.filter { item ->
                val extractedData = extractData(item)
                extractedData.searchableText.contains(query, ignoreCase = true)
            }.toMutableList()
        }

        isSortAscending?.let { ascending ->
            sortData(currentSortField, ascending)
        }

        selectedItems.clear()
        selectAllState = false
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(0)
    }

    fun sortData(field: SortField, ascending: Boolean) {
        currentSortField = field
        isSortAscending = ascending

        filteredList = filteredList.sortedWith(compareBy<Map<String, Any>> { item ->
            val extractedData = extractData(item)
            when (field) {
                SortField.NAMA -> extractedData.namaText
                SortField.JABATAN -> extractedData.jabatanText
            }
        }.let { comparator ->
            if (!ascending) comparator.reversed() else comparator
        }).toMutableList()

        notifyDataSetChanged()
    }

    fun resetSort() {
        isSortAscending = null
        currentSortField = SortField.NAMA
        filteredList = namaKaryawanList.toMutableList()
        notifyDataSetChanged()
    }

    class ListAbsensiViewHolder(private val binding: TableItemRowAbsensiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            data: Map<String, Any>,
            isSelected: Boolean,
            archiveState: Int,
            onCheckedChange: (Boolean) -> Unit,
//            checkboxEnabled: Boolean,
            extractData: (Map<String, Any>) -> ExtractedData
        ) {
            val extractedData = extractData(data)
            binding.tvItemNama.text = extractedData.namaText
            binding.tvItemJabatan.text = extractedData.jabatanText

            if (archiveState == 1) {
                binding.checkBoxAbsensi.visibility = View.GONE
//                binding.numListTersimpan.visibility = View.VISIBLE
//                binding.numListTersimpan.text = "${adapterPosition + 1}."
            } else {
                binding.checkBoxAbsensi.visibility = View.VISIBLE
//                binding.numListTersimpan.visibility = View.GONE
                binding.checkBoxAbsensi.isChecked = isSelected
                binding.checkBoxAbsensi.setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange(isChecked)
                }
            }
        }
    }

    fun getCurrentData(): List<Map<String, Any>> {
        return filteredList.toList()
    }

    fun isAllSelected(): Boolean {
        return selectAllState
    }

    fun getSelectedItems(): List<Map<String, Any>> {
        return selectedItems.mapNotNull { position -> namaKaryawanList.getOrNull(position) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListAbsensiViewHolder {
        val binding = TableItemRowAbsensiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ListAbsensiViewHolder(binding)
    }

    fun selectAll(select: Boolean) {
        selectAllState = select
        selectedItems.clear()
        if (select) {
            for (i in namaKaryawanList.indices) {
                if (currentArchiveState != 1) {
                    selectedItems.add(i)
                }
            }
        }
        // Remove this line
        // areCheckboxesEnabled = !select  // Disable checkboxes when selecting all
        Handler(Looper.getMainLooper()).post {
            notifyDataSetChanged()
            onSelectionChangeListener?.invoke(selectedItems.size)
        }
    }

    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ListAbsensiViewHolder, position: Int) {
        holder.bind(
            filteredList[position],
            selectedItems.contains(position),
            currentArchiveState,
            { isChecked ->
                if (isChecked) {
                    selectedItems.add(position)
                } else {
                    selectedItems.remove(position)
                    selectAllState = false
                }
                onSelectionChangeListener?.invoke(selectedItems.size)
            },
            extractData = ::extractData // Pass the function reference
        )
    }

    fun updateArchiveState(state: Int) {
        currentArchiveState = state
        notifyDataSetChanged()
    }

    override fun getItemCount() = filteredList.size

    private var onSelectionChangedListener: ((Int) -> Unit)? = null

    fun setOnSelectionChangedListener(listener: (Int) -> Unit) {
        onSelectionChangeListener = listener
    }

    private fun notifySelectedItemsChanged() {
        onSelectionChangedListener?.invoke(selectedItems.size)
    }

    fun clearAll() {
        selectedItems.clear()
        namaKaryawanList.clear()
        selectAllState = false
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(0)
    }

    fun updateData(newData: List<Map<String, Any>>) {
        namaKaryawanList.clear()
        selectedItems.clear()
        selectAllState = false
        isSortAscending = null  // Add this line
        namaKaryawanList.addAll(newData)
        filteredList = namaKaryawanList.toMutableList()
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(0)
    }
}