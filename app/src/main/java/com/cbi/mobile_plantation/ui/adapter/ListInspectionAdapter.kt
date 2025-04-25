package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.data.model.PathWithInspectionTphRelations
import com.cbi.mobile_plantation.databinding.TableItemRowBinding
import com.cbi.mobile_plantation.utils.AppLogger
import java.text.SimpleDateFormat
import java.util.Locale

class ListInspectionAdapter(
    private val onItemClick: (PathWithInspectionTphRelations) -> Unit,
    private val onCheckboxChanged: (List<String>) -> Unit
) : RecyclerView.Adapter<ListInspectionAdapter.InspectionDataViewHolder>() {

    private var inspectionPaths: List<PathWithInspectionTphRelations> = emptyList()
    private val selectedIds = mutableSetOf<String>()
    private var currentState = 0

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<PathWithInspectionTphRelations>) {
        inspectionPaths = data
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun toggleSelectAll(isSelected: Boolean) {
        if (isSelected) {
            inspectionPaths.forEach { selectedIds.add(it.getPathId()) }
        } else {
            selectedIds.clear()
        }
        notifyDataSetChanged()

        onCheckboxChanged(selectedIds.toList())
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateCurrentState(state: Int) {
        currentState = state
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspectionDataViewHolder {
        val binding = TableItemRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InspectionDataViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InspectionDataViewHolder, position: Int) {
        holder.bind(inspectionPaths[position])
    }

    override fun getItemCount(): Int = inspectionPaths.size

    inner class InspectionDataViewHolder(
        private val binding: TableItemRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(inspectionPaths[position])
                }
            }

            binding.checkBoxPanen.setOnCheckedChangeListener { _, isChecked ->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val id = inspectionPaths[position].getPathId()
                    if (isChecked) {
                        selectedIds.add(id)
                    } else {
                        selectedIds.remove(id)
                    }
                    onCheckboxChanged(selectedIds.toList())
                }
            }
        }

        fun bind(item: PathWithInspectionTphRelations) {
            binding.apply {
                binding.td1.visibility = View.VISIBLE
                binding.td2.visibility = View.VISIBLE
                binding.td3.visibility = View.VISIBLE

                binding.td1.text = item.getBlok()
                binding.td2.text = item.getTotalData().toString()

                val formattedTime = try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                    val outputFormat = SimpleDateFormat("HH:mm", Locale("id", "ID")) // Indonesian format
                    val date = inputFormat.parse(item.getCreatedDate())
                    outputFormat.format(date ?: "-")
                } catch (e: Exception) {
                    "-"
                }
                binding.td3.text = formattedTime

                flCheckBoxItemTph.visibility = if (currentState == 0) View.VISIBLE else View.GONE
                checkBoxPanen.setOnCheckedChangeListener(null)
                checkBoxPanen.isChecked = selectedIds.contains(item.getPathId())
                checkBoxPanen.setOnCheckedChangeListener { _, isChecked ->
                    val id = item.getPathId()
                    if (isChecked) {
                        selectedIds.add(id)
                    } else {
                        selectedIds.remove(id)
                    }
                    onCheckboxChanged(selectedIds.toList())
                }
            }
        }
    }
}