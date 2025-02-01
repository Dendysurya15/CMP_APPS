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
    private var tphList = mutableListOf<Map<String, Any>>()
    private var filteredList = mutableListOf<Map<String, Any>>()
    private var currentArchiveState: Int = 0
    private var areCheckboxesEnabled = true
    private val selectedItems = mutableSetOf<Int>()
    private var isSortAscending: Boolean? = null // null means no sorting applied
    private var selectAllState = false

    private var onSelectionChangeListener: ((Int) -> Unit)? = null

    fun filterData(query: String) {
        // First apply the filter
        filteredList = if (query.isEmpty()) {
            tphList.toMutableList()
        } else {
            tphList.filter { item ->
                val deptName = item["dept_name"] as? String ?: "-"
                val divisiName = item["divisi_name"] as? String ?: "-"
                val blokName = item["blok_name"] as? String ?: "-"
                val tphName = item["tph_name"] as? String ?: "-"

                // Extract jjg_json values
                val jjgJsonString = item["jjg_json"] as? String ?: "{}"
                val jjgJson = try {
                    JSONObject(jjgJsonString)
                } catch (e: JSONException) {
                    JSONObject()
                }
                val ri = jjgJson.optInt("RI", 0)
                val kp = jjgJson.optInt("KP", 0)
                val pa = jjgJson.optInt("PA", 0)

                val searchableText = "$deptName $divisiName $blokName Nomor $tphName Masak: $ri Kirim Pabrik: $kp TBS dibayar: $pa"
                searchableText.contains(query, ignoreCase = true)
            }.toMutableList()
        }

        // Then apply the sort if it exists
        isSortAscending?.let { ascending ->
            filteredList = if (ascending) {
                filteredList.sortedBy { it["tph_name"] as? String ?: "-" }.toMutableList()
            } else {
                filteredList.sortedByDescending { it["tph_name"] as? String ?: "-" }.toMutableList()
            }
        }

        selectedItems.clear()
        selectAllState = false
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(0)
    }

    fun sortData(ascending: Boolean) {
        isSortAscending = ascending

        filteredList = if (ascending) {
            filteredList.sortedBy { it["tph_name"] as? String ?: "-" }.toMutableList()
        } else {
            filteredList.sortedByDescending { it["tph_name"] as? String ?: "-" }.toMutableList()
        }

        notifyDataSetChanged()
    }

    fun resetSort() {
        isSortAscending = null
        notifyDataSetChanged()
    }
    class ListPanenTPHViewHolder(private val binding: TableItemRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            data: Map<String, Any>,
            isSelected: Boolean,
            archiveState: Int,
            onCheckedChange: (Boolean) -> Unit,
            checkboxEnabled: Boolean
        ) {


            AppLogger.d(data.toString())

            // Extract jjg_json and parse it
            val jjgJsonString = data["jjg_json"] as? String ?: "{}"
            val jjgJson = try {
                JSONObject(jjgJsonString)
            } catch (e: JSONException) {
                JSONObject()
            }

            val ri = jjgJson.optInt("RI", 0)
            val kp = jjgJson.optInt("KP", 0)
            val pa = jjgJson.optInt("PA", 0)

// Set the tvItemGrading text with the desired format
            binding.tvItemGrading.text = "Masak: $ri\nKirim Pabrik: $kp\nTBS dibayar: $pa"

            val deptName = data["dept_name"] as? String ?: "-"
            val divisiName = data["divisi_name"] as? String ?: "-"
            val blokName = data["blok_name"] as? String ?: "-"
            val tphName = data["tph_name"] as? String ?: "-"

            binding.tvItemBlok.text = "$deptName $divisiName\n$blokName\nNomor $tphName"

            if (archiveState == 1) {
                binding.checkBoxPanen.visibility = View.GONE
                binding.numListTerupload.visibility = View.VISIBLE
                binding.numListTerupload.text = "${adapterPosition + 1}."
            } else {
                binding.checkBoxPanen.visibility = View.VISIBLE
                binding.numListTerupload.visibility = View.GONE
                binding.checkBoxPanen.isChecked = isSelected
                binding.checkBoxPanen.isEnabled = checkboxEnabled
                binding.checkBoxPanen.setOnCheckedChangeListener { _, isChecked ->
                    if (checkboxEnabled) {  // Only trigger if enabled
                        onCheckedChange(isChecked)
                    }
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
        areCheckboxesEnabled = !select  // Disable checkboxes when selecting all
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
            areCheckboxesEnabled
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