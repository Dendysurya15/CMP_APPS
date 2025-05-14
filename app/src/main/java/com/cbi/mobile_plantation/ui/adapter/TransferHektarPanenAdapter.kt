package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.view.panenTBS.ListPanenTBSActivity
import java.text.SimpleDateFormat
import java.util.Locale

data class TransferHektarPanenData(
    val time: String,
    val blok: String,
    val janjang: String,
    val noTph: String,
    val namaPemanen: String,
    val status_scan: Int?,
    val id: Int?
)

class TransferHektarPanenAdapter(private var items: List<TransferHektarPanenData>, private val context: Activity) :
    RecyclerView.Adapter<TransferHektarPanenAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val constraint_table_item_row: ConstraintLayout = view.findViewById(R.id.constraint_table_item_row)
        val td1: TextView = view.findViewById(R.id.td1)
        val td2: TextView = view.findViewById(R.id.td2)
        val td3: TextView = view.findViewById(R.id.td3)
        val td4: TextView = view.findViewById(R.id.td4)
        val td5: LinearLayout = view.findViewById(R.id.td5) // Change to LinearLayout
        val td6: LinearLayout = view.findViewById(R.id.td6) // Change to LinearLayout
        val td7: TextView = view.findViewById(R.id.td7)
        val checkbox: CheckBox = view.findViewById(R.id.checkBoxPanen) // Add this
        val flCheckBoxItemTph = view.findViewById<FrameLayout>(R.id.flCheckBoxItemTph)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_item_row, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.flCheckBoxItemTph.visibility = View.GONE
        holder.td1.visibility = View.VISIBLE
        holder.td2.visibility = View.VISIBLE
        holder.td3.visibility = View.VISIBLE
        holder.td4.visibility = View.VISIBLE
        holder.td5.visibility = View.GONE
        holder.td6.visibility = View.GONE
        holder.td7.visibility = View.VISIBLE

        holder.td1.text = formatToIndonesianDateTime(item.time)
        holder.td2.text = item.blok
        holder.td3.text = item.janjang
        holder.td4.text = item.namaPemanen
        holder.td7.text = item.noTph

//        holder.constraint_table_item_row.setOnClickListener {
//            val intent = Intent(context, ListPanenTBSActivity::class.java).putExtra("FEATURE_NAME", "Detail eSPB").putExtra("id_espb", "${item.id}")
//            context.startActivity(intent)
//            (context).overridePendingTransition(0, 0)
//        }
//
//        val layoutParamsTd5 = holder.td5.layoutParams as LinearLayout.LayoutParams
//        layoutParamsTd5.weight = 0.3f
//        holder.td5.layoutParams = layoutParamsTd5
//        val layoutParamsTd6 = holder.td6.layoutParams as LinearLayout.LayoutParams
//        layoutParamsTd6.weight = 0.3f
//        holder.td6.layoutParams = layoutParamsTd6
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    fun updateList(newList: List<TransferHektarPanenData>) {
        items = newList
        notifyDataSetChanged()
    }

    fun formatToIndonesianDateTime(dateTimeStr: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateTimeStr) ?: return dateTimeStr
            val outputFormat = SimpleDateFormat("d MMM yy\nHH:mm", Locale("id"))
            return outputFormat.format(date)
        } catch (e: Exception) {
            return dateTimeStr
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Map<String, Any>>) {

//        preselectedTphIds.clear()
//        preselectedTphIds.addAll(tphListScan)
//
//        tphList.clear()
//        selectedItems.clear()
//        selectAllState = false
//        isSortAscending = null
//        tphList.addAll(newData)
//        manuallyDeselectedItems.clear() // Add this line
//        filteredList = tphList.toMutableList()
//
//        // Pre-select items that match the scanned TPH IDs
//        if (preselectedTphIds.isNotEmpty()) {
//            tphList.forEachIndexed { index, item ->
//                try {
//                    val tphId = item["tph_id"].toString()
//                    if (preselectedTphIds.contains(tphId)) {
//                        selectedItems.add(index)
//                    }
//                } catch (e: Exception) {
//                    Log.e("ListPanenTPHAdapter", "Error pre-selecting TPH: ${e.message}")
//                }
//            }
//        }

        notifyDataSetChanged()
//        onSelectionChangeListener?.invoke(selectedItems.size)
//        calculateTotals() // Add this line
    }

    override fun getItemCount() = items.size
}