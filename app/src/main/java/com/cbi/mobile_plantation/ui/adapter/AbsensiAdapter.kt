package com.cbi.mobile_plantation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R

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

    fun updateList(newList: List<AbsensiDataList>, append: Boolean = false) {
        items = if (append) {
            (items + newList).toMutableList()  // <-- Ubah ke MutableList agar bisa diubah
        } else {
            newList.toMutableList()  // <-- Pastikan tetap MutableList
        }
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    // Fungsi untuk mendapatkan daftar absensi yang terbaru
    fun getItems(): List<AbsensiDataList> {
        return items
    }
}