package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.PathWithInspectionRelations
import com.cbi.mobile_plantation.databinding.TableItemRowBinding
import com.cbi.mobile_plantation.ui.adapter.ListPanenTPHAdapter.ExtractedData
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class ListInspectionAdapter : RecyclerView.Adapter<ListInspectionAdapter.ViewHolder>() {

    private var tphList = mutableListOf<Map<String, Any>>()
    private var filteredList = mutableListOf<Map<String, Any>>()

    data class MappingData(
        val pathId: String,
        val tphId: Int,
        val createdDate: String,
    )

    private fun dataMapping(item: Map<String, Any>): MappingData {
        val idData = item["path_id"] as? String ?: "0"
        val tphId = item["tph_id"] as? String ?: "0"

        val dateCreated = item["created_date"] as? String ?: "-"
        val formattedTime = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("HH:mm", Locale("id", "ID")) // Indonesian format
            val date = inputFormat.parse(dateCreated)
            outputFormat.format(date ?: "-")
        } catch (e: Exception) {
            "-"
        }

        return MappingData(
            idData,
            tphId.toInt(),
            formattedTime,
        )
    }

    class ViewHolder(private val binding: TableItemRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            data: Map<String, Any>,
            mapData: (Map<String, Any>) -> MappingData,
        ) {
            val mappingData = mapData(data)

            binding.flCheckBoxItemTph.visibility = View.GONE
            binding.td1.visibility = View.VISIBLE
            binding.td2.visibility = View.VISIBLE
            binding.td3.visibility = View.VISIBLE

            binding.td1.text = mappingData.pathId
            binding.td2.text = mappingData.tphId.toString()
            binding.td3.text = mappingData.createdDate
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = TableItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredList[position]

        holder.bind(data = item, mapData = ::dataMapping)

        AppLogger.d("data item: $item")
    }

    override fun getItemCount() = filteredList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Map<String, Any>>) {
        tphList.clear()
        tphList.addAll(newData)
        filteredList = tphList.toMutableList()
        AppLogger.d("filteredList: $filteredList")

        notifyDataSetChanged()
    }

}