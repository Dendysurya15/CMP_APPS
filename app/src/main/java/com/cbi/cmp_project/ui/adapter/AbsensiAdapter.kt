package com.cbi.cmp_project.ui.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.databinding.TableItemRowAbsensiBinding
import com.cbi.cmp_project.databinding.TableItemRowBinding
import com.cbi.cmp_project.ui.viewModel.AbsensiViewModel
import com.cbi.cmp_project.utils.AppLogger

data class AbsensiDataList(
    val id: Int,
    val nama: String,
    val jabatan: String,
    var isChecked: Boolean = false
)

class AbsensiAdapter(
    private var items: MutableList<AbsensiDataList>) :
    RecyclerView.Adapter<AbsensiAdapter.ViewHolder>() {

    // ViewHolder untuk Absensi
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNama: TextView = itemView.findViewById(R.id.tvItemNama)
        val tvJabatan: TextView = itemView.findViewById(R.id.tvItemJabatan)
        val flCheckbox: CheckBox = itemView.findViewById(R.id.checkBoxAbsensi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_item_row_absensi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        // Bind data ke ViewHolder
        holder.tvNama.text = item.nama
        holder.tvJabatan.text = item.jabatan
        holder.flCheckbox.setOnCheckedChangeListener(null) // Reset listener untuk menghindari pemanggilan ulang saat rebind
        holder.flCheckbox.isChecked = item.isChecked

        holder.flCheckbox.setOnCheckedChangeListener { _, isChecked ->
            items[position].isChecked = isChecked // <-- Perbarui nilai isChecked langsung
        }

    }

    // ✅ Function to remove a worker by ID
    fun removeWorkerById(workerId: String) {
        AppLogger.d("Removing AbsensiDataList ID: $workerId")
        items.removeAll { it.id.toString() == workerId }
        notifyDataSetChanged()
    }


    fun updateList(newList: List<AbsensiDataList>, append: Boolean = true) {
        items = if (append) {
            (items + newList).distinctBy { it.id }.toMutableList()
            // Prevent duplicate entries based on unique `id`
        } else {
            newList.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun clearList() {
        items.clear()
        notifyDataSetChanged()
    }


    override fun getItemCount() = items.size

    // Fungsi untuk mendapatkan daftar absensi yang terbaru
    fun getItems(): List<AbsensiDataList> {
        return items
    }
}