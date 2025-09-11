package com.cbi.mobile_plantation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.view.ListTPHApproval

class ReceiveDataBTHektaranAdapter(
    private val dataList: MutableList<ListTPHApproval.BluetoothDataItem>
) : RecyclerView.Adapter<ReceiveDataBTHektaranAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSenderName: TextView = itemView.findViewById(R.id.tvSenderName)
        val tvDataPreview: TextView = itemView.findViewById(R.id.tvDataPreview)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receive_bluetooth_data, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]

        holder.tvSenderName.text = "Dari: ${item.senderName}"
        holder.tvTimestamp.text = item.timestamp

        // Show preview of received data
        try {
            val jsonObject = org.json.JSONObject(item.jsonData)
            val username = jsonObject.optString("username", "Unknown")
            val kemandoranId = jsonObject.optString("kemandoran_id", "Unknown")
            val tphData = jsonObject.optString("tph_0", "")
            val dataCount = if (tphData.isNotEmpty()) tphData.split(";").size - 1 else 0

            holder.tvDataPreview.text = "User: $username\nKemandoran: $kemandoranId\nData TPH: $dataCount items"
        } catch (e: Exception) {
            holder.tvDataPreview.text = "Data preview error"
        }
    }

    override fun getItemCount(): Int = dataList.size
}