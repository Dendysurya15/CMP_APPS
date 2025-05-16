package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.adapter.WeighBridgeAdapter.Info
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.PrefManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Locale

data class AbsensiDataRekap(
    val id: Int,
    val afdeling: String,
    val datetime: String,
    val kemandoran: String,
    val karyawan_msk_id: String,
    val karyawan_tdk_msk_id: String,
    val karyawan_msk_nama: String,
    val karyawan_tdk_msk_nama: String,
    val karyawan_msk_nik: String,
    val karyawan_tdk_msk_nik: String,
)

class ListAbsensiAdapter(private val context: Context,
                         private var items: List<AbsensiDataRekap>):
    RecyclerView.Adapter<ListAbsensiAdapter.ListAbsensiViewHolder>() {

        private  val selectedItems = mutableSetOf<AbsensiDataRekap>()
    private var currentArchiveState: Int = 0
    private var selectionMode = false
    private var selectAllState = false
    private var onSelectionChangeListener: ((Int) -> Unit)? = null
    private var prefManager: PrefManager? = null
    private var userName: String? = null

    class ListAbsensiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val td1: TextView = view.findViewById(R.id.td1ListAbsensi)
        val td2: TextView = view.findViewById(R.id.td2ListAbsensi)
        val td3: TextView = view.findViewById(R.id.td3ListAbsensi)
        val checkBox: CheckBox = view.findViewById(R.id.checkBoxItemAbsensi)
        val flCheckBox: FrameLayout = view.findViewById(R.id.flCheckBoxItemAbsensi)
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
        prefManager = PrefManager(context)
        userName = prefManager!!.nameUserLogin

        val jmlhKaryawanMskDetail = if (item.karyawan_msk_id.isNotEmpty()) item.karyawan_msk_id.split(",").size else 0
        val jmlhKaryawanTdkMskDetail = if (item.karyawan_tdk_msk_id.isNotEmpty()) item.karyawan_tdk_msk_id.split(",").size else 0

        // Handle item clicks for both bottom sheet and selection mode
        holder.itemView.setOnClickListener {
            if (selectionMode) {
                // When in selection mode, toggle the checkbox
                holder.checkBox.isChecked = !holder.checkBox.isChecked
                toggleSelection(item)
            } else {
                // When not in selection mode, show the bottom sheet
                showBottomSheetDialog(holder, item, jmlhKaryawanMskDetail, jmlhKaryawanTdkMskDetail)
            }
        }

        val jmlhKaryawanMsk = if (item.karyawan_msk_id.isNotEmpty()) item.karyawan_msk_id.split(",").size else 0
        val jmlhKaryawanTdkMsk = if (item.karyawan_tdk_msk_id.isNotEmpty()) item.karyawan_tdk_msk_id.split(",").size else 0

//        holder.td1.text = formatToIndonesianDateTime(item.datetime)
        holder.td1.text = item.afdeling
        holder.td2.text = item.kemandoran
        holder.td3.text = "$jmlhKaryawanMsk orang"

        // Control checkbox visibility based on selection mode
//        holder.flCheckBox.visibility = if (selectionMode) View.VISIBLE else View.GONE
        // In onBindViewHolder
        holder.flCheckBox.visibility = View.VISIBLE // Always visible
        holder.checkBox.isChecked = selectedItems.contains(item)

        // Handle checkbox clicks
        holder.checkBox.setOnClickListener {
            toggleSelection(item)
        }

        // Handle item clicks to toggle selection when in selection mode
//        holder.itemView.setOnClickListener {
//            if (selectionMode) {
//                holder.checkBox.isChecked = !holder.checkBox.isChecked
//                toggleSelection(item)
//            }
//        }

        // Allow long click to enter selection mode
        holder.itemView.setOnLongClickListener {
            if (!selectionMode) {
                enableSelectionMode()
                toggleSelection(item)
                holder.checkBox.isChecked = true
            }
            true
        }
    }

    // Create a separate method for showing the bottom sheet dialog
    private fun showBottomSheetDialog(holder: ListAbsensiViewHolder, item: AbsensiDataRekap,
                                      jmlhKaryawanMskDetail: Int, jmlhKaryawanTdkMskDetail: Int) {
        AppLogger.d("testTampil")
        val context = holder.itemView.context
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context)
            .inflate(R.layout.layout_bottom_sheet_detail_table_absensi, null)

        view.findViewById<TextView>(R.id.titleDialogDetailTableAbsensi).text =
            "Detail Kehadiran ${userName}"

        view.findViewById<android.widget.Button>(R.id.btnCloseDetailTableAbsensi).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)

        val maxHeight = (context.resources.displayMetrics.heightPixels * 0.85).toInt()

        // Format karyawan masuk with NIK and name
        val formattedKaryawanMsk = if (item.karyawan_msk_nama.isNotEmpty() && item.karyawan_msk_nik.isNotEmpty()) {
            val nikList = item.karyawan_msk_nik.split(",")
            val namaList = item.karyawan_msk_nama.split(",")
            val combinedList = mutableListOf<String>()

            // Combine NIK and name with line breaks
            val minSize = minOf(nikList.size, namaList.size)
            for (i in 0 until minSize) {
                combinedList.add("${nikList[i].trim()} - ${namaList[i].trim()}")
            }

            combinedList.joinToString("\n")
        } else {
            item.karyawan_msk_nama
        }

        // Format karyawan tidak masuk with NIK and name
        val formattedKaryawanTdkMsk = if (item.karyawan_tdk_msk_nama.isNotEmpty() && item.karyawan_tdk_msk_nik.isNotEmpty()) {
            val nikList = item.karyawan_tdk_msk_nik.split(",")
            val namaList = item.karyawan_tdk_msk_nama.split(",")
            val combinedList = mutableListOf<String>()

            // Combine NIK and name with line breaks
            val minSize = minOf(nikList.size, namaList.size)
            for (i in 0 until minSize) {
                combinedList.add("${nikList[i].trim()} - ${namaList[i].trim()}")
            }

            combinedList.joinToString("\n")
        } else {
            item.karyawan_tdk_msk_nama
        }

        val infoItems = listOf(
            InfoAbsensi.DATE to item.datetime,
            InfoAbsensi.AFDELING to item.afdeling,
            InfoAbsensi.KEMANDORAN to item.kemandoran,
            InfoAbsensi.KARYAWANMSK to formattedKaryawanMsk,
            InfoAbsensi.KARYAWANTDKMSK to formattedKaryawanTdkMsk,
            InfoAbsensi.TTLKEHADIRAN to "Masuk: $jmlhKaryawanMskDetail orang\n  Tidak Masuk: $jmlhKaryawanTdkMskDetail orang"
        )

        infoItems.forEach { (type, value) ->
            val itemView = view.findViewById<View>(type.id)

            if (itemView != null) {
                if (value != null) {
                    setInfoItemValues(itemView, type.label, value)
                }
            }
        }

        // Show the dialog before configuring the bottom sheet
        bottomSheetDialog.show()

        bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { bottomSheet ->
                val behavior = BottomSheetBehavior.from(bottomSheet)

                behavior.apply {
                    this.peekHeight = maxHeight
                    this.state = BottomSheetBehavior.STATE_EXPANDED
                    this.isFitToContents = true
                    this.isDraggable = false
                }

                bottomSheet.layoutParams?.height = maxHeight
            }
    }

    private fun setInfoItemValues(view: View, label: String, value: String) {
        view.findViewById<TextView>(R.id.tvLabel)?.text = label

        view.findViewById<TextView>(R.id.tvValue)?.text = when (view.id) {
//            R.id.infoKemandoranAbsensi -> value
            else -> ": $value"
        }
    }

    private fun toggleSelection(item: AbsensiDataRekap) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }
        onSelectionChangeListener?.invoke(selectedItems.size)
    }

    fun enableSelectionMode() {
        if (!selectionMode) {
            selectionMode = true
            notifyDataSetChanged()
            onSelectionChangeListener?.invoke(selectedItems.size)
        }
    }

    fun disableSelectionMode() {
        if (selectionMode) {
            selectionMode = false
            selectedItems.clear()
            notifyDataSetChanged()
            onSelectionChangeListener?.invoke(0)
        }
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(items)
        selectAllState = true
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(selectedItems.size)
    }

    fun deselectAll() {
        selectedItems.clear()
        selectAllState = false
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(0)
    }

    fun toggleSelectAll() {
        if (selectAllState) {
            deselectAll()
        } else {
            selectAll()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<AbsensiDataRekap>) {
        items = newList
        selectedItems.clear() // Clear selection on update
        notifyDataSetChanged()
    }

    fun getSelectedItems(): Set<AbsensiDataRekap> {
        return selectedItems
    }

    fun getSelectedItemsIdLocal(): List<Map<String, Any>> {
        return selectedItems.map { selectedItem ->
            mapOf(
                "id" to (selectedItem.id)
            )
        }
    }

    fun getSelectedItemsForUpload(): List<Map<String, Any>> {
        return selectedItems.map { selectedItem ->
            mapOf(
                "id" to (selectedItem.id),
                "divisi" to (selectedItem.afdeling),
                "karyawan_msk" to (selectedItem.karyawan_msk_id),
            )
        }
    }

    // Add this method to your ListAbsensiAdapter class
    fun clearSelections() {
        selectedItems.clear()
        selectAllState = false
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(0)
    }

    fun setOnSelectionChangeListener(listener: (Int) -> Unit) {
        onSelectionChangeListener = listener
    }

    enum class InfoAbsensi(val id: Int, val label: String) {
        AFDELING(R.id.infoEstAfdAbsensi, "Afdeling"),
        DATE(R.id.infoTglAbsensi, "Tanggal Buat"),
        KEMANDORAN(R.id.infoKemandoranAbsensi, "Kemandoran"),
        KARYAWANMSK(R.id.infoKaryawanMskAbsensi, "Kehadiran"),
        KARYAWANTDKMSK(R.id.infoKaryawanTdkMskAbsensi, "Tidak Hadir"),
        TTLKEHADIRAN(R.id.infoTotalKehadiran, "Total Kehadiran"),
    }

    override fun getItemCount() = items.size

    fun updateArchiveState(state: Int) {
        currentArchiveState = state
        notifyDataSetChanged()
    }
}