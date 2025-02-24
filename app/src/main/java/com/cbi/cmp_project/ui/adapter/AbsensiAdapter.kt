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
import com.cbi.cmp_project.ui.adapter.ListPanenTPHAdapter.ListPanenTPHViewHolder
import com.cbi.cmp_project.ui.adapter.WBData
import com.cbi.cmp_project.ui.viewModel.AbsensiViewModel

data class AbsensiDataList(
    val nama: String,
    val jabatan: String
)

class AbsensiAdapter(
    private var items: List<AbsensiDataList>) :
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
//        holder.flCheckbox.isChecked = item.isChecked

        // Event listener untuk perubahan checkbox
//        holder.flCheckbox.setOnCheckedChangeListener { _, isChecked ->
//            // Logika atau callback jika diperlukan
////            items[position].isChecked = isChecked
//            onCheckedChange(position, isChecked)
//        }
    }

    fun updateList(newList: List<AbsensiDataList>) {
        items = newList
        notifyDataSetChanged()
    }
    override fun getItemCount() = items.size
}