package com.cbi.mobile_plantation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.TphRvData

class TPHRvAdapter(private var items: List<TphRvData>) :
    RecyclerView.Adapter<TPHRvAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val td1: TextView = view.findViewById(R.id.td1)
        val td2: TextView = view.findViewById(R.id.td2)
        val td3: TextView = view.findViewById(R.id.td3)
        val td4: TextView = view.findViewById(R.id.td4)
        val flCheckBoxItemTph: FrameLayout = view.findViewById(R.id.flCheckBoxItemTph)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.td1.visibility = View.VISIBLE
        holder.td2.visibility = View.VISIBLE
        holder.td3.visibility = View.VISIBLE
        holder.td4.visibility = View.VISIBLE

        holder.td1.text = item.namaBlok
        holder.td2.text = item.noTPH.toString()
        holder.td3.text = item.jjg.toString()
        holder.td4.text = item.time
        holder.flCheckBoxItemTph.visibility = View.GONE
    }

    fun updateList(newList: List<TphRvData>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
}