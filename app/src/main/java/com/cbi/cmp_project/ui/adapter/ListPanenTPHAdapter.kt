package com.cbi.cmp_project.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.databinding.TableItemRowBinding
import com.cbi.cmp_project.utils.AppLogger
import org.json.JSONException
import org.json.JSONObject
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
        val panenId: Int
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

        val totalJjg =   if (featureName=="Buat eSPB"){
            jjgJson.optInt("KP", 0)
        }else{
            jjgJson.optInt("TO", 0) //diganti KP
        }

        val formattedTime = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("HH:mm", Locale("id", "ID")) // Indonesian format
            val date = inputFormat.parse(dateCreated)
            outputFormat.format(date ?: "-")
        } catch (e: Exception) {
            "-"
        }

        val blokText = "$blokName"
        val noTPHText = noTPH
        val gradingText = "$totalJjg"
        val searchableText = "$blokText $noTPHText $gradingText $formattedTime"

        return ExtractedData(gradingText, blokText, formattedTime,noTPHText, searchableText, tphId.toInt(), panenId.toInt())
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
                SortField.TPH -> extractData(item).tphText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
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
        // In ListPanenTPHAdapter.kt, update the bind method in ListPanenTPHViewHolder class:

        // In the ListPanenTPHAdapter.kt, modify the ViewHolder bind method:

        fun bind(
            data: Map<String, Any>,
            isSelected: Boolean,
            archiveState: Int,
            onCheckedChange: (Boolean) -> Unit,
            extractData: (Map<String, Any>) -> ExtractedData,
            featureName: String = "",
            tphListScan: List<String> = emptyList(),
            isScannedItem: Boolean = false
        ) {
            val extractedData = extractData(data)

            // Set cell content
            binding.td1.visibility = View.VISIBLE
            binding.td2.visibility = View.VISIBLE
            binding.td3.visibility = View.VISIBLE
            binding.td4.visibility = View.VISIBLE
            binding.td1.text = extractedData.blokText
            binding.td2.text = extractedData.tphText
            binding.td3.text = extractedData.gradingText
            binding.td4.text = extractedData.tanggalText

            if (archiveState == 1 || featureName == "Rekap panen dan restan") {
                binding.checkBoxPanen.visibility = View.GONE
                binding.numListTerupload.visibility = View.VISIBLE
                binding.numListTerupload.text = "${adapterPosition + 1}."
            } else {
                binding.checkBoxPanen.visibility = View.VISIBLE
                binding.numListTerupload.visibility = View.GONE

                // IMPORTANT: Remove listener before setting state
                binding.checkBoxPanen.setOnCheckedChangeListener(null)

                // Set state based on selection and whether it's from a scan
                binding.checkBoxPanen.isChecked = isSelected
                binding.checkBoxPanen.isEnabled = !isScannedItem

                // Add listener AFTER setting state
                binding.checkBoxPanen.setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange(isChecked)
                }
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

    override fun onBindViewHolder(holder: ListPanenTPHViewHolder, position: Int) {
        val item = filteredList[position]
        val tphId = extractData(item).tphId.toString()
        val isScannedItem = tphListScan.contains(tphId)
        val originalPosition = tphList.indexOf(item)

        // Save scanned TPH IDs in our set for quick lookup
        if (isScannedItem) {
            scannedTphIdsSet.add(tphId)
        }

        // Setup the view holder with correct state
        holder.bind(
            data = item,
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

//    fun updateData(newData: List<Map<String, Any>>) {
//        tphList.clear()
//        selectedItems.clear()
//        selectAllState = false
//        isSortAscending = null  // Add this line
//        tphList.addAll(newData)
//        filteredList = tphList.toMutableList()
//        notifyDataSetChanged()
//        onSelectionChangeListener?.invoke(0)
//    }


    // Then modify the updateData method to preserve selections from scanned items
    fun updateData(newData: List<Map<String, Any>>) {
        // Keep track of tphListScan before clearing selections
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