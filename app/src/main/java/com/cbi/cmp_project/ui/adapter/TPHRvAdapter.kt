package com.cbi.cmp_project.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.model.TphRvData

class TPHRvAdapter(private var items: List<TphRvData>) :
    RecyclerView.Adapter<TPHRvAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBlok: TextView = view.findViewById(R.id.tvItemBlok)
        val tvTPH: TextView = view.findViewById(R.id.tvItemTPH)
        val tvJjg: TextView = view.findViewById(R.id.tvItemGrading)
        val tvTime: TextView = view.findViewById(R.id.tvItemJam)
        val flCheckBoxItemTph: FrameLayout = view.findViewById(R.id.flCheckBoxItemTph)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvBlok.text = item.namaBlok
        holder.tvTPH.text = item.noTPH.toString()
        holder.tvJjg.text = item.jjg.toString()
        holder.tvTime.text = item.time
        holder.flCheckBoxItemTph.visibility = View.GONE
    }

    fun updateList(newList: List<TphRvData>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
}