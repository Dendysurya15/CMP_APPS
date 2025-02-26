package com.cbi.cmp_project.ui.adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import java.text.SimpleDateFormat
import java.util.Locale

data class ESPBData(
    val time: String,
    val blok: String,
    val janjang: String,
    val tphCount: String,
    val status_mekanisasi: Int?,
    val status_scan: Int?,
)

class ESPBAdapter(private var items: List<ESPBData>) :
    RecyclerView.Adapter<ESPBAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val td1: TextView = view.findViewById(R.id.td1)
        val td2: TextView = view.findViewById(R.id.td2)
        val td3: TextView = view.findViewById(R.id.td3)
        val td4: TextView = view.findViewById(R.id.td4)
        val td5: LinearLayout = view.findViewById(R.id.td5) // Change to LinearLayout
        val checkbox: CheckBox = view.findViewById(R.id.checkBoxPanen) // Add this
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
        holder.td5.visibility = View.VISIBLE

        holder.td1.text = formatToIndonesianDateTime(item.time)
        holder.td2.text = item.blok
        holder.td3.text = item.janjang
        holder.td4.text = item.tphCount

        if (item.status_mekanisasi == 1 && item.status_scan == 1){
            holder.checkbox.apply {
                isChecked = true
                isEnabled = false  // Disable checkbox interaction
                alpha = 0.5f
            }
        }

        val statusLayout = LinearLayout(holder.itemView.context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // CMP Row
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                // MEKANISASI Text
                addView(TextView(context).apply {
                    text = "MEKANISASI"
                    gravity = Gravity.START
                    typeface = ResourcesCompat.getFont(context, R.font.manrope_extrabold) // Add font family and make it bold
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = 4.dpToPx(context)
                    }
                })

                // MEKANISASI Icon
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

            // SCAN Row
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                // SCAN Text
                addView(TextView(context).apply {
                    text = "SCAN"
                    gravity = Gravity.START
                    typeface = ResourcesCompat.getFont(context, R.font.manrope_extrabold) // Add font family and make it bold
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = 4.dpToPx(context)
                    }
                })

                // SCAN Icon
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

        holder.td5.removeAllViews()
        holder.td5.addView(statusLayout)
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