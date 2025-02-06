package com.cbi.cmp_project.ui.adapter

import android.os.Handler
import android.os.Looper
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
        TIME
    }

    private var currentSortField: SortField = SortField.TPH
    private var tphList = mutableListOf<Map<String, Any>>()
    private var filteredList = mutableListOf<Map<String, Any>>()
    private var currentArchiveState: Int = 0
    private var areCheckboxesEnabled = true
    private val selectedItems = mutableSetOf<Int>()
    private var isSortAscending: Boolean? = null // null means no sorting applied
    private var selectAllState = false

    private var onSelectionChangeListener: ((Int) -> Unit)? = null

    data class ExtractedData(
        val gradingText: String,
        val blokText: String,
        val tanggalText: String,
        val tphText: String,
        val searchableText: String,
    )

    fun extractData(item: Map<String, Any>): ExtractedData {
        val blokName = item["blok_name"] as? String ?: "-"
        val noTPH = item["tph_name"] as? String ?: "-"
        val dateCreated = item["date_created"] as? String ?: "-"

        val jjgJsonString = item["jjg_json"] as? String ?: "{}"
        val jjgJson = try {
            JSONObject(jjgJsonString)
        } catch (e: JSONException) {
            JSONObject()
        }

        val totalJjg = jjgJson.optInt("TO", 0)


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

        return ExtractedData(gradingText, blokText, formattedTime,noTPHText, searchableText)
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
            val extractedData = extractData(item)
            when (field) {
                SortField.TPH -> extractedData.tphText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                SortField.BLOK -> extractedData.blokText
                SortField.GRADING -> extractedData.gradingText.toIntOrNull() ?: 0
                SortField.TIME -> extractedData.tanggalText
            }
        }.let { comparator ->
            if (!ascending) comparator.reversed() else comparator
        }).toMutableList()

        notifyDataSetChanged()
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
            extractData: (Map<String, Any>) -> ExtractedData
        ) {


            val extractedData = extractData(data)
            binding.tvItemBlok.text = extractedData.blokText
            binding.tvItemTPH.text = extractedData.tphText
            binding.tvItemGrading.text = extractedData.gradingText
            binding.tvItemJam.text = extractedData.tanggalText


            if (archiveState == 1) {
                binding.checkBoxPanen.visibility = View.GONE
                binding.numListTerupload.visibility = View.VISIBLE
                binding.numListTerupload.text = "${adapterPosition + 1}."
            } else {
                binding.checkBoxPanen.visibility = View.VISIBLE
                binding.numListTerupload.visibility = View.GONE
                binding.checkBoxPanen.isChecked = isSelected
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