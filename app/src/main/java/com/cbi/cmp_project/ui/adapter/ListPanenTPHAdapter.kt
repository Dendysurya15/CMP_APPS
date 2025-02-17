package com.cbi.cmp_project.ui.adapter

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

    private var featureName: String = ""
    private var tphListScan: List<String> = emptyList()

    private var onSelectionChangeListener: ((Int) -> Unit)? = null

    data class ExtractedData(
        val gradingText: String,
        val blokText: String,
        val tanggalText: String,
        val tphText: String,
        val searchableText: String,
        val tphId: Int
    )

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
        notifyDataSetChanged() // Only call this if you need to refresh the views
    }


    fun extractData(item: Map<String, Any>): ExtractedData {
        Log.d("ListPanenTPHAdapterTest", "extractData: $item")


        AppLogger.d(item.toString())
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

        return ExtractedData(gradingText, blokText, formattedTime,noTPHText, searchableText, tphId.toInt())
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
        fun bind(
            data: Map<String, Any>,
            isSelected: Boolean,
            archiveState: Int,
            onCheckedChange: (Boolean) -> Unit,
//            checkboxEnabled: Boolean,
            extractData: (Map<String, Any>) -> ExtractedData,
            featureName: String = "",
            tphListScan: List<String> = emptyList()
        ) {
            val extractedData = extractData(data)
            Log.d("ListPanenTPHAdapterTest", "bind: $extractedData")
            binding.tvItemBlok.text = extractedData.blokText
            binding.tvItemTPH.text = extractedData.tphText
            binding.tvItemGrading.text = extractedData.gradingText
            binding.tvItemJam.text = extractedData.tanggalText

            if (archiveState == 1) {
                Log.d("ListPanenTPHAdapterTest", "archiveState == 1")

                binding.checkBoxPanen.visibility = View.GONE
                binding.numListTerupload.visibility = View.VISIBLE
                binding.numListTerupload.text = "${adapterPosition + 1}."
            } else {
                Log.d("ListPanenTPHAdapterTest", "archiveState == else")
                binding.checkBoxPanen.visibility = View.VISIBLE
                binding.numListTerupload.visibility = View.GONE
                if (tphListScan.isNotEmpty()){
                    Log.d("ListPanenTPHAdapterTest", "tphListScan.isNotEmpty()")
                    tphListScan.forEach { scan ->
                        Log.d("ListPanenTPHAdapterTest", "scan: $scan")
                        Log.d("ListPanenTPHAdapterTest", "tphId: ${extractedData.tphId}")
                        if (extractedData.tphId.toString() == scan) {
                            Log.d("ListPanenTPHAdapterTest", "scan: COCOK")
                            binding.checkBoxPanen.isChecked = true
                            binding.checkBoxPanen.isEnabled = false
                        }
                    }
                }
//                binding.checkBoxPanen.isChecked = isSelected
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
        holder.bind(
            filteredList[position],
            selectedItems.contains(tphList.indexOf(filteredList[position])), // Modified to handle filtered list
            currentArchiveState,
            { isChecked ->
                val originalPosition = tphList.indexOf(filteredList[position])
                if (isChecked) {
                    selectedItems.add(originalPosition)
                } else {
                    selectedItems.remove(originalPosition)
                    selectAllState = false
                }
                onSelectionChangeListener?.invoke(selectedItems.size)
                // Optionally re-sort the list when selection changes
                if (currentSortField == SortField.CHECKED) {
                    sortByCheckedItems(isSortAscending ?: true)
                }
            },
            extractData = ::extractData,
            featureName = featureName,
            tphListScan = tphListScan
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

    fun updateData(newData: List<Map<String, Any>>) {
        tphList.clear()
        selectedItems.clear()
        selectAllState = false
        isSortAscending = null  // Add this line
        tphList.addAll(newData)
        filteredList = tphList.toMutableList()
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(0)
    }

}