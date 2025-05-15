package com.cbi.mobile_plantation.ui.adapter

import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.displayHektarPanenTanggalBlok
import java.text.SimpleDateFormat
import java.util.Locale

class ListHektarPanenAdapter(
    private var items: List<displayHektarPanenTanggalBlok>,
    private val context: Activity
) : RecyclerView.Adapter<ListHektarPanenAdapter.ViewHolder>() {

    // Interface for callback to Activity
    interface OnLuasPanenChangeListener {
        fun onLuasPanenChanged(id: Int, newValue: Float)
    }

    // Reference to the listener
    private var listener: OnLuasPanenChangeListener? = null

    // Method to set the listener
    fun setOnLuasPanenChangeListener(listener: OnLuasPanenChangeListener) {
        this.listener = listener
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val constraint_table_item_row: ConstraintLayout = view.findViewById(R.id.constraint_table_item_row)
        val td1: TextView = view.findViewById(R.id.td1)
        val td2: TextView = view.findViewById(R.id.td2)
        val td3: TextView = view.findViewById(R.id.td3)
        val et4: EditText = view.findViewById(R.id.et4)
        val td5: LinearLayout = view.findViewById(R.id.td5)
        val td6: LinearLayout = view.findViewById(R.id.td6)
        val checkbox: CheckBox = view.findViewById(R.id.checkBoxPanen)
        val flCheckBoxItemTph = view.findViewById<FrameLayout>(R.id.flCheckBoxItemTph)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Configure view visibility
        holder.flCheckBoxItemTph.visibility = View.GONE
        holder.td1.visibility = View.VISIBLE
        holder.td2.visibility = View.VISIBLE
        holder.td3.visibility = View.VISIBLE
        holder.et4.visibility = View.VISIBLE
        holder.td5.visibility = View.VISIBLE
        holder.td6.visibility = View.GONE

// Calculate sum of dibayar values
        val dibayar = item.dibayar_arr.split(";").sumOf {
            it.replace(" ","").toDoubleOrNull() ?: 0.0
        }
        Log.d("ListHektarPanenAdapter", "dibayar_arr: ${item.dibayar_arr}")
        Log.d("ListHektarPanenAdapter", "dibayar: $dibayar")

        // Set text data
        holder.td1.text = item.nama
        holder.td2.text = item.blok
        holder.td3.text = dibayar.toString()

        // Set current value and tag with the item id
        holder.et4.setTag(R.id.item_id_tag, item.id) // You'll need to define this ID in res/values/ids.xml
        if (item.luas_panen == 0.0f){
            holder.et4.setText("")
            holder.td1.setTextColor(ContextCompat.getColor(context, R.color.colorRed))
        }else{
            holder.et4.setText(item.luas_panen.toString())
            holder.td1.setTextColor(ContextCompat.getColor(context, R.color.black))
        }

        // Remove existing TextWatcher if there is one
        if (holder.et4.getTag(R.id.text_watcher_tag) != null) {
            val oldTextWatcher = holder.et4.getTag(R.id.text_watcher_tag) as TextWatcher
            holder.et4.removeTextChangedListener(oldTextWatcher)
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Check if the input contains more than 2 digits after decimal point
                val input = s.toString()
                if (input.isNotEmpty()) {
                    val decimalIndex = input.indexOf('.')
                    if (decimalIndex != -1 && input.length > decimalIndex + 3) {
                        // More than 2 digits after decimal point, truncate the input
                        val truncated = input.substring(0, decimalIndex + 3)
                        holder.et4.removeTextChangedListener(this)
                        holder.et4.setText(truncated)
                        holder.et4.setSelection(truncated.length)
                        holder.et4.addTextChangedListener(this)
                    }
                }

                val newValue = s.toString().toFloatOrNull() ?: 0f
                val itemId = holder.et4.getTag(R.id.item_id_tag) as Int

                // Notify the activity about the change
                listener?.onLuasPanenChanged(itemId, newValue)
            }
        }

        val decimalDigitsInputFilter = InputFilter { source, start, end, dest, dstart, dend ->
            val builder = StringBuilder(dest)
            builder.replace(dstart, dend, source.subSequence(start, end).toString())
            val resultString = builder.toString()

            if (resultString.isEmpty()) {
                return@InputFilter null
            }

            // Allow negative sign at the beginning
            if (resultString == "-") {
                return@InputFilter null
            }

            // Check if it matches the pattern: optional negative sign, digits, optional decimal point and up to 2 digits
            val regex = "^-?\\d*(\\.\\d{0,2})?$".toRegex()
            if (!regex.matches(resultString)) {
                return@InputFilter ""
            }

            null
        }

        holder.et4.filters = arrayOf(decimalDigitsInputFilter)

        // Store the TextWatcher as a tag to be able to remove it later
        holder.et4.setTag(R.id.text_watcher_tag, textWatcher)
        holder.et4.addTextChangedListener(textWatcher)

        // Adjust layout parameters for td5
        val layoutParamsTd5 = holder.td5.layoutParams as LinearLayout.LayoutParams
        layoutParamsTd5.weight = 0.3f
        holder.td5.layoutParams = layoutParamsTd5
    }

    fun updateList(newList: List<displayHektarPanenTanggalBlok>) {
        items = newList
        notifyDataSetChanged()
    }

    // Update a specific item in the list
    fun updateItemLuasPanen(id: Int, newValue: Float) {
        val index = items.indexOfFirst { it.id == id }
        if (index != -1) {
            val updatedItem = items[index].copy(luas_panen = newValue)
            val mutableItems = items.toMutableList()
            mutableItems[index] = updatedItem
            items = mutableItems
            notifyItemChanged(index)
        }
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