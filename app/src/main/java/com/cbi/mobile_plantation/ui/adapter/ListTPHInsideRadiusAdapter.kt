package com.cbi.mobile_plantation.ui.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.markertph.data.model.JenisTPHModel
import com.cbi.mobile_plantation.R
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
        val tphItem = tphList[position]

        val jenisTPHId = tphItem.jenisTPHId.toIntOrNull() ?: 1


        // Create a drawable for the background with rounded corners and border
        val itemBackground = GradientDrawable()
        itemBackground.cornerRadius = 8f // Adjust the corner radius as needed

        // Set background and border colors based on jenis_tph_id value
        when (jenisTPHId) {
            1 -> {
                // Green for normal
                itemBackground.setColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.greenlight
                    )
                )
                itemBackground.setStroke(
                    4,
                    ContextCompat.getColor(holder.itemView.context, R.color.strokeSelectWorkerGreen)
                )
                holder.jenisTPHNameTextView.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.strokeSelectWorkerGreen
                    )
                )
            }

            2 -> {
                // blue for induk
                itemBackground.setColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.bluelight
                    )
                )
                itemBackground.setStroke(
                    4,
                    ContextCompat.getColor(holder.itemView.context, R.color.blueLightBorder)
                )
                holder.jenisTPHNameTextView.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.blueLightBorder
                    )
                )
            }

            3 -> {
                // Red for banjir
                itemBackground.setColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.redlight
                    )
                )
                itemBackground.setStroke(
                    4,
                    ContextCompat.getColor(holder.itemView.context, R.color.colorRedDark)
                )
                holder.jenisTPHNameTextView.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.colorRedDark
                    )
                )
            }

            else -> {
                // Default white with gray border
                itemBackground.setColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.white
                    )
                )
                itemBackground.setStroke(
                    2,
                    ContextCompat.getColor(holder.itemView.context, R.color.graytextdark)
                )
                holder.jenisTPHNameTextView.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.black
                    )
                )
            }
        }

        // Apply the background drawable to the entire item view
        holder.itemView.background = itemBackground

        // Add some padding to make it look better
        holder.itemView.setPadding(16, 8, 16, 8)

        // Add a small margin between items
        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
        layoutParams.setMargins(8, 4, 8, 4)
        holder.itemView.layoutParams = layoutParams

        // Find the corresponding JenisTPHModel to display its name
        val jenisTPHName = jenisTPHList.find { it.id == jenisTPHId }?.jenis_tph ?: "normal"


        // Format distance text to always show actual distance with special case for >100m
        val distanceValue = when {
            tphItem.distance > 100 -> ">100 m"
            else -> "${tphItem.distance.toInt()} m"
        }

        // Base TPH info text without distance
        val baseText = "TPH ${tphItem.number} - ${tphItem.blockCode}"

// Format text differently based on whether TPH is within range
        val plainText: String

        if (!tphItem.isWithinRange) {
            plainText = "$baseText ($distanceValue)\ndiluar jangkauan\n$jenisTPHName"
        } else {
            plainText = "$baseText ($distanceValue)\n$jenisTPHName"
        }

// Create a spannable string to apply different colors
        val spannable = SpannableString(plainText)

// 1. First determine main text color based on whether the TPH is already selected
        val mainTextColor = if (tphItem.isAlreadySelected) {
            ContextCompat.getColor(holder.itemView.context, R.color.greendarkerbutton)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.black)
        }

// Apply the main color to the whole text
        spannable.setSpan(
            ForegroundColorSpan(mainTextColor),
            0,
            plainText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

// 2. If out of range, apply yellow color to both distance and "diluar jangkauan" text
        if (!tphItem.isWithinRange) {
            val yellowColor = ContextCompat.getColor(holder.itemView.context, R.color.yellowbutton)

            // Find the exact positions of elements
            val openParenIndex = plainText.indexOf('(')
            val closeParenIndex = plainText.indexOf(')')
            val newlineIndex = plainText.indexOf('\n')
            val secondNewlineIndex = plainText.lastIndexOf('\n')

            // Color both the parenthesized distance and the "diluar jangkauan" text yellow
            if (openParenIndex >= 0 && closeParenIndex > openParenIndex && newlineIndex > closeParenIndex) {
                spannable.setSpan(
                    ForegroundColorSpan(yellowColor),
                    openParenIndex,  // Start from the opening parenthesis
                    secondNewlineIndex, // Go up to the second newline (before jenisTPHName)
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

// 3. Apply jenisTph color to the jenisTph name text
// Find the start position for jenisTph name text (after the last newline)
        val lastNewlineIndex = plainText.lastIndexOf('\n')
        if (lastNewlineIndex >= 0 && lastNewlineIndex < plainText.length - 1) {
            val jenisTphColor = when (jenisTPHId) {
                1 -> ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.strokeSelectWorkerGreen
                )

                2 -> ContextCompat.getColor(holder.itemView.context, R.color.blueLightBorder)
                3 -> ContextCompat.getColor(holder.itemView.context, R.color.colorRedDark)
                else -> mainTextColor // Use the main text color as default
            }

            spannable.setSpan(
                ForegroundColorSpan(jenisTphColor),
                lastNewlineIndex + 1,  // Start right after the last newline
                plainText.length,      // Go to the end
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        holder.tphInfoTextView.text = spannable

        val isCurrentlySelected = tphItem.id == selectedTPHId
        holder.radioButton.isChecked = isCurrentlySelected

        if (position == selectedPosition) {
            selectedTPHId = tphItem.id
        }

        // Handle status text based on selection and range
        if (tphItem.isAlreadySelected) {
            // Show different messages based on selection count
            if (tphItem.selectionCount >= AppUtils.MAX_SELECTIONS_PER_TPH) {
                // TPH has reached maximum selections
                holder.tphHasBeenSelected.text =
                    "TPH sudah terpilih ${tphItem.selectionCount} kali (maksimal)!"
                holder.tphHasBeenSelected.visibility = View.VISIBLE

                // Disable radio button
                holder.radioButton.isEnabled = false
                holder.radioButton.alpha = 0.5f // Make it look disabled
            } else {
                // TPH has been selected but can be selected again
                holder.tphHasBeenSelected.text =
                    "TPH sudah terpilih ${tphItem.selectionCount} kali!"
                holder.tphHasBeenSelected.visibility = View.VISIBLE

                // Radio button remains enabled
                holder.radioButton.isEnabled = true
                holder.radioButton.alpha = 1.0f
            }
        } else {
            // TPH has not been selected before
            holder.tphHasBeenSelected.visibility = View.GONE

            // Handle out of range TPHs
            if (!tphItem.isWithinRange) {
                holder.radioButton.isEnabled = false
                holder.radioButton.alpha = 0.5f // Make it look disabled
            } else {
                holder.radioButton.isEnabled = true
                holder.radioButton.alpha = 1.0f
            }
        }

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
        } else {
            // Remove click listeners if TPH cannot be selected again or is out of range
            holder.radioButton.setOnClickListener(null)
            holder.itemView.setOnClickListener(null)
        }
    }


    override fun getItemCount() = tphList.size
}



