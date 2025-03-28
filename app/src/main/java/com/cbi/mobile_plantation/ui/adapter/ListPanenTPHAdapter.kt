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
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.databinding.TableItemRowBinding
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
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

    private var currentSortField: SortField = SortField.TPH
    private var tphList = mutableListOf<Map<String, Any>>()
    private var filteredList = mutableListOf<Map<String, Any>>()
    private var currentArchiveState: Int = 0
    private var areCheckboxesEnabled = true
    private val selectedItems = mutableSetOf<Int>()
    private var isSortAscending: Boolean? = null
    private var selectAllState = false
    private val preselectedTphIds = mutableSetOf<String>()
    private val scannedTphIdsSet = mutableSetOf<String>()

    private var featureName: String = ""
    private var tphListScan: List<String> = emptyList()

    private var onSelectionChangeListener: ((Int) -> Unit)? = null

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

    // Then modify setFeatureAndScanned to call this method:
    fun setFeatureAndScanned(feature: String, scannedResult: String) {
        featureName = feature
        Log.d("ListPanenTPHAdapterTest", "featureName: $featureName")

        tphListScan = try {
            val tphString = scannedResult
                .removePrefix("""{"tph":"""")
                .removeSuffix(""""}""")
            tphString.split(";")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
        Log.d("ListPanenTPHAdapterTest", "tphListScan: $tphListScan")

        // Pre-select TPH IDs that match scanned list
        preSelectTphIds()

        notifyDataSetChanged()
    }


    fun extractData(item: Map<String, Any>): ExtractedData {
        Log.d("ListPanenTPHAdapterTest", "extractData: $item")


        AppLogger.d(item.toString())
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

        val totalJjg = if (featureName == "Buat eSPB" || featureName == "Rekap panen dan restan" || featureName == AppUtils.ListFeatureNames.DetailESPB) {
            jjgJson.optInt("KP", 0)
        } else {
            jjgJson.optInt("TO", 0) //diganti KP
        }

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

    class ListPanenTPHViewHolder(private val binding: TableItemRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            data: Map<String, Any>,
            context: android.content.Context,
            isSelected: Boolean,
            archiveState: Int,
            onCheckedChange: (Boolean) -> Unit,
            extractData: (Map<String, Any>) -> ExtractedData,
            featureName: String = "",
            tphListScan: List<String> = emptyList(),
            isScannedItem: Boolean = false
        ) {

            val extractedData = extractData(data)
            binding.td1.visibility = View.VISIBLE
            binding.td2.visibility = View.VISIBLE
            binding.td3.visibility = View.VISIBLE
            binding.td4.visibility = View.VISIBLE

            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen && archiveState == 2) {
                binding.td1.text = data["nama_karyawans"].toString()
                binding.td2.text = data["jjg_each_blok"].toString()
                binding.td3.text = data["tph_count"].toString()
                binding.td4.text = "${data["jjg_total_blok"]} (${data["jjg_dibayar"]})"

                itemView.setOnClickListener(null)
                itemView.isClickable = false
                itemView.isFocusable = false
            }
            else if(featureName == AppUtils.ListFeatureNames.BuatESPB ){
                binding.td5.visibility = View.VISIBLE
                val params = binding.td5.layoutParams as LinearLayout.LayoutParams
                params.weight = 0.2f
                binding.td5.layoutParams = params

                // Set a different color based on username
                val username = extractedData.username
                val color = ListPanenTPHAdapter().getUsernameColor(username, context)
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

                // Create the ColorStateList with the appropriate checked color
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

                if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen && archiveState == 0 || featureName == AppUtils.ListFeatureNames.RekapHasilPanen && archiveState == 1) {
                    AppLogger.d(archiveState.toString())
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

                        val recyclerView = view.findViewById<RecyclerView>(R.id.rvPhotoAttachments)

                        if (recyclerView != null) {

                            val recyclerView =
                                view.findViewById<RecyclerView>(R.id.rvPhotoAttachments)

                            // Disable scrolling with custom layout manager
                            val nonScrollLayoutManager =
                                object : LinearLayoutManager(context, HORIZONTAL, false) {
                                    override fun canScrollHorizontally(): Boolean {
                                        return false // Disable horizontal scrolling
                                    }
                                }
                            recyclerView.layoutManager = nonScrollLayoutManager

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

                            // Disable overscroll effect
                            recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

                            // Get photo URLs from item["foto"] using the updated function
                            val photoUrls = getPhotoUrlsFromItem(data, context)

                            // Create adapter - always shows 3 items
                            val photoAdapter =
                                PhotoAttachmentAdapterDetailTable(photoUrls) { position ->
                                    // Handle photo click - show fullscreen
                                    if (position < photoUrls.size) {
                                        // Check if file exists before showing
                                        val photoFile = File(photoUrls[position])
                                        if (photoFile.exists()) {
                                            // Show fullscreen photo
                                            showFullscreenImage(context, photoFile)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Photo file not found",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
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

                        val infoItems = listOf(
                            DetailInfoType.TANGGAL_BUAT to formattedDate,
                            DetailInfoType.BLOK_BANJIR to when(data["blok_banjir"]) {
                                "1" -> "Tidak"
                                else -> "Ya"
                            },
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
                            setInfoItemValues(itemView, type.label, value.toString())
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
                    AppLogger.d(archiveState.toString())
                    itemView.setOnClickListener(null)
                    itemView.isClickable = false
                    itemView.isFocusable = false
                }
            }


            if (archiveState == 1 || featureName == "Rekap panen dan restan" || featureName == "Detail eSPB") {
                binding.checkBoxPanen.visibility = View.GONE
                binding.numListTerupload.visibility = View.VISIBLE
                binding.numListTerupload.text = "${adapterPosition + 1}."
            } else if (archiveState == 2 && featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                binding.checkBoxPanen.visibility = View.GONE
                binding.numListTerupload.visibility = View.GONE
                binding.flCheckBoxItemTph.visibility = View.GONE
            } else {
                binding.checkBoxPanen.visibility = View.VISIBLE
                binding.numListTerupload.visibility = View.GONE
                binding.checkBoxPanen.setOnCheckedChangeListener(null)
                binding.checkBoxPanen.isChecked = isSelected
                binding.checkBoxPanen.isEnabled = !isScannedItem

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
            }
        }

        @SuppressLint("SetTextI18n")
        private fun setInfoItemValues(view: View, label: String, value: String) {
            val textViewLabel = view.findViewById<TextView>(R.id.tvLabel)
            val textViewValue = view.findViewById<TextView>(R.id.tvValue)

            textViewLabel?.text = label

            if (label == DetailInfoType.KEMANDORAN.label) {
                textViewValue?.text = value
            } else {
                textViewValue?.text = ": $value"
            }

            if (label in listOf(
                    DetailInfoType.TOTAL_JANJANG.label,
                    DetailInfoType.TOTAL_DIKIRIM_KE_PABRIK.label,
                    DetailInfoType.TOTAL_JANJANG_DI_BAYAR.label
                )
            ) {
                textViewLabel?.setTypeface(null, Typeface.BOLD)
                textViewValue?.setTypeface(null, Typeface.BOLD)
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
                .take(3) // Limit to maximum 3 photos
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

    // Finally update getSelectedItems to include both manually selected AND scanned items
    fun getSelectedItems(): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()

        // Get manually selected items
        for (position in selectedItems) {
            tphList.getOrNull(position)?.let { result.add(it) }
        }

        // Add all scanned items
        for (item in tphList) {
            val tphId = extractData(item).tphId.toString()
            if (tphListScan.contains(tphId)) {
                result.add(item)
            }
        }

        // Return distinct items to avoid duplicates
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
        }
    }

    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ListPanenTPHViewHolder, position: Int) {
        val item = filteredList[position]


        val tphId = extractData(item).tphId.toString()
        val isScannedItem = tphListScan.contains(tphId)
        val originalPosition = tphList.indexOf(item)

        // Save scanned TPH IDs in our set for quick lookup
        if (isScannedItem) {
            scannedTphIdsSet.add(tphId)
        }

        holder.bind(
            data = item,
            context = holder.itemView.context,
            isSelected = selectedItems.contains(originalPosition) || isScannedItem,
            archiveState = currentArchiveState,
            onCheckedChange = { isChecked ->
                val origPos = tphList.indexOf(item)
                if (!isScannedItem) { // Only modify selection for non-scanned items
                    if (isChecked) {
                        selectedItems.add(origPos)
                    } else {
                        selectedItems.remove(origPos)
                        selectAllState = false
                    }
                    onSelectionChangeListener?.invoke(selectedItems.size)
                }
            },
            extractData = ::extractData,
            featureName = featureName,
            isScannedItem = isScannedItem
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
        NAMA_PEMANEN(R.id.DataNamaPemanen, "Nama Pemanen"),
        TOTAL_JANJANG(R.id.DataTotalJanjang, "Total Janjang"),
        TOTAL_DIKIRIM_KE_PABRIK(R.id.DataDikirimKePabrik, "Dikirim Ke Pabrik"),
        TOTAL_JANJANG_DI_BAYAR(R.id.DataJanjangDibayar, "Janjang DiBayar"),
        TOTAL_DATA_BUAH_MENTAH(R.id.DataBuahMentah, "Buah Mentah"),
        TOTAL_DATA_LEWAT_MASAK(R.id.DataBuahLewatMasak, "Buah Lewat Masak"),
        TOTAL_DATA_JJG_KOSONG(R.id.DataBuahJjgKosong, "Janjang Kosong"),
        TOTAL_DATA_ABNORMAL(R.id.DataBuahAbnormal, "Buah Abnormal"),
        TOTAL_DATA_SERANGAN_TIKUS(R.id.DataBuahSeranganTikus, "Serangan Tikus"),
        TOTAL_DATA_TANGKAI_PANJANG(R.id.DataBuahTangkaiPanjang, "Tangkai Panjang"),
        TOTAL_DATA_TIDAK_VCUT(R.id.DataBuahTidakVcut, "Tidak V-Cut"),
    }

    fun setOnSelectionChangedListener(listener: (Int) -> Unit) {
        onSelectionChangeListener = listener
    }

    fun getUsernameColor(username: String, context: Context): Int {
        // Create a deterministic hash of the username to ensure the same user always gets the same color
        val hash = username.hashCode()

        // Define a set of predefined colors - you can customize these
        val colors = arrayOf(
            context.resources.getColor(R.color.bluedark),
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#FF5722"), // Deep Orange
            Color.parseColor("#607D8B")  // Blue Grey
        )

        // Use the hash to select a color
        val colorIndex = Math.abs(hash) % colors.size
        return colors[colorIndex]
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
            }
        }
    }


}