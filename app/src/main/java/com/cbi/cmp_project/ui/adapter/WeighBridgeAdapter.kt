package com.cbi.cmp_project.ui.adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.marginEnd
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.utils.AppLogger
import java.text.SimpleDateFormat
import java.util.Locale

data class WBData(
    val id: Int,
    val dept_ppro : Int,
    val divisi_ppro :Int,
    val commodity : Int,
    val blok_jjg : String,
    val nopol : String,
    val driver : String,
    val pemuat_id : String,
    val transporter_id :Int,
    val mill_id : Int,
    val created_by_id : Int,
    val created_at : String,
    val noSPB: String,
    val estate: String,
    val afdeling: String,
    val datetime: String,
    val status_cmp: Int?,
    val status_ppro: Int?,
)

class WeighBridgeAdapter(private var items: List<WBData>) :
    RecyclerView.Adapter<WeighBridgeAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<WBData>() // Track selected items

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

        holder.td1.text = item.noSPB
        holder.td2.text = item.estate
        holder.td3.text = item.afdeling
        holder.td4.text = formatToIndonesianDateTime(item.datetime)

        holder.checkbox.isChecked = selectedItems.contains(item)

        // Handle checkbox click
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedItems.add(item)
            } else {
                selectedItems.remove(item)
            }
        }

        if (item.status_cmp == 1 && item.status_ppro == 1){
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

                // CMP Text
                addView(TextView(context).apply {
                    text = "CMP"
                    gravity = Gravity.START
                    typeface = ResourcesCompat.getFont(context, R.font.manrope_extrabold) // Add font family and make it bold
                    setTextColor(ResourcesCompat.getColor(resources, R.color.black, null))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = 4.dpToPx(context)
                    }
                })

                // CMP Icon
                addView(ImageView(context).apply {
                    setImageResource(
                        if (item.status_cmp == 1) R.drawable.baseline_check_box_24
                        else R.drawable.baseline_close_24
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        24.dpToPx(context),
                        24.dpToPx(context)
                    )
                    val color = if (item.status_cmp == 1) {
                        ContextCompat.getColor(context, R.color.greendarkerbutton)
                    } else {
                        ContextCompat.getColor(context, R.color.colorRedDark)
                    }
                    setColorFilter(color)
                })
            })

            // PPRO Row
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                // PPRO Text
                addView(TextView(context).apply {
                    text = "PPRO"
                    gravity = Gravity.START
                    typeface = ResourcesCompat.getFont(context, R.font.manrope_extrabold) // Add font family and make it bold
                    setTextColor(ResourcesCompat.getColor(resources, R.color.black, null))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = 4.dpToPx(context)
                    }
                })

                // PPRO Icon
                addView(ImageView(context).apply {
                    setImageResource(
                        if (item.status_ppro == 1) R.drawable.baseline_check_box_24
                        else R.drawable.baseline_close_24
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        24.dpToPx(context),
                        24.dpToPx(context)
                    )
                    val color = if (item.status_ppro == 1) {
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

    fun updateList(newList: List<WBData>) {
        items = newList
        selectedItems.clear() // Clear selection on update
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

    fun getSelectedItemsIdLocal(): List<Map<String, Any>> {
        return selectedItems.map { selectedItem ->
            mapOf(
                "id" to (selectedItem.id ?: "")
            )
        }
    }

    fun getSelectedItemsForUpload(): List<Map<String, Any>> {
        return selectedItems.map { selectedItem ->
            mapOf(
                "id" to (selectedItem.id ?: ""),
                "dept_ppro" to (selectedItem.dept_ppro ?: ""),
                "divisi_ppro" to (selectedItem.divisi_ppro ?: ""),
                "commodity" to (selectedItem.commodity ?: ""),
                "blok_jjg" to (selectedItem.blok_jjg ?: ""),
                "nopol" to (selectedItem.nopol ?: ""),
                "driver" to (selectedItem.driver ?: ""),
                "pemuat_id" to (selectedItem.pemuat_id ?: ""),
                "transporter_id" to (selectedItem.transporter_id ?: ""),
                "mill_id" to (selectedItem.mill_id ?: ""),
                "created_by_id" to (selectedItem.created_by_id ?: ""),
                "created_at" to (selectedItem.created_at ?: ""),
                "noSPB" to (selectedItem.noSPB ?: ""),
            )
        }
    }

    override fun getItemCount() = items.size
}