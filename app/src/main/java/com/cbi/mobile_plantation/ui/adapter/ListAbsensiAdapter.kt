package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import java.text.SimpleDateFormat
import java.util.Locale

data class AbsensiDataRekap(
    val id: Int,
    val afdeling: String,
    val datetime: String,
    val karyawan_msk_id: String,
    val karyawan_tdk_msk_id: String,
)

class ListAbsensiAdapter(private var items: List<AbsensiDataRekap>):
    RecyclerView.Adapter<ListAbsensiAdapter.ListAbsensiViewHolder>() {

        private  val selectedItems = mutableSetOf<AbsensiDataRekap>()

    class ListAbsensiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val td1: TextView = view.findViewById(R.id.td1ListAbsensi)
        val td2: TextView = view.findViewById(R.id.td2ListAbsensi)
        val td3: TextView = view.findViewById(R.id.td3ListAbsensi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListAbsensiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_item_row_list_absensi, parent, false)
        return ListAbsensiViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ListAbsensiViewHolder, position: Int) {
        val item = items[position]
        holder.td1.visibility = View.VISIBLE
        holder.td2.visibility = View.VISIBLE
        holder.td3.visibility = View.VISIBLE

        val jmlhKaryawanMsk = if (item.karyawan_msk_id.isNotEmpty()) item.karyawan_msk_id.split(",").size else 0
        val jmlhKaryawanTdkMsk = if (item.karyawan_tdk_msk_id.isNotEmpty()) item.karyawan_tdk_msk_id.split(",").size else 0


        holder.td1.text = formatToIndonesianDateTime(item.datetime)
        holder.td2.text = item.afdeling
        holder.td3.text = "$jmlhKaryawanMsk orang"
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<AbsensiDataRekap>) {
        items = newList
        selectedItems.clear() // Clear selection on update
        notifyDataSetChanged()
    }

    fun formatToIndonesianDateTime(dateTimeStr: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateTimeStr) ?: return dateTimeStr
            val outputFormat = SimpleDateFormat("d MMM yy\nHH:mm", Locale("id"))
            return outputFormat.format(date)
        } catch (e: Exception) {
            return dateTimeStr
        }
    }

    fun getSelectedItemsIdLocal(): List<Map<String, Any>> {
        return selectedItems.map { selectedItem ->
            mapOf(
                "id" to (selectedItem.id ?: "")
            )
        }
    }

    fun getSelectedItemsForUpload(): List<Map<String, Any>> {
        return selectedItems.map { selectedItem ->
            mapOf(
                "id" to (selectedItem.id ?: ""),
                "divisi" to (selectedItem.afdeling ?: ""),
                "karyawan_msk" to (selectedItem.karyawan_msk_id ?: ""),
            )
        }
    }

//    fun selectAllItems(selectAll: Boolean) {
//        if (selectAll) {
//            selectedItems.addAll(items.filter { it.status_cmp != 1 || it.status_ppro != 1 })
//        } else {
//            selectedItems.clear()
//        }
//        notifyDataSetChanged()
//    }


    override fun getItemCount() = items.size

//    enum class SortField {
//        NAMA,
//        JABATAN
//    }
//
//    private var currentSortField: SortField = SortField.NAMA
//    private var namaKaryawanList = mutableListOf<Map<String, Any>>()
//    private var filteredList = mutableListOf<Map<String, Any>>()
//    private var currentArchiveState: Int = 0
//    private var areCheckboxesEnabled = true
//    private var isSortAscending: Boolean? = null // null means no sorting applied
//    private var selectAllState = false
//
//    private var onSelectionChangeListener: ((Int) -> Unit)? = null

