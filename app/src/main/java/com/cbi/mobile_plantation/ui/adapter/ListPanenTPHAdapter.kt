package com.cbi.mobile_plantation.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.databinding.TableItemRowBinding
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.playSound
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.jaredrummler.materialspinner.MaterialSpinner
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale



class ListPanenTPHAdapter : RecyclerView.Adapter<ListPanenTPHAdapter.ListPanenTPHViewHolder>() {
    enum class SortField {
        TPH,
        BLOK,
        GRADING,
        TIME,
        CHECKED
    }
    private val manuallyDeselectedItems = mutableSetOf<Int>()
    private val checkedBlocksDetails = mutableMapOf<String, Pair<Int, Int>>() // Map of blokName to (totalJJG, count)
    private var currentSortField: SortField = SortField.TPH
    private var tphList = mutableListOf<Map<String, Any>>()
    private var filteredList = mutableListOf<Map<String, Any>>()
    private var currentArchiveState: Int = 0
    private val selectedItems = mutableSetOf<Int>()
    private var isSortAscending: Boolean? = null
    private var selectAllState = false
    private val preselectedTphIds = mutableSetOf<String>()
    private val scannedTphIdsSet = mutableSetOf<String>()

    private var featureName: String = ""
    private var tphListScan: List<String> = emptyList()

    private var onSelectionChangeListener: ((Int) -> Unit)? = null

    private val uniqueUsernames = mutableListOf<String>()

    private var onDataRefreshCallback: ((position: Int) -> Unit)? = null

    // Method to set the callback
    fun setOnDataRefreshCallback(callback: (position: Int) -> Unit) {
        onDataRefreshCallback = callback
    }


    data class ExtractedData(
        val gradingText: String,
        val blokText: String,
        val tanggalText: String,
        val tphText: String,
        val searchableText: String,
        val tphId: Int,
        val panenId: Int,
        val username: String
    )

    fun setFeatureAndScanned(feature: String, tphList: List<String>) {
        featureName = feature
        tphListScan = tphList


        preSelectTphIds()
        notifyDataSetChanged()
    }

    fun extractData(item: Map<String, Any>): ExtractedData {
        Log.d("ListPanenTPHAdapterTest", "extractData: $item")

        val panenId = item["id"] as? String ?: "0"
        val tphId = item["tph_id"] as? String ?: "0"

        val blokName = item["blok_name"] as? String ?: "-"
        val noTPH = item["nomor"] as? String ?: "-"
        val dateCreated = item["date_created"] as? String ?: "-"

        val jjgJsonString = item["jjg_json"] as? String ?: "{}"
        val jjgJson = try {
            JSONObject(jjgJsonString)
        } catch (e: JSONException) {
            JSONObject()
        }

//        val totalJjg = if (featureName == "Buat eSPB" || featureName == "Rekap panen dan restan" || featureName == AppUtils.ListFeatureNames.DetailESPB) {
//            jjgJson.optInt("KP", 0)
//        } else {
//            jjgJson.optInt("TO", 0) //diganti KP
//        }

        val totalJjg =  jjgJson.optInt("KP", 0)

        val formattedTime = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("dd MMM yy\nHH:mm", Locale("id", "ID")) // Indonesian format
            val date = inputFormat.parse(dateCreated)
            outputFormat.format(date ?: "-")
        } catch (e: Exception) {
            "-"
        }
        val username = try {
            item["username"] as? String ?: "-"
        }catch (e: Exception){
            AppLogger.e(e.toString())
            "-"
        }

        val blokText = "$blokName"
        val noTPHText = noTPH
        val gradingText = "$totalJjg"
        val searchableText = "$blokText $noTPHText $gradingText $formattedTime"

