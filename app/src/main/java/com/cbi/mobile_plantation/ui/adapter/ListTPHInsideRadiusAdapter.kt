package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.markertph.data.model.JenisTPHModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.ScannedTPHSelectionItem
import com.google.android.material.radiobutton.MaterialRadioButton

/**
 * Modified getTPHsInsideRadius function to keep selected TPHs even if they're outside the range
 */

class ListTPHInsideRadiusAdapter(
    private val tphList: List<ScannedTPHSelectionItem>,
    private val listener: OnTPHSelectedListener,
    private val jenisTPHList: List<JenisTPHModel> // Added parameter
) : RecyclerView.Adapter<ListTPHInsideRadiusAdapter.ViewHolder>() {

    private var selectedPosition = -1

    // Keep track of currently selected TPH ID to maintain selection during refresh
    private var selectedTPHId: Int? = null

    // Updated interface
    interface OnTPHSelectedListener {
        fun onTPHSelected(selectedTPH: ScannedTPHSelectionItem)
        fun getCurrentlySelectedTPHId(): Int? // New method to get currently selected TPH ID
    }

    init {
        // Initialize selected position based on currently selected TPH
        selectedTPHId = listener.getCurrentlySelectedTPHId()
        if (selectedTPHId != null) {
            for (i in tphList.indices) {
                if (tphList[i].id == selectedTPHId) {
                    selectedPosition = i
                    break
                }
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tphInfoTextView: TextView = itemView.findViewById(R.id.tphInfoTextView)
        val jenisTPHNameTextView: TextView = itemView.findViewById(R.id.jenisTPHName)
        val radioButton: MaterialRadioButton = itemView.findViewById(R.id.rbScannedTPHInsideRadius)
        val tphHasBeenSelected: TextView = itemView.findViewById(R.id.tphHasBeenSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_tph_inside_radius, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18s", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        // Get the TPH item for this position
        val tphItem = tphList[position]

        AppLogger.d("tphItem $tphItem")

        val jenisTPHId = tphItem.jenisTPHId.toInt()

        // Create background drawable
        val itemBackground = GradientDrawable()
        itemBackground.cornerRadius = 8f

        // Set the appropriate color based on jenisTPHId
        val textColor = when (jenisTPHId) {
            1 -> ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton)
            2 -> ContextCompat.getColor(holder.itemView.context, R.color.bluedarklight)
            3 -> ContextCompat.getColor(holder.itemView.context, R.color.colorRedDark)
            else -> ContextCompat.getColor(holder.itemView.context, R.color.black)
        }

        // Set up the background
        holder.itemView.background = itemBackground
        holder.itemView.setPadding(16, 8, 16, 8)

        // Setup margins
        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
        layoutParams.setMargins(8, 4, 8, 4)
        holder.itemView.layoutParams = layoutParams

        // Get the jenisTPH name - IMPORTANT FIX: Make sure to use the jenisTPHId from this item, not a cached one
        val jenisTPHName = jenisTPHList.find { it.id == jenisTPHId }?.jenis_tph ?: "normal"
        AppLogger.d("Position $position: Found jenisTPH name: $jenisTPHName for id: $jenisTPHId")

        // Format distance text
        val distanceValue = when {
            tphItem.distance > 100 -> ">100 m"
            else -> "${tphItem.distance.toInt()} m"
        }

        // Base TPH info text
        val baseText = "TPH ${tphItem.number} - ${tphItem.blockCode}"

        // Create the full text
        val plainText = if (!tphItem.isWithinRange) {
            "$baseText ($distanceValue)\ndiluar jangkauan"
        } else {
            "$baseText ($distanceValue)"
        }

        // Create spannable for coloring
        val spannable = SpannableString(plainText)

        // Color the baseText part with the appropriate color
        spannable.setSpan(
            ForegroundColorSpan(textColor),
            0,
            baseText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Color the distance part yellow if out of range
        if (!tphItem.isWithinRange) {
            val yellowColor = ContextCompat.getColor(holder.itemView.context, R.color.yellowbutton)
            val openParenIndex = plainText.indexOf('(')

            if (openParenIndex >= 0) {
                spannable.setSpan(
                    ForegroundColorSpan(yellowColor),
                    openParenIndex,
                    plainText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        // Set the text
        holder.tphInfoTextView.text = spannable

        // IMPORTANT FIX: Always reset the jenisTPHNameTextView visibility and content for each item
        // Handle jenisTPHName TextView visibility
        if (jenisTPHId == 1) {
            // Hide for Normal type
            holder.jenisTPHNameTextView.visibility = View.GONE
            holder.jenisTPHNameTextView.text = "" // Clear text for recycled views
        } else {
            // Show for other types
            holder.jenisTPHNameTextView.visibility = View.VISIBLE

            // Format the text
            val capitalizedJenisTPHName = jenisTPHName.replaceFirstChar { it.uppercase() }
            val formattedJenisTPHName = "TPH $capitalizedJenisTPHName"

            // Create italic style
            val spannableJenisTPH = SpannableString(formattedJenisTPHName)
            spannableJenisTPH.setSpan(
                StyleSpan(Typeface.ITALIC),
                0,
                formattedJenisTPHName.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Set text and color
            holder.jenisTPHNameTextView.text = spannableJenisTPH
            holder.jenisTPHNameTextView.setTextColor(textColor)

        }

        val isCurrentlySelected = tphItem.id == selectedTPHId
        holder.radioButton.isChecked = isCurrentlySelected

        if (position == selectedPosition) {
            selectedTPHId = tphItem.id
        }

        // Handle status text based on selection and range
        if (tphItem.isAlreadySelected) {
            val jenisTPHId = tphItem.jenisTPHId.toInt()
            // Get the default limit from jenisTPHList
            val defaultLimit = jenisTPHList.find { it.id == jenisTPHId }?.limit ?: 1

            // Get the custom limit if available
            val customLimit = try {
                tphItem.customLimit?.toIntOrNull()
            } catch (e: Exception) {
                null
            }

            // Calculate the limit to use based on TPH type
            val limit = if (jenisTPHId == 2 && jenisTPHList.find { it.id == 2 }?.jenis_tph == "induk") {
                // Special case for jenis_tph = induk (id = 2)
                if (customLimit != null && customLimit > 3 && customLimit <= 999) {
                    // Use the custom limit if it's greater than 3 and up to 999
                    customLimit
                } else {
                    // Otherwise, use the default limit (7)
                    defaultLimit
                }
            } else {
                // For other jenis_tph_id values
                customLimit ?: defaultLimit
            }

            // Show different messages based on selection count
            if (tphItem.selectionCount >= limit!!) {
                // TPH has reached maximum selections
                holder.tphHasBeenSelected.text =
                    "TPH sudah terpilih ${tphItem.selectionCount} dari $limit kali (maksimal)!"
                holder.tphHasBeenSelected.visibility = View.VISIBLE

                // Disable radio button
                holder.radioButton.isEnabled = false
                holder.radioButton.alpha = 0.5f // Make it look disabled
            } else {
                // TPH has been selected but can be selected again
                holder.tphHasBeenSelected.text =
                    "TPH sudah terpilih ${tphItem.selectionCount} dari $limit kali!"
                holder.tphHasBeenSelected.visibility = View.VISIBLE

                // Radio button remains enabled
                holder.radioButton.isEnabled = true
                holder.radioButton.alpha = 1.0f
            }
        }

        // Reset click listeners for all items
        holder.radioButton.setOnClickListener(null)
        holder.itemView.setOnClickListener(null)

        // Only enable click listener if the TPH can be selected again and is within range
        if (tphItem.canBeSelectedAgain && tphItem.isWithinRange) {
            holder.radioButton.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = position
                selectedTPHId = tphItem.id

                // Update old and new positions
                if (oldPosition >= 0) notifyItemChanged(oldPosition)
                notifyItemChanged(position)

                // Notify the activity about the selected item
                listener.onTPHSelected(tphItem)
            }

            // Make the entire item clickable as well
            holder.itemView.setOnClickListener {
                holder.radioButton.performClick()
            }
        }
    }


    override fun getItemCount() = tphList.size
}