//    data class ExtractedData(
//        val namaText: String,
//        val jabatanText: String,
//        val searchableText: String,
//    )
//
//    fun extractData(item: Map<String, Any>): ExtractedData {
//        val namaKaryawan = item["nama_karyawan"] as? String?: "-"
//        val jabatanKaryawan = item["jabatan_karyawan"] as? String?: "-"
//
//        val namaText = "$namaKaryawan"
//        val jabatanText = "$jabatanKaryawan"
//        val searchableText = "$namaKaryawan $jabatanKaryawan"
//
//        return ExtractedData(namaText, jabatanText, searchableText)
//    }
//
//    fun filterData(query: String) {
//        filteredList = if (query.isEmpty()) {
//            namaKaryawanList.toMutableList()
//        } else {
//            namaKaryawanList.filter { item ->
//                val extractedData = extractData(item)
//                extractedData.searchableText.contains(query, ignoreCase = true)
//            }.toMutableList()
//        }
//
//        isSortAscending?.let { ascending ->
//            sortData(currentSortField, ascending)
//        }
//
//        selectedItems.clear()
//        selectAllState = false
//        notifyDataSetChanged()
//        onSelectionChangeListener?.invoke(0)
//    }
//
//    fun sortData(field: SortField, ascending: Boolean) {
//        currentSortField = field
//        isSortAscending = ascending
//
//        filteredList = filteredList.sortedWith(compareBy<Map<String, Any>> { item ->
//            val extractedData = extractData(item)
//            when (field) {
//                SortField.NAMA -> extractedData.namaText
//                SortField.JABATAN -> extractedData.jabatanText
//            }
//        }.let { comparator ->
//            if (!ascending) comparator.reversed() else comparator
//        }).toMutableList()
//
//        notifyDataSetChanged()
//    }
//
//    fun resetSort() {
//        isSortAscending = null
//        currentSortField = SortField.NAMA
//        filteredList = namaKaryawanList.toMutableList()
//        notifyDataSetChanged()
//    }

//    class ListAbsensiViewHolder(private val binding: TableItemRowAbsensiBinding) : RecyclerView.ViewHolder(binding.root) {
//        fun bind(
//            data: Map<String, Any>,
//            isSelected: Boolean,
//            archiveState: Int,
//            onCheckedChange: (Boolean) -> Unit,
////            checkboxEnabled: Boolean,
//            extractData: (Map<String, Any>) -> ExtractedData
//        ) {
//            val extractedData = extractData(data)
//            binding.tvItemNama.text = extractedData.namaText
//            binding.tvItemJabatan.text = extractedData.jabatanText
//
//            if (archiveState == 1) {
//                binding.checkBoxAbsensi.visibility = View.GONE
////                binding.numListTersimpan.visibility = View.VISIBLE
////                binding.numListTersimpan.text = "${adapterPosition + 1}."
//            } else {
//                binding.checkBoxAbsensi.visibility = View.VISIBLE
////                binding.numListTersimpan.visibility = View.GONE
//                binding.checkBoxAbsensi.isChecked = isSelected
//                binding.checkBoxAbsensi.setOnCheckedChangeListener { _, isChecked ->
//                    onCheckedChange(isChecked)
//                }
//            }
//        }
//    }

//    fun getCurrentData(): List<Map<String, Any>> {
//        return filteredList.toList()
//    }
//
//    fun isAllSelected(): Boolean {
//        return selectAllState
//    }
//
//    fun getSelectedItems(): List<Map<String, Any>> {
//        return selectedItems.mapNotNull { position -> namaKaryawanList.getOrNull(position) }
//    }
//
//    fun clearSelections() {
//        selectedItems.clear()
//        notifyDataSetChanged()
//    }
//
//
//
//    fun updateArchiveState(state: Int) {
//        currentArchiveState = state
//        notifyDataSetChanged()
//    }
//
//    private var onSelectionChangedListener: ((Int) -> Unit)? = null
//
//    fun setOnSelectionChangedListener(listener: (Int) -> Unit) {
//        onSelectionChangeListener = listener
//    }
//
//    private fun notifySelectedItemsChanged() {
//        onSelectionChangedListener?.invoke(selectedItems.size)
//    }
//
//    fun clearAll() {
//        selectedItems.clear()
//        namaKaryawanList.clear()
//        selectAllState = false
//        notifyDataSetChanged()
//        onSelectionChangeListener?.invoke(0)
//    }
//
//    fun updateData(newData: List<Map<String, Any>>) {
//        namaKaryawanList.clear()
//        selectedItems.clear()
//        selectAllState = false
//        isSortAscending = null  // Add this line
//        namaKaryawanList.addAll(newData)
//        filteredList = namaKaryawanList.toMutableList()
//        notifyDataSetChanged()
//        onSelectionChangeListener?.invoke(0)
//    }
}