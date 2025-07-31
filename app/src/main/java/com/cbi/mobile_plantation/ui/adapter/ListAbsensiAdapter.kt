package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.ui.adapter.WeighBridgeAdapter.Info
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.PrefManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

data class AbsensiDataRekap(
    val id: Int,
    val afdeling: String,
    val datetime: String,
    val kemandoran_kode: String,
    val kemandoran: String,
    val karyawan_msk_id: String,
    val karyawan_tdk_msk_id: String,
    val karyawan_msk_nama: String,
    val karyawan_tdk_msk_nama: String,
    val karyawan_msk_nik: String,
    val karyawan_tdk_msk_nik: String,
    val status_upload: Int
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

        // Use new JSON-aware counting method
        val jmlhKaryawanMskDetail = calculateAttendanceCounts(item.karyawan_msk_id)
        val jmlhKaryawanTdkMskDetail = calculateAttendanceCounts(item.karyawan_tdk_msk_id)

        // Handle item clicks differently based on archive state
        holder.itemView.setOnClickListener {
            if (selectionMode) {
                // When in selection mode, toggle the checkbox
                if (canSelectItem(item)) { // Only allow selection if appropriate for current archive state
                    holder.checkBox.isChecked = !holder.checkBox.isChecked
                    toggleSelection(item)
                }
            } else {
                // When not in selection mode, show the bottom sheet
                showBottomSheetDialog(holder, item, jmlhKaryawanMskDetail, jmlhKaryawanTdkMskDetail)
                AppLogger.d("testTampil${holder}")
                AppLogger.d("testTampil${item}")
                AppLogger.d("testTampil${jmlhKaryawanMskDetail}")
                AppLogger.d("testTampil${jmlhKaryawanTdkMskDetail}")
            }
        }

        // Use the same counting method for display
        val jmlhKaryawanMsk = calculateAttendanceCounts(item.karyawan_msk_id)
        val jmlhKaryawanTdkMsk = calculateAttendanceCounts(item.karyawan_tdk_msk_id)

        holder.td1.text = item.afdeling
        holder.td2.text = item.kemandoran
        // Show the count of employees present (different text in archived state)
        if (currentArchiveState == 0) { // Before archive
            holder.td3.text = "$jmlhKaryawanMsk orang"
        } else { // After archive
            holder.td3.text = "$jmlhKaryawanMsk orang" // Same display but make sure it's from the right data source
        }

        // Apply different styling based on archive state
        applyStateSpecificStyling(holder, item)

        // Control checkbox visibility based on selection mode
        holder.flCheckBox.visibility = View.VISIBLE // Always visible
        holder.checkBox.isChecked = selectedItems.contains(item)

        // Disable checkbox for items that can't be selected in current state
        holder.checkBox.isEnabled = canSelectItem(item)

        // Handle checkbox clicks
        holder.checkBox.setOnClickListener {
            if (canSelectItem(item)) {
                toggleSelection(item)
            }
        }

        // Allow long click to enter selection mode
        holder.itemView.setOnLongClickListener {
            if (!selectionMode && canSelectItem(item)) {
                enableSelectionMode()
                toggleSelection(item)
                holder.checkBox.isChecked = true
                true
            } else {
                false
            }
        }
    }

    private fun applyStateSpecificStyling(holder: ListAbsensiViewHolder, item: AbsensiDataRekap) {
        when (currentArchiveState) {
            0 -> { // Active items (before archive)
                holder.itemView.alpha = 1.0f
                holder.itemView.isEnabled = true
            }
            1 -> { // Archived items (after archive)
                holder.itemView.alpha = 0.7f // Dimmed appearance for archived items
            }
        }
    }

    private fun canSelectItem(item: AbsensiDataRekap): Boolean {
        // For state 0 (before archive) - allow selection
        // For state 1 (after archive) - don't allow selection
        return currentArchiveState == 0
    }

    // Data class for employee attendance editing
    data class EmployeeAttendance(
        val id: String,
        val nik: String,
        val name: String,
        var status: String, // Hadir, Mangkir, Sakit, Izin, Cuti
        val kemandoranKey: String? = null
    )

    // Adapter for employee attendance editing
    class EditAttendanceAdapter(
        private val employees: MutableList<EmployeeAttendance>
    ) : RecyclerView.Adapter<EditAttendanceAdapter.EditAttendanceViewHolder>() {

        private val attendanceStatuses = listOf("Hadir", "Mangkir", "Sakit", "Izin", "Cuti")

        class EditAttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvEmployeeName: TextView = itemView.findViewById(R.id.tvEmployeeName)
            val spAttendanceStatus: MaterialSpinner = itemView.findViewById(R.id.spAttendanceStatus)
            val cardView: MaterialCardView = itemView.findViewById(R.id.MCVSpinner)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditAttendanceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_edit_absensi, parent, false)
            return EditAttendanceViewHolder(view)
        }

        override fun onBindViewHolder(holder: EditAttendanceViewHolder, position: Int) {
            val employee = employees[position]

            // Display name with NIK on new line
            holder.tvEmployeeName.text = "${employee.name}\n${employee.nik}"

            // Set up spinner with attendance statuses
            holder.spAttendanceStatus.setItems(attendanceStatuses)

            // Set current selection based on employee status
            val currentIndex = attendanceStatuses.indexOf(employee.status)
            if (currentIndex != -1) {
                holder.spAttendanceStatus.selectedIndex = currentIndex
            }

            // Set initial border color based on current status
            updateCardBorderColor(holder.cardView, employee.status)

            // Handle spinner selection change
            holder.spAttendanceStatus.setOnItemSelectedListener { _, _, _, item ->
                val newStatus = item.toString()
                employee.status = newStatus
                updateCardBorderColor(holder.cardView, newStatus)
            }
        }

        private fun updateCardBorderColor(cardView: MaterialCardView, status: String) {
            val context = cardView.context
            when (status) {
                "Hadir" -> {
                    cardView.strokeColor = ContextCompat.getColor(context, R.color.greenDarker)
                    cardView.strokeWidth = 5
                }
                "Mangkir" -> {
                    cardView.strokeColor = ContextCompat.getColor(context, R.color.colorRedDark)
                    cardView.strokeWidth = 5
                }
                "Sakit", "Izin", "Cuti" -> {
                    cardView.strokeColor = ContextCompat.getColor(context, R.color.orangeButton)
                    cardView.strokeWidth = 5
                }
                else -> {
                    cardView.strokeColor = ContextCompat.getColor(context, R.color.grayDefault)
                    cardView.strokeWidth = 5
                }
            }
        }

        override fun getItemCount(): Int = employees.size

        fun getUpdatedEmployees(): List<EmployeeAttendance> = employees
    }

    // Updated showBottomSheetDialog method in your main adapter
    private fun showBottomSheetDialog(holder: ListAbsensiViewHolder, item: AbsensiDataRekap,
                                      jmlhKaryawanMskDetail: Int, jmlhKaryawanTdkMskDetail: Int) {
        AppLogger.d("testTampil")
        val context = holder.itemView.context
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context)
            .inflate(R.layout.layout_bottom_sheet_detail_table_absensi, null)

        view.findViewById<TextView>(R.id.titleDialogDetailTableAbsensi).text =
            "Detail Kehadiran ${userName}"

        // Add archive state information to the bottom sheet
        val stateText = if (currentArchiveState == 0) "Aktif" else "Diarsipkan"
        view.findViewById<TextView>(R.id.titleDialogDetailTableAbsensi).append(" ($stateText)")

        // Close button click listener
        view.findViewById<android.widget.Button>(R.id.btnCloseDetailTableAbsensi).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // Edit button setup with visibility control
        val editButton = view.findViewById<android.widget.Button>(R.id.btnEditAbsensi)
        if (currentArchiveState == 0) {
            // Show edit button for active state
            editButton.visibility = View.VISIBLE
            editButton.setOnClickListener {
                bottomSheetDialog.dismiss()
                showEditAttendanceBottomSheetNew(context, item)
            }
        } else {
            // Hide edit button for archived state
            editButton.visibility = View.GONE
        }

        bottomSheetDialog.setContentView(view)

        val maxHeight = (context.resources.displayMetrics.heightPixels * 0.85).toInt()

        // Format attendance data using a utility function to eliminate code duplication
        val formattedKaryawanMsk = formatAttendanceData(
            item.karyawan_msk_nik,
            item.karyawan_msk_nama,
            item.karyawan_msk_id,
            item.kemandoran_kode,
            item.kemandoran
        )

        val formattedKaryawanTdkMsk = formatAttendanceData(
            item.karyawan_tdk_msk_nik,
            item.karyawan_tdk_msk_nama,
            item.karyawan_tdk_msk_id,
            item.kemandoran_kode,
            item.kemandoran
        )

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

    private fun showEditAttendanceBottomSheetNew(context: Context, item: AbsensiDataRekap) {
        val editBottomSheetDialog = BottomSheetDialog(context)
        val editView = LayoutInflater.from(context)
            .inflate(R.layout.layout_bottom_sheet_edit_absensi, null)

        val employeeList = mutableListOf<EmployeeAttendance>()

        // Parse MSK (Present) employees - these will have "h" key in JSON
        val mskEmployees = parseNewAttendanceDataFromSeparateFields(
            item.karyawan_msk_id,
            item.karyawan_msk_nama,
            item.karyawan_msk_nik
        )
        employeeList.addAll(mskEmployees)

        // Parse TDK_MSK (Absent) employees - these will have "m", "s", "i", "c" keys in JSON
        val tdkMskEmployees = parseNewAttendanceDataFromSeparateFields(
            item.karyawan_tdk_msk_id,
            item.karyawan_tdk_msk_nama,
            item.karyawan_tdk_msk_nik
        )
        employeeList.addAll(tdkMskEmployees)

        // Sort employees: Mangkir first, then others
        employeeList.sortWith(compareBy<EmployeeAttendance> {
            when(it.status) {
                "Mangkir" -> 0
                "Hadir" -> 1
                else -> 2
            }
        }.thenBy { it.name })

        // Set up RecyclerView
        val recyclerView = editView.findViewById<RecyclerView>(R.id.recyclerViewEditAbsensi)
        val editAdapter = EditAttendanceAdapter(employeeList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = editAdapter

        // Cancel button
        editView.findViewById<Button>(R.id.btnCancelEditAbsensi).setOnClickListener {
            editBottomSheetDialog.dismiss()
        }

        // Save button - IMPLEMENT THE SAVE FUNCTIONALITY
        editView.findViewById<Button>(R.id.btnUpdateAbsensi).setOnClickListener {
            // Tampilkan konfirmasi sebelum update
            AlertDialogUtility.withTwoActions(
                context,
                "KONFIRMASI",
                "Perbarui Data Absensi",
                "Apakah Anda yakin ingin memperbarui data absensi ini?",
                "warning.json",
                function = {
                    val updatedEmployees = editAdapter.getUpdatedEmployees()
                    handleSaveNewAttendanceStructure(updatedEmployees, item, context) { success ->
                        if (success) {
                            editBottomSheetDialog.dismiss()

                            // Tampilkan dialog sukses
                            AlertDialogUtility.withTwoActions(
                                context,
                                "BERHASIL",
                                "Data Berhasil Diperbarui",
                                "Data absensi berhasil diperbarui",
                                "success.json",
                                function = {
                                    // Kembali ke activity sebelumnya
                                    (context as? Activity)?.finish()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Gagal mengupdate data absensi", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        editBottomSheetDialog.setContentView(editView)

        val maxHeight = (context.resources.displayMetrics.heightPixels * 0.85).toInt()
        editBottomSheetDialog.show()

        editBottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
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

    private fun parseNewAttendanceDataFromSeparateFields(
        idData: String?,
        namaData: String?,
        nikData: String?
    ): List<EmployeeAttendance> {
        if (idData.isNullOrEmpty() || namaData.isNullOrEmpty() || nikData.isNullOrEmpty()) {
            return emptyList()
        }

        val employeeList = mutableListOf<EmployeeAttendance>()

        try {
            val idJson = JSONObject(idData)
            val namaJson = JSONObject(namaData)
            val nikJson = JSONObject(nikData)

            // Status mapping
            val statusMap = mapOf(
                "h" to "Hadir",
                "m" to "Mangkir",
                "s" to "Sakit",
                "i" to "Izin",
                "c" to "Cuti"
            )

            // Process each status that exists in the JSON
            statusMap.forEach { (statusKey, statusName) ->
                if (idJson.has(statusKey) && namaJson.has(statusKey) && nikJson.has(statusKey)) {
                    val idStatusJson = idJson.getJSONObject(statusKey)
                    val namaStatusJson = namaJson.getJSONObject(statusKey)
                    val nikStatusJson = nikJson.getJSONObject(statusKey)

                    // Process each kemandoran within the status
                    val keysIterator = idStatusJson.keys()
                    while (keysIterator.hasNext()) {
                        val kemandoranKey = keysIterator.next()

                        if (namaStatusJson.has(kemandoranKey) && nikStatusJson.has(kemandoranKey)) {
                            val ids = idStatusJson.getString(kemandoranKey).split(",").map { it.trim() }
                            val names = namaStatusJson.getString(kemandoranKey).split(",").map { it.trim() }
                            val niks = nikStatusJson.getString(kemandoranKey).split(",").map { it.trim() }

                            // Ensure all arrays have the same size
                            if (ids.size == names.size && names.size == niks.size) {
                                ids.forEachIndexed { index, id ->
                                    if (id.isNotBlank() && names[index].isNotBlank() && niks[index].isNotBlank()) {
                                        employeeList.add(
                                            EmployeeAttendance(
                                                id = id,
                                                nik = niks[index],
                                                name = names[index],
                                                status = statusName,
                                                kemandoranKey = kemandoranKey // IMPORTANT: Add kemandoran tracking
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            AppLogger.e("Error parsing attendance data from separate fields: ${e.message}")
        }

        return employeeList
    }

    private fun handleSaveNewAttendanceStructure(
        updatedEmployees: List<EmployeeAttendance>,
        originalItem: AbsensiDataRekap,
        context: Context,
        callback: (Boolean) -> Unit
    ) {

        val database = AppDatabase.getDatabase(context)
        val absensiDao = database.absensiDao()
        val uploadCmpDao = database.uploadCMPDao() // Assuming you have this DAO

        try {
            // ... (previous code for creating JSON structures remains the same)

            // Status mapping - reverse mapping for JSON keys
            val statusToKeyMap = mapOf(
                "Hadir" to "h",
                "Mangkir" to "m",
                "Sakit" to "s",
                "Izin" to "i",
                "Cuti" to "c"
            )

            // Separate present (Hadir) and absent (non-Hadir) employees
            val presentEmployees = updatedEmployees.filter { it.status == "Hadir" }
            val absentEmployees = updatedEmployees.filter { it.status != "Hadir" }

            // Create MSK (Present) JSON structures
            val mskIdJson = JSONObject()
            val mskNamaJson = JSONObject()
            val mskNikJson = JSONObject()

            if (presentEmployees.isNotEmpty()) {
                val hIdObj = JSONObject()
                val hNamaObj = JSONObject()
                val hNikObj = JSONObject()

                val presentByKemandoran = presentEmployees.groupBy { it.kemandoranKey ?: "314" }

                presentByKemandoran.forEach { (kemandoran, employees) ->
                    val ids = employees.map { it.id }.joinToString(",")
                    val names = employees.map { it.name }.joinToString(",")
                    val niks = employees.map { it.nik }.joinToString(",")

                    hIdObj.put(kemandoran, ids)
                    hNamaObj.put(kemandoran, names)
                    hNikObj.put(kemandoran, niks)
                }

                mskIdJson.put("h", hIdObj)
                mskNamaJson.put("h", hNamaObj)
                mskNikJson.put("h", hNikObj)
            }

            // Create TDK_MSK (Absent) JSON structures
            val tdkMskIdJson = JSONObject()
            val tdkMskNamaJson = JSONObject()
            val tdkMskNikJson = JSONObject()

            if (absentEmployees.isNotEmpty()) {
                val absentByStatus = absentEmployees.groupBy { it.status }

                absentByStatus.forEach { (status, employees) ->
                    val statusKey = statusToKeyMap[status] ?: return@forEach

                    val statusIdObj = JSONObject()
                    val statusNamaObj = JSONObject()
                    val statusNikObj = JSONObject()

                    val employeesByKemandoran = employees.groupBy { it.kemandoranKey ?: "314" }

                    employeesByKemandoran.forEach { (kemandoran, kemandoranEmployees) ->
                        val ids = kemandoranEmployees.map { it.id }.joinToString(",")
                        val names = kemandoranEmployees.map { it.name }.joinToString(",")
                        val niks = kemandoranEmployees.map { it.nik }.joinToString(",")

                        statusIdObj.put(kemandoran, ids)
                        statusNamaObj.put(kemandoran, names)
                        statusNikObj.put(kemandoran, niks)
                    }

                    tdkMskIdJson.put(statusKey, statusIdObj)
                    tdkMskNamaJson.put(statusKey, statusNamaObj)
                    tdkMskNikJson.put(statusKey, statusNikObj)
                }
            }

            // Determine the new status_upload value
            val newStatusUpload = if (originalItem.status_upload != 0) 0 else originalItem.status_upload

            // Create updated item with new JSON data
            val updatedItem = originalItem.copy(
                karyawan_msk_id = if (mskIdJson.length() > 0) mskIdJson.toString() else "{}",
                karyawan_msk_nama = if (mskNamaJson.length() > 0) mskNamaJson.toString() else "{}",
                karyawan_msk_nik = if (mskNikJson.length() > 0) mskNikJson.toString() else "{}",
                karyawan_tdk_msk_id = if (tdkMskIdJson.length() > 0) tdkMskIdJson.toString() else "{}",
                karyawan_tdk_msk_nama = if (tdkMskNamaJson.length() > 0) tdkMskNamaJson.toString() else "{}",
                karyawan_tdk_msk_nik = if (tdkMskNikJson.length() > 0) tdkMskNikJson.toString() else "{}",
                status_upload = newStatusUpload
            )

            AppLogger.d("Original item: ${originalItem.toString()}")
            AppLogger.d("Original status_upload: ${originalItem.status_upload}")
            AppLogger.d("New status_upload: $newStatusUpload")

            CoroutineScope(Dispatchers.IO).launch {
                try {

                    // Before deletion
                    val recordsToDelete = uploadCmpDao.deleteUploadCmpByAbsensiId(originalItem.id)
                    AppLogger.d("Records to delete: ${recordsToDelete}")

                    // Update attendance record
                    absensiDao.updateAbsensiFields(
                        id = originalItem.id,
                        mskId = updatedItem.karyawan_msk_id,
                        mskNama = updatedItem.karyawan_msk_nama,
                        mskNik = updatedItem.karyawan_msk_nik,
                        tdkMskId = updatedItem.karyawan_tdk_msk_id,
                        tdkMskNama = updatedItem.karyawan_tdk_msk_nama,
                        tdkMskNik = updatedItem.karyawan_tdk_msk_nik,
                        statusUpload = newStatusUpload
                    )

                    withContext(Dispatchers.Main) {
                        callback(true)
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error in database operations: ${e.message}")
                    withContext(Dispatchers.Main) {
                        callback(false)
                    }
                }
            }

        } catch (e: Exception) {
            AppLogger.e("Error saving attendance data: ${e.message}")
            callback(false)
        }
    }

    private fun calculateAttendanceCounts(jsonString: String): Int {
        return try {
            if (jsonString.isNotEmpty() && jsonString.startsWith("{")) {
                val jsonObj = JSONObject(jsonString)
                var totalCount = 0

                jsonObj.keys().forEach { kemandoranId ->
                    val list = jsonObj.optString(kemandoranId, "").split(",").filter { it.isNotEmpty() }
                    totalCount += list.size
                }

                totalCount
            } else {
                // Fallback for old format or empty data
                if (jsonString.isNotEmpty()) {
                    jsonString.split(",").filter { it.isNotEmpty() }.size
                } else {
                    0
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error calculating attendance counts: ${e.message}")
            // Fallback to old method if JSON parsing fails
            if (jsonString.isNotEmpty()) {
                jsonString.split(",").filter { it.isNotEmpty() }.size
            } else {
                0
            }
        }
    }

    private fun formatAttendanceData(
        nikJson: String,
        namaJson: String,
        idJson: String,
        kemandoranKode: String,
        kemandoranNames: String
    ): String {
        try {
            // Parse JSON strings
            val nikJsonObj = if (nikJson.isNotEmpty() && nikJson.startsWith("{")) {
                JSONObject(nikJson)
            } else {
                return "-"  // Return "-" if no data
            }

            val namaJsonObj = if (namaJson.isNotEmpty() && namaJson.startsWith("{")) {
                JSONObject(namaJson)
            } else {
                return "-"  // Return "-" if no data
            }

            // Split kemandoran codes and names
            val kemandoranKodes = kemandoranKode.split(",").map { it.trim() }
            val kemandoranNameList = kemandoranNames.split("\n").map { it.trim() }

            val result = StringBuilder()
            var hasAnyData = false  // Track if we have any actual data

            kemandoranKodes.forEachIndexed { index, kode ->
                try {
                    val nikList = mutableListOf<String>()
                    val namaList = mutableListOf<String>()

                    // Get all category keys from both JSON objects
                    val nikCategoryKeys = mutableListOf<String>()
                    val namaCategoryKeys = mutableListOf<String>()

                    // Collect NIK category keys
                    val nikIterator = nikJsonObj.keys()
                    while (nikIterator.hasNext()) {
                        nikCategoryKeys.add(nikIterator.next())
                    }

                    // Collect Nama category keys
                    val namaIterator = namaJsonObj.keys()
                    while (namaIterator.hasNext()) {
                        namaCategoryKeys.add(namaIterator.next())
                    }

                    // Process NIK data from all categories
                    nikCategoryKeys.forEach { categoryKey ->
                        val categoryObj = nikJsonObj.optJSONObject(categoryKey)
                        if (categoryObj != null) {
                            val nikData = categoryObj.optString(kode, "")
                            if (nikData.isNotEmpty()) {
                                nikList.addAll(nikData.split(",").filter { it.isNotEmpty() })
                            }
                        }
                    }

                    // Process Nama data from all categories
                    namaCategoryKeys.forEach { categoryKey ->
                        val categoryObj = namaJsonObj.optJSONObject(categoryKey)
                        if (categoryObj != null) {
                            val namaData = categoryObj.optString(kode, "")
                            if (namaData.isNotEmpty()) {
                                namaList.addAll(namaData.split(",").filter { it.isNotEmpty() })
                            }
                        }
                    }

                    // Only add if there's data for this kemandoran
                    if (nikList.isNotEmpty() && namaList.isNotEmpty()) {
                        hasAnyData = true  // We found some data

                        // Get kemandoran name (use index if available, otherwise use kode)
                        val kemandoranName = if (index < kemandoranNameList.size) {
                            kemandoranNameList[index]
                        } else {
                            "Kemandoran $kode"
                        }

                        result.append("$kemandoranName\n")

                        // Combine NIK and Nama
                        val minSize = minOf(nikList.size, namaList.size)
                        for (i in 0 until minSize) {
                            result.append("• ${nikList[i]} - ${namaList[i]}\n")
                        }

                        // Add extra space between kemandoran groups (except for the last one)
                        if (index != kemandoranKodes.size - 1) {
                            result.append("\n")
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error processing kemandoran $kode: ${e.message}")
                }
            }

            // Return "-" if no data was found, otherwise return the formatted result
            return if (hasAnyData) {
                result.toString().trimEnd()
            } else {
                "-"
            }

        } catch (e: Exception) {
            AppLogger.e("Error formatting attendance data: ${e.message}")
            return "-"  // Return "-" on error instead of error message
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
        // Only select items that are valid for the current state
        selectedItems.addAll(items.filter { canSelectItem(it) })
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

        AppLogger.d("List updated with ${newList.size} items in state: ${if (currentArchiveState == 0) "Active" else "Archived"}")
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

    fun updateArchiveState(state: Int) {
        if (currentArchiveState != state) {
            currentArchiveState = state
            clearSelections()
            notifyDataSetChanged()
            AppLogger.d("Archive state updated to: ${if (state == 0) "Active" else "Archived"}")
        }
    }

    // Get current archive state
    fun getCurrentArchiveState(): Int {
        return currentArchiveState
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
}