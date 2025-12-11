package com.cbi.mobile_plantation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils

data class AbsensiDataList(
    val id: Int,
    val nama: String,
    val namaOnly: String,
    val nik: String,
    val kemandoranId: Int,
    val kemandoranName: String, // Tambahkan kode kemandoran
    var alokasiKerja: String? = null,
    var keterangan: String? = null,
    var isChecked: Boolean = false
)

class AbsensiAdapter(
    private var originalItems: MutableList<AbsensiDataList> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // View type constants
    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

//    private val workLocations = listOf("Panen", "Potong Buah", "Gardan", "Supir", "Rawat Jalan", "Pruning", "Perbaikan Unit", "Jangkos", "Perawatan")

    // Mapping radio button ID to attendance status
    private val radioButtonMap = mapOf(
        R.id.rbHadir to "Hadir",
        R.id.rbMangkir to "Mangkir",
        R.id.rbSakit to "Sakit",
        R.id.rbIzin to "Izin",
        R.id.rbCuti to "Cuti",
        R.id.rbTidakAbsen to "TA"
    )

    // Reverse mapping for setting radio button
    private val statusToRadioButton = mapOf(
        "Hadir" to R.id.rbHadir,
        "H" to R.id.rbHadir,
        "Mangkir" to R.id.rbMangkir,
        "M" to R.id.rbMangkir,
        "Sakit" to R.id.rbSakit,
        "S" to R.id.rbSakit,
        "Izin" to R.id.rbIzin,
        "I" to R.id.rbIzin,
        "Cuti" to R.id.rbCuti,
        "C" to R.id.rbCuti,
        "TA" to R.id.rbTidakAbsen
    )

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
        val spinnerAlokasiKerja: Spinner = itemView.findViewById(R.id.spinnerAbsensiAlokasiKerja)
        val radioGroup: RadioGroup = itemView.findViewById(R.id.radioGroupKeterangan)
        val rbHadir: RadioButton = itemView.findViewById(R.id.rbHadir)
        val rbSakit: RadioButton = itemView.findViewById(R.id.rbSakit)
        val rbIzin: RadioButton = itemView.findViewById(R.id.rbIzin)
        val rbCuti: RadioButton = itemView.findViewById(R.id.rbCuti)
        val rbMangkir: RadioButton = itemView.findViewById(R.id.rbMangkir)
        val rbTidakAbsen: RadioButton = itemView.findViewById(R.id.rbTidakAbsen)
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

    // Function to check if kemandoran is "Panen" based on kode
    private fun isKemandoranPanen(kemandoranName: String): Boolean {
        // Hanya kemandoran yang mengandung kata "panen" (tidak hanya huruf 'P')
        return kemandoranName.contains("panen", ignoreCase = true)
    }


    // Function to update radio button visibility based on kemandoran
    private fun updateRadioButtonVisibility(holder: ItemViewHolder, isPanen: Boolean) {
        // Show/Hide radio buttons based on kemandoran type
        holder.rbHadir.visibility = View.VISIBLE // H selalu tampil

        if (isPanen) {
            // Jika Kemandoran Panen, tampilkan semua radio button
            holder.rbSakit.visibility = View.VISIBLE
            holder.rbIzin.visibility = View.VISIBLE
            holder.rbCuti.visibility = View.VISIBLE
            holder.rbMangkir.visibility = View.VISIBLE
            holder.rbTidakAbsen.visibility = View.GONE
        } else {
            // Jika bukan Kemandoran Panen, hanya tampilkan H dan TA
            holder.rbSakit.visibility = View.GONE
            holder.rbIzin.visibility = View.GONE
            holder.rbCuti.visibility = View.GONE
            holder.rbMangkir.visibility = View.GONE
            holder.rbTidakAbsen.visibility = View.VISIBLE // TA selalu tampil
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            when (val item = adapterItems[position]) {
                is AdapterItem.Header -> {
                    holder as HeaderViewHolder
                    holder.tvKemandoranName.text = item.kemandoranName
                    AppLogger.d("kemandoran nama ${item.kemandoranName}")

                    // Add top margin to all headers except the first one
                    val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
                    params.topMargin = if (position == 0) 0 else 8
                    holder.itemView.layoutParams = params
                }
                is AdapterItem.Item -> {
                    holder as ItemViewHolder
                    val absensiData = item.absensiData
                    AppLogger.d("data absen ${item.absensiData}")


                    // Bind nama
                    holder.tvNama.text = absensiData.nama

                    // Check if kemandoran is Panen
                    val isPanen = isKemandoranPanen(absensiData.kemandoranName)

                    AppLogger.d("Kemandoran Kode: ${absensiData.kemandoranName}, isPanen: $isPanen")

                    // Setup Spinner untuk Alokasi Kerja dengan custom layout
                    val spinnerAdapter = object : ArrayAdapter<String>(
                        holder.itemView.context,
                        android.R.layout.simple_spinner_item,
                        AppUtils.workLocations
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent)
                            val textView = view.findViewById<TextView>(android.R.id.text1)
                            textView.setTextColor(holder.itemView.context.getColor(R.color.black))
                            textView.textSize = 14f
                            return view
                        }

                        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getDropDownView(position, convertView, parent)
                            val textView = view.findViewById<TextView>(android.R.id.text1)
                            textView.setTextColor(holder.itemView.context.getColor(R.color.black))
                            textView.setBackgroundColor(holder.itemView.context.getColor(android.R.color.white))
                            textView.textSize = 14f
                            textView.setPadding(16, 16, 16, 16)
                            return view
                        }
                    }
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    holder.spinnerAlokasiKerja.adapter = spinnerAdapter

                    // Set popup background putih
                    holder.spinnerAlokasiKerja.setPopupBackgroundResource(android.R.color.white)

                    // Set selected item - default "Panen" jika belum ada pilihan
                    if (absensiData.alokasiKerja != null) {
                        val index = AppUtils.workLocations.indexOf(absensiData.alokasiKerja)
                        if (index >= 0) {
                            holder.spinnerAlokasiKerja.setSelection(index)
                        }
                    } else {
                        // Default ke "Panen" (index 0)
                        holder.spinnerAlokasiKerja.setSelection(0)
                        absensiData.alokasiKerja = AppUtils.workLocations[0]
                        val originalItem = originalItems.find { it.id == absensiData.id }
                        originalItem?.alokasiKerja = AppUtils.workLocations[0]
                    }

                    // Update radio button visibility based on kemandoran type
                    updateRadioButtonVisibility(holder, isPanen)

                    // Remove previous listener
                    holder.spinnerAlokasiKerja.onItemSelectedListener = null

                    // Set listener untuk spinner
                    holder.spinnerAlokasiKerja.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            val selectedItem = AppUtils.workLocations[pos]
                            val originalItem = originalItems.find { it.id == absensiData.id }
                            originalItem?.alokasiKerja = selectedItem
                            absensiData.alokasiKerja = selectedItem

                            AppLogger.d("Alokasi Kerja updated for ${absensiData.nama}: $selectedItem")
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            // Do nothing
                        }
                    }

                    // Set radio button berdasarkan keterangan yang tersimpan
                    holder.radioGroup.setOnCheckedChangeListener(null) // Remove previous listener

                    // Set radio button menggunakan mapping - default berdasarkan kemandoran type
                    if (absensiData.keterangan != null) {
                        val radioButtonId = statusToRadioButton[absensiData.keterangan]
                        if (radioButtonId != null) {
                            holder.radioGroup.check(radioButtonId)
                        } else {
                            holder.radioGroup.clearCheck()
                        }
                    } else {
                        // Default berdasarkan kemandoran type
                        if (isPanen) {
                            // Default ke "Hadir" untuk Kemandoran Panen
                            holder.radioGroup.check(R.id.rbHadir)
                            absensiData.keterangan = "Hadir"
                            val originalItem = originalItems.find { it.id == absensiData.id }
                            originalItem?.keterangan = "Hadir"
                        } else {
                            // Default ke "TA" untuk bukan Kemandoran Panen
                            holder.radioGroup.check(R.id.rbTidakAbsen)
                            absensiData.keterangan = "TA"
                            val originalItem = originalItems.find { it.id == absensiData.id }
                            originalItem?.keterangan = "TA"
                        }
                    }

                    // Set listener untuk radio group
                    holder.radioGroup.setOnCheckedChangeListener { _, checkedId ->
                        val originalItem = originalItems.find { it.id == absensiData.id }

                        // Get keterangan dari mapping
                        val keterangan = radioButtonMap[checkedId]

                        originalItem?.keterangan = keterangan
                        absensiData.keterangan = keterangan

                        AppLogger.d("Keterangan updated for ${absensiData.nama}: $keterangan")
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

    // Function to get checked items (yang sudah ada keterangan absensi)
    fun getCheckedItems(): List<AbsensiDataList> {
        return originalItems.filter { it.keterangan != null }
    }
}