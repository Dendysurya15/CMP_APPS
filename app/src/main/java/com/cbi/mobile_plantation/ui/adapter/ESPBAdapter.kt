package com.cbi.mobile_plantation.ui.adapter

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
import java.text.SimpleDateFormat
import java.util.Locale

data class ESPBData(
    val time: String,
    val blok: String,
    val janjang: String,
    val tphCount: String,
    val status_mekanisasi: Int?,
    val status_scan: Int?,
    val id: Int?
)

class ESPBAdapter(private var items: List<ESPBData>, private val context: Activity) :
    RecyclerView.Adapter<ESPBAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val constraint_table_item_row: ConstraintLayout = view.findViewById(R.id.constraint_table_item_row)
        val td1: TextView = view.findViewById(R.id.td1)
        val td2: TextView = view.findViewById(R.id.td2)
        val td3: TextView = view.findViewById(R.id.td3)
        val td4: TextView = view.findViewById(R.id.td4)
        val td5: LinearLayout = view.findViewById(R.id.td5) // Change to LinearLayout
        val td6: LinearLayout = view.findViewById(R.id.td6) // Change to LinearLayout
        val checkbox: CheckBox = view.findViewById(R.id.checkBoxPanen) // Add this
        val flCheckBoxItemTph = view.findViewById<FrameLayout>(R.id.flCheckBoxItemTph)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.flCheckBoxItemTph.visibility = View.GONE
        holder.td1.visibility = View.VISIBLE
        holder.td2.visibility = View.VISIBLE
        holder.td3.visibility = View.VISIBLE
        holder.td4.visibility = View.VISIBLE
        holder.td5.visibility = View.VISIBLE
        holder.td6.visibility = View.VISIBLE

        holder.td1.text = formatToIndonesianDateTime(item.time)
        holder.td2.text = item.blok
        holder.td3.text = item.janjang
        holder.td4.text = item.tphCount

        holder.constraint_table_item_row.setOnClickListener {
            val intent = Intent(context, ListPanenTBSActivity::class.java).putExtra("FEATURE_NAME", "Detail eSPB").putExtra("id_espb", "${item.id}")
            context.startActivity(intent)
            (context).overridePendingTransition(0, 0)
        }

        if (item.status_mekanisasi == 1 && item.status_scan == 1){
            holder.checkbox.apply {
                isChecked = true
                isEnabled = false  // Disable checkbox interaction
                alpha = 0.5f
            }
        }

        val statusMekanisasi = LinearLayout(holder.itemView.context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(ImageView(context).apply {
                    setImageResource(
                        if (item.status_mekanisasi == 1) R.drawable.baseline_check_box_24
                        else R.drawable.baseline_close_24
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        24.dpToPx(context),
                        24.dpToPx(context)
                    )
                    val color = if (item.status_mekanisasi == 1) {
                        ContextCompat.getColor(context, R.color.greendarkerbutton)
                    } else {
                        ContextCompat.getColor(context, R.color.colorRedDark)
                    }
                    setColorFilter(color)
                })
            })
        }

        holder.td5.removeAllViews()
        holder.td5.addView(statusMekanisasi)

        val statusScan = LinearLayout(holder.itemView.context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // SCAN Row
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(ImageView(context).apply {
                    setImageResource(
                        if (item.status_scan == 1) R.drawable.baseline_check_box_24
                        else R.drawable.baseline_close_24
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        24.dpToPx(context),
                        24.dpToPx(context)
                    )
                    val color = if (item.status_scan == 1) {
                        ContextCompat.getColor(context, R.color.greendarkerbutton)
                    } else {
                        ContextCompat.getColor(context, R.color.colorRedDark)
                    }
                    setColorFilter(color)
                })
            })
        }

        holder.td6.removeAllViews()
        holder.td6.addView(statusScan)

        val layoutParamsTd5 = holder.td5.layoutParams as LinearLayout.LayoutParams
        layoutParamsTd5.weight = 0.3f
        holder.td5.layoutParams = layoutParamsTd5
        val layoutParamsTd6 = holder.td6.layoutParams as LinearLayout.LayoutParams
        layoutParamsTd6.weight = 0.3f
        holder.td6.layoutParams = layoutParamsTd6
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    fun updateList(newList: List<ESPBData>) {
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

    override fun getItemCount() = items.size
}