        return ExtractedData(
            gradingText,
            blokText,
            formattedTime,
            noTPHText,
            searchableText,
            tphId.toInt(),
            panenId.toInt(),
            username
        )
    }


    fun filterData(query: String) {
        // First apply the filter
        filteredList = if (query.isEmpty()) {
            tphList.toMutableList()
        } else {
            tphList.filter { item ->
                val extractedData = extractData(item)
                extractedData.searchableText.contains(query, ignoreCase = true)
            }.toMutableList()
        }

        // Then reapply current sort if exists
        isSortAscending?.let { ascending ->
            sortData(currentSortField, ascending)
        }

        selectedItems.clear()
        selectAllState = false
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(0)
    }

    fun sortData(ascending: Boolean) {
        sortData(SortField.TPH, ascending) // Default to TPH sorting for backward compatibility
    }

    fun sortData(field: SortField, ascending: Boolean) {
        currentSortField = field
        isSortAscending = ascending

        filteredList = filteredList.sortedWith(compareBy<Map<String, Any>> { item ->
            when (field) {
                SortField.CHECKED -> {
                    // Sort by checked status first
                    val position = tphList.indexOf(item)
                    if (!selectedItems.contains(position)) 1 else 0
                }

                SortField.TPH -> extractData(item).tphText.replace(Regex("[^0-9]"), "")
                    .toIntOrNull() ?: 0

                SortField.BLOK -> extractData(item).blokText
                SortField.GRADING -> extractData(item).gradingText.toIntOrNull() ?: 0
                SortField.TIME -> extractData(item).tanggalText
            }
        }.let { comparator ->
            if (!ascending) comparator.reversed() else comparator
        }).toMutableList()

        notifyDataSetChanged()
    }

    fun sortByCheckedItems(ascending: Boolean = true) {
        sortData(SortField.CHECKED, ascending)
    }

    fun resetSort() {
        isSortAscending = null
        currentSortField = SortField.TPH
        filteredList = tphList.toMutableList()
        notifyDataSetChanged()
    }

    private var totalCheckedTPH = 0
    private var totalCheckedJjg = 0
    // Add a new field to track block names
    private val checkedBlocks = mutableSetOf<String>()

    // Modify the listener to include blocks
    private var onTotalsUpdateListener: ((tphCount: Int, jjgCount: Int, formattedBlocks: List<String>) -> Unit)? = null

    // Update the method signature
    fun setOnTotalsUpdateListener(listener: (tphCount: Int, jjgCount: Int, formattedBlocks: List<String>) -> Unit) {
        onTotalsUpdateListener = listener
        // Initialize with current values
        calculateTotals()
    }



    private fun calculateTotals() {
        var jjgCount = 0
        var tphCount = 0
        checkedBlocks.clear() // Clear previous blocks
        checkedBlocksDetails.clear() // Clear previous block details

        val tphMap = mutableMapOf<String, Int>()

        // Process manually selected items
        for (position in selectedItems) {
            tphList.getOrNull(position)?.let { item ->
                val tphId = item["tph_id"].toString()
                val extractedData = extractData(item)

                // Get block name directly from item data
                val blokName = item["blok_name"].toString()

                // Add block name to the set
                checkedBlocks.add(blokName)

                // Extract jjg count from the item
                val jjgJsonString = item["jjg_json"] as? String ?: "{}"
                try {
                    val jjgJson = JSONObject(jjgJsonString)
                    // Use different fields based on feature name
                    val jjgValue =
                        jjgJson.optInt("KP", 0)


                    // Update block details - add jjgValue to total and increment count
                    val currentDetails = checkedBlocksDetails[blokName] ?: Pair(0, 0)
                    checkedBlocksDetails[blokName] = Pair(
                        currentDetails.first + jjgValue,  // Sum of jjg values
                        currentDetails.second + 1         // Count of occurrences
                    )

                    // Add to the map - if TPH ID already exists, sum the values
                    tphMap[tphId] = (tphMap[tphId] ?: 0) + jjgValue
                    jjgCount += jjgValue

                    // Count each TPH even if it's a duplicate ID
                    tphCount++
                } catch (e: Exception) {
                    Log.e("ListPanenTPHAdapter", "Error parsing jjg_json: ${e.message}")
                }
            }
        }

        // Process scanned items
        for (item in tphList) {
            val tphId = item["tph_id"].toString()
            if (tphListScan.contains(tphId) && !selectedItems.contains(tphList.indexOf(item))) {
                val blokName = item["blok_name"].toString()

                // Add block name to the set
                checkedBlocks.add(blokName)

                // Extract jjg count from the item
                val jjgJsonString = item["jjg_json"] as? String ?: "{}"
                try {
                    val jjgJson = JSONObject(jjgJsonString)
                    // Use different fields based on feature name
                    val jjgValue =
                        jjgJson.optInt("KP", 0)

                    // Update block details - add jjgValue to total and increment count
                    val currentDetails = checkedBlocksDetails[blokName] ?: Pair(0, 0)
                    checkedBlocksDetails[blokName] = Pair(
                        currentDetails.first + jjgValue,  // Sum of jjg values
                        currentDetails.second + 1         // Count of occurrences
                    )

                    // Add to the map
                    tphMap[tphId] = (tphMap[tphId] ?: 0) + jjgValue
                    jjgCount += jjgValue

                    // Count each TPH even if it's a duplicate ID
                    tphCount++
                } catch (e: Exception) {
                    Log.e("ListPanenTPHAdapter", "Error parsing jjg_json: ${e.message}")
                }
            }
        }

        // Store the raw count instead of unique IDs count
        totalCheckedTPH = tphCount
        totalCheckedJjg = jjgCount

        // Create formatted block list with counts and jjg totals
        val formattedBlocks = checkedBlocksDetails.map { (blokName, details) ->
            val (jjgTotal, count) = details
            "$blokName ($jjgTotal/$count)"
        }.sorted()

        // Notify listener with the formatted blocks list
        onTotalsUpdateListener?.invoke(totalCheckedTPH, totalCheckedJjg, formattedBlocks)
    }

    class ListPanenTPHViewHolder(private val binding: TableItemRowBinding) :
        RecyclerView.ViewHolder(binding.root) {



        fun bind(
            data: Map<String, Any>,
            context: Context,
            isSelected: Boolean,
            archiveState: Int,
            onCheckedChange: (Boolean) -> Unit,
            extractData: (Map<String, Any>) -> ExtractedData,
            featureName: String = "",
            isScannedItem: Boolean = false,
            uniqueUsernames: MutableList<String>,
            onDataRefreshCallback: ((position: Int) -> Unit)? = null
        ) {
//            var usernameList: List<Int> = emptyList()

            val extractedData = extractData(data)
            binding.td1.visibility = View.VISIBLE
            binding.td2.visibility = View.VISIBLE
            binding.td3.visibility = View.VISIBLE
            if (featureName == AppUtils.ListFeatureNames.DetailESPB && archiveState ==1){
                binding.td4.visibility = View.GONE
            }else{
                binding.td4.visibility = View.VISIBLE
            }

            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen && archiveState == 2) {
                binding.td1.text = data["nama_karyawans"].toString()
                binding.td2.text = data["jjg_each_blok"].toString()
                binding.td3.text = data["tph_count"].toString()
                binding.td4.text = "${data["jjg_total_blok"]} (${data["jjg_dibayar"]})"

                itemView.isClickable = true
                itemView.isFocusable = true

                itemView.setOnClickListener {
                    val context = itemView.context
                    val bottomSheetDialog = BottomSheetDialog(context)
                    val view = LayoutInflater.from(context)
                        .inflate(R.layout.layout_bottom_sheet_detail_per_pemanen, null)

                    view.findViewById<Button>(R.id.btnCloseDetailTable).setOnClickListener {
                        bottomSheetDialog.dismiss()
                    }

                    bottomSheetDialog.setContentView(view)

                    val maxHeight = (context.resources.displayMetrics.heightPixels * 0.85).toInt()

                    view.findViewById<TextView>(R.id.titleDialogDetailTable).text = "Detail Pemanen"

                    val jjgJsonStr = data["jjg_json"] as? String ?: "{}"
                    val jjgJson = JSONObject(jjgJsonStr)

                    val infoItems = listOf(
                        DetailInfoType.KEMANDORAN_PEMANEN to "${data["nama_kemandorans"]}",
                        DetailInfoType.NAMA_PEMANEN to "${data["nama_karyawans"]}",
                        DetailInfoType.TOTAL_JANJANG to jjgJson.optDouble("TO", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DIKIRIM_KE_PABRIK to jjgJson.optDouble("KP", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_BUAH_MASAK to jjgJson.optDouble("RI", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_JANJANG_DI_BAYAR to jjgJson.optDouble("PA", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_BUAH_MENTAH to jjgJson.optDouble("UN", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_LEWAT_MASAK to jjgJson.optDouble("OV", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_JJG_KOSONG to jjgJson.optDouble("EM", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_ABNORMAL to jjgJson.optDouble("AB", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_SERANGAN_TIKUS to jjgJson.optDouble("RA", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_TANGKAI_PANJANG to jjgJson.optDouble("LO", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_TIDAK_VCUT to jjgJson.optDouble("TI", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.DATA_BLOK to data["jjg_each_blok_bullet"].toString(),
                    )

                    infoItems.forEach { (type, value) ->
                        val itemView = view.findViewById<View>(type.id)
                        if (itemView != null) {
                            setInfoItemValues(itemView, type.label, value)
                        }
                    }
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
            }
            else if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen && archiveState == 3) {
                binding.td1.text = data["blok_name"].toString()
                binding.td2.text = data["jumlah_transaksi"].toString()
                binding.td3.text = data["jjg_total"].toString()
                binding.td4.text = data["jjg_dibayar"].toString()
                itemView.isClickable = true
                itemView.isFocusable = true


                itemView.setOnClickListener {


                    val context = itemView.context
                    val bottomSheetDialog = BottomSheetDialog(context)
                    val view = LayoutInflater.from(context)
                        .inflate(R.layout.layout_bottom_sheet_detail_per_blok, null)

                    view.findViewById<TextView>(R.id.titleDialogDetailTable).text =
                        "Detail Blok - ${data["nama_estate"]} ${data["nama_afdeling"]} ${data["blok_name"]}"

                    view.findViewById<Button>(R.id.btnCloseDetailTable).setOnClickListener {
                        bottomSheetDialog.dismiss()
                    }

                    bottomSheetDialog.show()

                    bottomSheetDialog.setContentView(view)

                    val maxHeight =
                        (context.resources.displayMetrics.heightPixels * 0.85).toInt()

                    val jjgJsonStr =
                        data["jjg_json"] as? String ?: "{}" // Ensure it's a valid JSON string
                    val jjgJson = JSONObject(jjgJsonStr) // Convert to JSONObject

                    val infoItems = listOf(
                        DetailInfoType.KEMANDORAN to "${data["nama_kemandorans_all"]}",
                        DetailInfoType.NAMA_PEMANEN to "${data["nama_karyawans_all"]}",
                        DetailInfoType.TOTAL_JANJANG to jjgJson.optDouble("TO", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DIKIRIM_KE_PABRIK to jjgJson.optDouble("KP", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_BUAH_MASAK to jjgJson.optDouble("RI", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_JANJANG_DI_BAYAR to jjgJson.optDouble("PA", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_BUAH_MENTAH to jjgJson.optDouble("UN", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_LEWAT_MASAK to jjgJson.optDouble("OV", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_JJG_KOSONG to jjgJson.optDouble("EM", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_ABNORMAL to jjgJson.optDouble("AB", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_SERANGAN_TIKUS to jjgJson.optDouble("RA", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_TANGKAI_PANJANG to jjgJson.optDouble("LO", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                        DetailInfoType.TOTAL_DATA_TIDAK_VCUT to jjgJson.optDouble("TI", 0.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
                    )

                    infoItems.forEach { (type, value) ->
                        val itemView = view.findViewById<View>(type.id)
                        if (itemView != null) {
                            setInfoItemValues(itemView, type.label, value)
                        }
                    }
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
            }
            else if(featureName == AppUtils.ListFeatureNames.BuatESPB ){
                binding.td5.visibility = View.VISIBLE
                val params = binding.td5.layoutParams as LinearLayout.LayoutParams
                params.weight = 0.2f
                binding.td5.layoutParams = params

                val username = extractedData.username


                AppLogger.d(username)
                if (!uniqueUsernames.contains(username)) {
                    uniqueUsernames.add(username)
                }
                val colorIndex = uniqueUsernames.indexOf(username)

                AppLogger.d("colorIndex $colorIndex")
                val color = getUsernameColor(colorIndex, context)
                binding.td5.setBackgroundColor(color)
                binding.td1.text = extractedData.blokText
                binding.td2.text = "${extractedData.tphText}/${extractedData.gradingText}"
                binding.td3.text = extractedData.tanggalText
                binding.td4.text = extractedData.username
            }
            else {
                binding.td1.text = extractedData.blokText
                binding.td2.text = extractedData.tphText
                binding.td3.text = extractedData.gradingText
                binding.td4.text = extractedData.tanggalText
                val checkedColor = if (isScannedItem) {
                    ContextCompat.getColor(context, R.color.greenDarker)
                } else {
                    Color.RED
                }

                val colorStateList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(checkedColor, Color.GRAY)
                )

                binding.checkBoxPanen.buttonTintList = colorStateList

                binding.checkBoxPanen.setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange(isChecked)
                }

                if ((featureName == AppUtils.ListFeatureNames.RekapHasilPanen && archiveState == 0) || (featureName == AppUtils.ListFeatureNames.RekapHasilPanen && archiveState == 1 )) {
                    itemView.isClickable = true
                    itemView.isFocusable = true

                    itemView.setOnClickListener {
                        val context = itemView.context
                        val bottomSheetDialog = BottomSheetDialog(context)
                        val view = LayoutInflater.from(context)
                            .inflate(R.layout.layout_bottom_sheet_detail_table, null)

                        view.findViewById<TextView>(R.id.titleDialogDetailTable).text =
                            "Detail Panen - ${extractedData.blokText} TPH ${extractedData.tphText}"

                        view.findViewById<Button>(R.id.btnCloseDetailTable).setOnClickListener {
                            bottomSheetDialog.dismiss()
                        }

                        AppLogger.d("data $data")

                        val btnEditPemanen = view.findViewById<Button>(R.id.btnEditPemanen)
                        if(featureName == AppUtils.ListFeatureNames.RekapHasilPanen && archiveState == 1){
                            btnEditPemanen.visibility = View.GONE
                        }else{
                            btnEditPemanen.visibility = View.VISIBLE
                        }


                        // Handle Edit Pemanen button click
                        btnEditPemanen.setOnClickListener {
                            showEditPemanenDialog(
                                context,
                                data,
                                bottomSheetDialog,
                                adapterPosition, // Pass the current position
                                onDataRefreshCallback // Pass the callback
                            )
                        }

                        val recyclerView = view.findViewById<RecyclerView>(R.id.rvPhotoAttachments)

                        if (recyclerView != null) {
                            // Enable horizontal scrolling with custom layout manager
                            val scrollableLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                            recyclerView.layoutManager = scrollableLayoutManager

                            // Add item decoration for even spacing
                            val itemSpacing = (8 * context.resources.displayMetrics.density).toInt()
                            recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
                                override fun getItemOffsets(
                                    outRect: Rect,
                                    view: View,
                                    parent: RecyclerView,
                                    state: RecyclerView.State
                                ) {
                                    outRect.left = itemSpacing / 2
                                    outRect.right = itemSpacing / 2
                                }
                            })

                            // Enable overscroll effect for better UX
                            recyclerView.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

                            val photoUrls = getPhotoUrlsFromItem(data, context)

                            // Create adapter - shows up to 5 items
                            val photoAdapter = PhotoAttachmentAdapterDetailTable(photoUrls) { position ->
                                // Handle photo click - show fullscreen
                                if (position < photoUrls.size) {
                                    // Check if file exists before showing
                                    val photoFile = File(photoUrls[position])
                                    if (photoFile.exists()) {
                                        // Show fullscreen photo
                                        showFullscreenImage(context, photoFile)
                                    } else {
                                        Toast.makeText(context, "Photo file not found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            // Set the adapter
                            recyclerView.adapter = photoAdapter
                        } else {
                            Log.e("BottomSheet", "RecyclerView not found in layout!")
                        }


                        bottomSheetDialog.setContentView(view)

                        val maxHeight =
                            (context.resources.displayMetrics.heightPixels * 0.85).toInt()

                        val dateCreatedRaw = data["date_created"] as? String ?: "-"

                        val originalFormat =
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val displayFormat =
                            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

                        val formattedDate = try {
                            val date = originalFormat.parse(dateCreatedRaw)
                            date?.let { displayFormat.format(it) } ?: "-"
                        } catch (e: Exception) {
                            "-"
                        }

                        val jjgJsonStr =
                            data["jjg_json"] as? String ?: "{}" // Ensure it's a valid JSON string
                        val jjgJson = JSONObject(jjgJsonStr) // Convert to JSONObject
                        val blokBanjir = data["blok_banjir"] as? Int ?: 0
                        val blokBanjirText = when (blokBanjir) {
                            1 -> "Ya"
                            0 -> "Tidak"
                            else -> "Tidak diketahui"
                        }
                        val infoItems = listOf(
                            DetailInfoType.TANGGAL_BUAT to formattedDate,
                            DetailInfoType.BLOK_BANJIR to blokBanjirText,
                            DetailInfoType.ESTATE_AFDELING to "${data["nama_estate"]} / ${data["nama_afdeling"]}",
                            DetailInfoType.BLOK_TAHUN to "${extractedData.blokText} / ${data["tahun_tanam"]}",
                            DetailInfoType.ANCAK to "${data["ancak"]}",
                            DetailInfoType.NO_TPH to extractedData.tphText,
                            DetailInfoType.KEMANDORAN to "${data["nama_kemandorans"]}",
                            DetailInfoType.NAMA_PEMANEN to "${data["nama_karyawans"]}",
                            DetailInfoType.TOTAL_JANJANG to (jjgJson["TO"]?.toString() ?: "0"),
                            DetailInfoType.TOTAL_DIKIRIM_KE_PABRIK to (jjgJson["KP"]?.toString()
                                ?: "0"),
                            DetailInfoType.TOTAL_JANJANG_DI_BAYAR to (jjgJson["PA"]?.toString()
                                ?: "0"),
                            DetailInfoType.TOTAL_DATA_BUAH_MENTAH to (jjgJson["UN"]?.toString()
                                ?: "0"),
                            DetailInfoType.TOTAL_DATA_LEWAT_MASAK to (jjgJson["OV"]?.toString()
                                ?: "0"),
                            DetailInfoType.TOTAL_DATA_JJG_KOSONG to (jjgJson["EM"]?.toString()
                                ?: "0"),
                            DetailInfoType.TOTAL_DATA_ABNORMAL to (jjgJson["AB"]?.toString()
                                ?: "0"),
                            DetailInfoType.TOTAL_DATA_SERANGAN_TIKUS to (jjgJson["RA"]?.toString()
                                ?: "0"),
                            DetailInfoType.TOTAL_DATA_TANGKAI_PANJANG to (jjgJson["LO"]?.toString()
                                ?: "0"),
                            DetailInfoType.TOTAL_DATA_TIDAK_VCUT to (jjgJson["TI"]?.toString()
                                ?: "0"),
                        )


                        // Set values for all items
                        infoItems.forEach { (type, value) ->
                            val itemView = view.findViewById<View>(type.id)
                            // Only call setInfoItemValues if the itemView exists
                            if (itemView != null) {
                                setInfoItemValues(itemView, type.label, value.toString())
                            }
                        }

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

                } else {
                    itemView.setOnClickListener(null)
                    itemView.isClickable = false
                    itemView.isFocusable = false
                }
            }


            if (archiveState == 1 || featureName == "Rekap panen dan restan" || featureName == "Detail eSPB") {
                if(binding.flCheckBoxItemTph.visibility  == View.GONE){
                    binding.flCheckBoxItemTph.visibility = View.VISIBLE
                }
                binding.checkBoxPanen.visibility = View.GONE
                if(archiveState == 1){
                    binding.flCheckBoxItemTph.visibility = View.GONE
                }else{
                    binding.numListTerupload.visibility = View.VISIBLE
                    binding.numListTerupload.text = "${adapterPosition + 1}."
                }

            }
            else if (archiveState == 2 && featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                binding.checkBoxPanen.visibility = View.GONE
                binding.numListTerupload.visibility = View.GONE
                binding.flCheckBoxItemTph.visibility = View.GONE
            }
            else {
                binding.checkBoxPanen.visibility = View.VISIBLE
                binding.numListTerupload.visibility = View.GONE
                binding.checkBoxPanen.setOnCheckedChangeListener(null)
                binding.checkBoxPanen.isChecked = isSelected
//                binding.checkBoxPanen.isEnabled = !isScannedItem

                val checkedColor = if (isScannedItem) {
                    ContextCompat.getColor(context, R.color.greenDarker)
                } else {
                    Color.RED
                }
                val colorStateList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(checkedColor, Color.GRAY)
                )

                // Apply the tint
                binding.checkBoxPanen.buttonTintList = colorStateList

                // Add listener AFTER setting state
                binding.checkBoxPanen.setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange(isChecked)
                }

                if(featureName == AppUtils.ListFeatureNames.RekapHasilPanen && archiveState ==3 ){
                    if (binding.checkBoxPanen.visibility == View.VISIBLE){
                        binding.flCheckBoxItemTph.visibility = View.GONE
                    }
                }
                else if(featureName == AppUtils.ListFeatureNames.RekapHasilPanen && archiveState ==0){
                    if (binding.flCheckBoxItemTph.visibility == View.GONE){
                        binding.flCheckBoxItemTph.visibility = View.VISIBLE
                    }
                }
                else if(featureName == AppUtils.ListFeatureNames.RekapHasilPanen && archiveState ==1){
                    if (binding.flCheckBoxItemTph.visibility == View.GONE){
                        binding.flCheckBoxItemTph.visibility = View.VISIBLE
                    }
                }
            }
        }

        @SuppressLint("SetTextI18n")
        private fun setInfoItemValues(view: View, label: String, value: String) {
            val textViewLabel = view.findViewById<TextView>(R.id.tvLabel)
            val textViewValue = view.findViewById<TextView>(R.id.tvValue)

            textViewLabel?.text = label

            textViewValue?.let { textView ->
                if (label == DetailInfoType.KEMANDORAN.label || label == DetailInfoType.DATA_BLOK.label) {
                    textView.text = value
                } else {
                    textView.text = ": $value"
                }
            }
        }

        private fun showEditPemanenDialog(
            context: Context,
            data: Map<String, Any>,
            bottomSheetDialog: BottomSheetDialog? = null,
            position: Int = -1,
            onDataRefreshCallback: ((position: Int) -> Unit)? = null
        ) {
            val editBottomSheetDialog = BottomSheetDialog(context)
            val editView = LayoutInflater.from(context)
                .inflate(R.layout.layout_bottom_sheet_edit_nama_pemanen, null)

            val titleDialogDetailTable= editView.findViewById<TextView>(R.id.titleDialogDetailTable)
            titleDialogDetailTable.text = "Edit Nama Pemanen"
            val selectedPemanenAdapter = SelectedWorkerAdapter()

            // Get the included layout
            val layoutPemanen = editView.findViewById<View>(R.id.layoutPemanen) // Your include layout

            // Set the title text
            val tvTitleFormPanenTBS = layoutPemanen.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
            tvTitleFormPanenTBS.text = "Pilih Pemanen"



            val maxHeight = (context.resources.displayMetrics.heightPixels * 0.65).toInt()
            // You need to add a RecyclerView to your pertanyaan_spinner_layout or create one dynamically
            // Option 1: Add RecyclerView dynamically to your existing layout
            val recyclerView = RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = selectedPemanenAdapter
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
            }

            // Add RecyclerView to the content container (inside ScrollView)
            val contentContainer = editView.findViewById<LinearLayout>(R.id.contentContainer)
            contentContainer.addView(recyclerView)

            // Get available workers from data (Worker objects from current data)
            val availableWorkers = data["available_workers"] as? List<Worker> ?: emptyList()
            selectedPemanenAdapter.setAvailableWorkers(availableWorkers)

            // Get dropdown options from absensi data (simple strings)
            val dropdownOptions = data["dropdown_absensi_edit"] as? List<String> ?: emptyList()

            // Setup existing workers (parse from current data and add to adapter)
            setupExistingWorkers(data, selectedPemanenAdapter)

            // Setup dropdown with absensi options using the existing spinner
            setupWorkerDropdown(layoutPemanen, selectedPemanenAdapter, dropdownOptions)

            // Handle Cancel button
            editView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                editBottomSheetDialog.dismiss()
            }

            editView.findViewById<Button>(R.id.btnUpdatePemanen).setOnClickListener {
                val selectedWorkers = selectedPemanenAdapter.getSelectedWorkers()
                AppLogger.d("Selected workers: $selectedWorkers")

                AlertDialogUtility.withTwoActions(
                    context,
                    context.getString(R.string.al_yes),
                    context.getString(R.string.confirmation_dialog_title),
                    "Apakah anda yakin untuk mengubah nama pemanen ini?",
                    "warning.json",
                    ContextCompat.getColor(context, R.color.greenDarker),
                    function = {
                        if (selectedWorkers.isNotEmpty()) {
                            // Get dropdown_absensi_full_data from the data map and cast it
                            val fullDataList = data["dropdown_absensi_full_data"] as? List<KaryawanModel> ?: emptyList()

                            AppLogger.d("Full data list size: ${fullDataList.size}")

                            // Extract data from dropdown_absensi_full_data based on selected workers
                            val matchedKaryawan = selectedWorkers.mapNotNull { selectedWorker ->
                                fullDataList.find { karyawan ->
                                    karyawan.nik == selectedWorker.id
                                }
                            }

                            AppLogger.d("Matched karyawan: $matchedKaryawan")

                            if (matchedKaryawan.isNotEmpty()) {
                                // Build the 4 required comma-separated strings
                                val updatedKaryawanIds = matchedKaryawan.joinToString(",") { it.id.toString() }
                                val updatedKaryawanNiks = matchedKaryawan.joinToString(",") { it.nik!! }
                                val updatedKaryawanNamas = matchedKaryawan.joinToString(",") { it.nama!! }
                                val updatedKemandoranIds = matchedKaryawan.joinToString(",") { it.kemandoran_id.toString() }

                                AppLogger.d("Updated karyawan_id: $updatedKaryawanIds")
                                AppLogger.d("Updated karyawan_nik: $updatedKaryawanNiks")
                                AppLogger.d("Updated karyawan_nama: $updatedKaryawanNamas")
                                AppLogger.d("Updated kemandoran_id: $updatedKemandoranIds")

                                // Update database with all 4 fields
                                updatePemanenWorkers(
                                    context,
                                    panenId = data["id"].toString(),
                                    karyawanIds = updatedKaryawanIds,
                                    karyawanNiks = updatedKaryawanNiks,
                                    karyawanNamas = updatedKaryawanNamas,
                                    kemandoranIds = updatedKemandoranIds,
                                    onSuccess = {
                                        // Only dismiss dialogs and trigger refresh on success
                                        editBottomSheetDialog.dismiss()
                                        bottomSheetDialog!!.dismiss()
                                        onDataRefreshCallback?.invoke(position)
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Data karyawan tidak ditemukan", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Pilih minimal satu pemanen", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                }
            }

            editBottomSheetDialog.setContentView(editView)

            editBottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.let { bottomSheet ->
                    val behavior = BottomSheetBehavior.from(bottomSheet)

                    behavior.apply {
                        this.peekHeight = maxHeight
                        this.state = BottomSheetBehavior.STATE_EXPANDED
                        this.isFitToContents = true  // Keep this as true like your working example
                        this.isDraggable = false
                    }

                    // This is the key line that was missing!
                    bottomSheet.layoutParams?.height = maxHeight
                }

            editBottomSheetDialog.show()
        }

        private fun setupExistingWorkers(data: Map<String, Any>, adapter: SelectedWorkerAdapter) {
            val karyawanNik = data["karyawan_nik"].toString()
            val karyawanNama = data["karyawan_nama"].toString()

            if (karyawanNik.isNotEmpty() && karyawanNama.isNotEmpty()) {
                val niks = karyawanNik.split(",")
                val names = karyawanNama.split(",")

                niks.forEachIndexed { index, nik ->
                    val cleanNik = nik.trim()
                    if (cleanNik.isNotEmpty() && index < names.size) {
                        val name = names[index].trim()
                        val worker = Worker(cleanNik, "$name - $cleanNik")
                        adapter.addWorker(worker)
                    }
                }
            }
        }

        private fun updatePemanenWorkers(
            context: Context,
            panenId: String,
            karyawanIds: String,
            karyawanNiks: String,
            karyawanNamas: String,
            kemandoranIds: String,
            onSuccess: (() -> Unit)? = null // Add success callback
        ) {
            val database = AppDatabase.getDatabase(context)
            val panenDao = database.panenDao()

            CoroutineScope(Dispatchers.IO).launch {
                try {
//                    throw Exception("Testing error case")

                    panenDao.updatePemanenWorkers(
                        id = panenId.toInt(),
                        karyawanId = karyawanIds,
                        karyawanNik = karyawanNiks,
                        karyawanNama = karyawanNamas,
                        kemandoranId = kemandoranIds
                    )

                    AppLogger.d("Successfully updated pemanen workers")

                    withContext(Dispatchers.Main) {
                        Toasty.success(context, "Nama pemanen berhasil diubah", Toast.LENGTH_SHORT).show()
                        context.playSound(R.raw.berhasil_edit_data)
                        onSuccess?.invoke()
                    }

                } catch (e: Exception) {
                    AppLogger.e("Error updating pemanen workers: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toasty.error(context, "Gagal memperbarui nama pemanen! ${e.message}", Toast.LENGTH_LONG).show()
                        // Don't trigger onSuccess callback on error - dialogs stay open
                    }
                }
            }
        }

        private fun setupWorkerDropdown(layoutPemanen: View, selectedPemanenAdapter: SelectedWorkerAdapter, dropdownOptions: List<String>) {
            // Use your existing MaterialSpinner
            val materialSpinner = layoutPemanen.findViewById<MaterialSpinner>(R.id.spPanenTBS)

            // Set initial hint
            materialSpinner.text = "Pilih Kategori yang sesuai"

            // Set click listener to show search popup
            materialSpinner.setOnClickListener {
                showWorkerSearchDropdown(
                    materialSpinner,
                    dropdownOptions,
                    selectedPemanenAdapter
                ) { selectedWorkerString, _ ->
                    // Parse the string to extract NIK and name
                    val parts = selectedWorkerString.split(" - ")
                    if (parts.size == 2) {
                        val workerName = parts[0].trim()
                        val workerNik = parts[1].trim()

                        // Create Worker object and add to adapter
                        val worker = Worker(workerNik, selectedWorkerString)
                        selectedPemanenAdapter.addWorker(worker)

                        // Reset spinner text
                        materialSpinner.text = "Pilih Pemanen untuk Ditambahkan"
                    }
                }
            }
        }

        private fun showWorkerSearchDropdown(
            spinner: MaterialSpinner,
            data: List<String>,
            selectedPemanenAdapter: SelectedWorkerAdapter,
            onItemSelected: (String, Int) -> Unit
        ) {
            val popupView = LayoutInflater.from(spinner.context).inflate(R.layout.layout_dropdown_search, null)
            val listView = popupView.findViewById<ListView>(R.id.listViewChoices)
            val editTextSearch = popupView.findViewById<EditText>(R.id.searchEditText)

            // Filter out already selected workers
            val selectedWorkers = selectedPemanenAdapter.getSelectedWorkers()
            val selectedNiks = selectedWorkers.map { it.id }.toSet()

            val availableData = data.filter { option ->
                val parts = option.split(" - ")
                if (parts.size == 2) {
                    val nik = parts[1].trim()
                    !selectedNiks.contains(nik)
                } else {
                    true
                }
            }

            // Create PopupWindow
            val popupWindow = PopupWindow(
                popupView,
                spinner.width,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                isFocusable = true
                isOutsideTouchable = true
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }

            var filteredData = availableData

            // Simple single selection adapter
            val adapter = object : ArrayAdapter<String>(
                spinner.context,
                android.R.layout.simple_list_item_1,
                filteredData
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val textView = view.findViewById<TextView>(android.R.id.text1)
                    textView.setTextColor(Color.BLACK)
                    return view
                }
            }

            listView.adapter = adapter

            // Search functionality
            editTextSearch.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val titleSearch = popupView.findViewById<TextView>(R.id.titleSearchDropdown)

                    filteredData = if (!s.isNullOrEmpty()) {
                        titleSearch.visibility = View.VISIBLE
                        availableData.filter { it.contains(s, ignoreCase = true) }
                    } else {
                        titleSearch.visibility = View.GONE
                        availableData
                    }

                    val filteredAdapter = object : ArrayAdapter<String>(
                        spinner.context,
                        android.R.layout.simple_list_item_1,
                        if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                            listOf("Data tidak tersedia!")
                        } else {
                            filteredData
                        }
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent)
                            val textView = view.findViewById<TextView>(android.R.id.text1)

                            if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                                textView.setTextColor(
                                    ContextCompat.getColor(context, R.color.colorRedDark)
                                )
                                textView.setTypeface(textView.typeface, Typeface.ITALIC)
                                view.isEnabled = false
                            } else {
                                textView.setTextColor(Color.BLACK)
                                textView.setTypeface(textView.typeface, Typeface.NORMAL)
                                view.isEnabled = true
                            }
                            return view
                        }

                        override fun isEnabled(position: Int): Boolean {
                            return filteredData.isNotEmpty()
                        }
                    }

                    listView.adapter = filteredAdapter
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            // Handle item selection
            listView.setOnItemClickListener { _, _, position, _ ->
                if (filteredData.isNotEmpty()) {
                    val selectedItem = filteredData[position]
                    val originalPosition = data.indexOf(selectedItem)
                    onItemSelected(selectedItem, originalPosition)
                    popupWindow.dismiss()
                }
            }

            popupWindow.showAsDropDown(spinner)

            editTextSearch.requestFocus()
            val imm = spinner.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextSearch, InputMethodManager.SHOW_IMPLICIT)
        }

// Remove the updateMaterialSpinnerOptions function as it's no longer needed

        private fun updatePemanenNama(id: String, niks: String, names: String) {
            // TODO: Implement database update
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Update in database
                    // panenViewModel.updateWorkers(id, niks, names)
                    AppLogger.d("Updated pemanen $id with niks: $niks, names: $names")
                } catch (e: Exception) {
                    AppLogger.e("Error updating pemanen: ${e.message}")
                }
            }
        }



        private fun showFullscreenImage(context: Context, imageFile: File) {
            // Create dialog
            val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
            dialog.setCancelable(true)

            // Set fullscreen flags
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

            // Inflate layout
            val view = LayoutInflater.from(context).inflate(R.layout.camera_edit, null)
            dialog.setContentView(view)

            // Find views
            val fullscreenImageView = view.findViewById<ImageView>(R.id.fotoZoom)
            val cardCloseZoom = view.findViewById<MaterialCardView>(R.id.cardCloseZoom)
            val btnCloseAlternative = view.findViewById<Button>(R.id.btnCloseAlternative)

            btnCloseAlternative.visibility = View.VISIBLE
            view.findViewById<MaterialCardView>(R.id.cardDeletePhoto).visibility = View.GONE
            view.findViewById<MaterialCardView>(R.id.cardChangePhoto).visibility = View.GONE
            cardCloseZoom.visibility = View.GONE

            Glide.with(context)
                .load(imageFile)
                .error(R.drawable.baseline_add_a_photo_24)
                .into(fullscreenImageView)

            // Set close button click listener
            btnCloseAlternative.setOnClickListener {
                dialog.dismiss()
            }

            // Show the dialog
            dialog.show()
        }

        fun getUsernameColor(index: Int, context: Context): Int {
            // Define a set of predefined colors - you can customize these
            val colors = arrayOf(
//            context.resources.getColor(R.color.bluedark),
                Color.parseColor("#4CAF50"), // Green
                Color.parseColor("#FF9800"), // Orange
                Color.parseColor("#9C27B0"), // Purple
                Color.parseColor("#E91E63"), // Pink
                Color.parseColor("#00BCD4"), // Cyan
                Color.parseColor("#FF5722"), // Deep Orange
                Color.parseColor("#607D8B")  // Blue Grey
            )

            // Use the hash to select a color
            val colorIndex = Math.abs(index) % colors.size
            return colors[colorIndex]
        }

        private fun getPhotoUrlsFromItem(item: Map<String, Any?>, context: Context): List<String> {
            // Get the root directory for storing photos
            val rootApp = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "CMP-${AppUtils.WaterMarkFotoDanFolder.WMPanenTPH}" // Store under "CMP-featureName"
            ).toString()

            // Get photos string from item
            val photoString = item["foto"] as? String

            if (photoString.isNullOrEmpty()) {
                Log.d("PhotoAdapter", "No photos found in item")
                return emptyList()
            }

            // Split the string by semicolons to get individual photo filenames
            val photoFileNames = photoString.split(";")

            // Create full file paths (up to maximum 3)
            return photoFileNames
                .take(5) // Limit to maximum 3 photos
                .map { fileName ->
                    "$rootApp/$fileName" // Full path to the photo file
                }
        }
    }

    // Add this function to get current data
    fun getCurrentData(): List<Map<String, Any>> {
        return filteredList.toList()
    }

    fun isAllSelected(): Boolean {
        return selectAllState
    }

    fun getPreSelectedItems(): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()

        // Get all items that match the scanned TPH IDs
        for (item in tphList) {
            val tphId = extractData(item).tphId.toString()
            if (tphListScan.contains(tphId)) {
                result.add(item)
            }
        }

        // Return distinct items to avoid duplicates
        return result.distinct()
    }

    fun getSelectedItems(): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()

        // Get manually selected items
        for (position in selectedItems) {
            tphList.getOrNull(position)?.let { result.add(it) }
        }


        AppLogger.d("manuallyDeselectedItems $manuallyDeselectedItems")
        // Add scanned items ONLY if they haven't been manually deselected
        for (i in tphList.indices) {
            if (!manuallyDeselectedItems.contains(i)) {
                val item = tphList[i]
                val tphId = extractData(item).tphId.toString()
                if (tphListScan.contains(tphId) && !selectedItems.contains(i)) {
                    result.add(item)
                }
            }
        }
        return result.distinct()
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
        Handler(Looper.getMainLooper()).post {
            notifyDataSetChanged()
            onSelectionChangeListener?.invoke(selectedItems.size)
            calculateTotals() // Add this line
        }
    }

    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
        manuallyDeselectedItems.clear()
        calculateTotals() // Add this line
    }

    override fun onBindViewHolder(holder: ListPanenTPHViewHolder, position: Int) {
        val item = filteredList[position]
        val tphId = extractData(item).tphId.toString()
        val isScannedItem = tphListScan.contains(tphId)
        val originalPosition = tphList.indexOf(item)

        if (isScannedItem) {
            scannedTphIdsSet.add(tphId)
        }

        holder.bind(
            data = item,
            context = holder.itemView.context,
            isSelected = selectedItems.contains(originalPosition) || (isScannedItem && !manuallyDeselectedItems.contains(originalPosition)),
            archiveState = currentArchiveState,
            onCheckedChange = { isChecked ->
                val origPos = tphList.indexOf(item)
                if (isChecked) {
                    selectedItems.add(origPos)
                    manuallyDeselectedItems.remove(origPos) // Remove from deselected if checked
                } else {
                    selectedItems.remove(origPos)
                    if (isScannedItem) {
                        // Track manually deselected scanned items
                        manuallyDeselectedItems.add(origPos)
                    }
                    selectAllState = false
                }
                onSelectionChangeListener?.invoke(selectedItems.size)
                calculateTotals()
            },
            extractData = ::extractData,
            featureName = featureName,
            isScannedItem = isScannedItem,
            uniqueUsernames = uniqueUsernames,
            onDataRefreshCallback = onDataRefreshCallback
        )
    }


    fun updateArchiveState(state: Int) {
        currentArchiveState = state
        notifyDataSetChanged()
    }

    override fun getItemCount() = filteredList.size  // Changed from tphList.size

    enum class DetailInfoType(val id: Int, val label: String) {
        TANGGAL_BUAT(R.id.DataTanggalBuat, "Dibuat pada"),
        BLOK_BANJIR(R.id.DataBlokBanjir, "Blok Banjir"),
        ESTATE_AFDELING(R.id.DataEstateAfdeling, "Estate/Afdeling"),
        BLOK_TAHUN(R.id.DataBlokTahunTanam, "Blok/Thn Tanam"),
        ANCAK(R.id.DataAncak, "Ancak"),
        NO_TPH(R.id.DataNoTPH, "No TPH"),
        KEMANDORAN(R.id.DataKemandoran, "Kemandoran"),
        KEMANDORAN_PEMANEN(R.id.DataKemandoranPemanen, "Nama Kemandoran"),
        NAMA_PEMANEN(R.id.DataNamaPemanen, "Nama Pemanen"),
        TOTAL_JANJANG(R.id.DataTotalJanjang, "Jumlah Buah Dipanen"),
        TOTAL_DIKIRIM_KE_PABRIK(R.id.DataDikirimKePabrik, "Jumlah Buah Dikirim ke Pabrik"),
        TOTAL_JANJANG_DI_BAYAR(R.id.DataJanjangDibayar, "Jumlah Janjang DiBayar"),
        TOTAL_DATA_BUAH_MASAK(R.id.DataBuahMasak, "Buah Masak"),
        TOTAL_DATA_BUAH_MENTAH(R.id.DataBuahMentah, "Buah Mentah"),
        TOTAL_DATA_LEWAT_MASAK(R.id.DataBuahLewatMasak, "Buah Lewat Masak"),
        TOTAL_DATA_JJG_KOSONG(R.id.DataBuahJjgKosong, "Janjang Kosong"),
        TOTAL_DATA_ABNORMAL(R.id.DataBuahAbnormal, "Buah Abnormal"),
        TOTAL_DATA_SERANGAN_TIKUS(R.id.DataBuahSeranganTikus, "Serangan Tikus"),
        TOTAL_DATA_TANGKAI_PANJANG(R.id.DataBuahTangkaiPanjang, "Tangkai Panjang"),
        TOTAL_DATA_TIDAK_VCUT(R.id.DataBuahTidakVcut, "Tidak V-Cut"),
        DATA_BLOK(R.id.DataBlok, "List Blok Panen"),
    }

    fun setOnSelectionChangedListener(listener: (Int) -> Unit) {
        onSelectionChangeListener = listener
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Map<String, Any>>) {

        preselectedTphIds.clear()
        preselectedTphIds.addAll(tphListScan)

        tphList.clear()
        selectedItems.clear()
        selectAllState = false
        isSortAscending = null
        tphList.addAll(newData)
        manuallyDeselectedItems.clear() // Add this line
        filteredList = tphList.toMutableList()

        // Pre-select items that match the scanned TPH IDs
        if (preselectedTphIds.isNotEmpty()) {
            tphList.forEachIndexed { index, item ->
                try {
                    val tphId = item["tph_id"].toString()
                    if (preselectedTphIds.contains(tphId)) {
                        selectedItems.add(index)
                    }
                } catch (e: Exception) {
                    Log.e("ListPanenTPHAdapter", "Error pre-selecting TPH: ${e.message}")
                }
            }
        }

        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(selectedItems.size)
        calculateTotals() // Add this line
    }


    // Add this method to ListPanenTPHAdapter class
    fun preSelectTphIds() {
        if (tphListScan.isNotEmpty()) {
            var selectionChanged = false

            // Find all matching items and add them to selectedItems
            tphList.forEachIndexed { index, item ->
                try {
                    val extractedData = extractData(item)
                    if (tphListScan.contains(extractedData.tphId.toString())) {
                        selectedItems.add(index)
                        selectionChanged = true
                    }
                } catch (e: Exception) {
                    Log.e("ListPanenTPHAdapter", "Error pre-selecting TPH: ${e.message}")
                }
            }

            if (selectionChanged) {
                notifyDataSetChanged()
                onSelectionChangeListener?.invoke(selectedItems.size)
                calculateTotals() // Add this line
            }
        }
    }


}