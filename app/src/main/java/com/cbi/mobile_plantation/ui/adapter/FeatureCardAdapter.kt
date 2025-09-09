package com.cbi.mobile_plantation.ui.adapter

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.FeatureStateManager

data class FeatureCard(
    val cardBackgroundColor: Int,
    val featureName: String,
    val featureNameBackgroundColor: Int,
    val iconResource: Int? = null,
    val count: String? = null,
    val functionDescription: String,
    val displayType: DisplayType,
    val subTitle: String? = "",
    var isDisabled: Boolean = false // Make this var for reactive updates
) {
    /**
     * Get the current disabled state from FeatureStateManager
     * This ensures we always have the most up-to-date state
     */
    fun getCurrentDisabledState(): Boolean {
        return FeatureStateManager.isFeatureDisabled(featureName)
    }
}

enum class DisplayType {
    ICON,
    COUNT
}

class FeatureCardAdapter(private val onFeatureClicked: (FeatureCard) -> Unit) :
    RecyclerView.Adapter<FeatureCardAdapter.FeatureViewHolder>() {

    private var features = ArrayList<FeatureCard>()
    private var lifecycleOwner: LifecycleOwner? = null

    /**
     * Set lifecycle owner to enable reactive updates
     * Call this from your Activity/Fragment after creating the adapter
     */
    fun setLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner = owner
        observeFeatureStates()
        AppLogger.d("FeatureCardAdapter: Lifecycle owner set, starting state observation")
    }

    /**
     * Observe state changes from FeatureStateManager
     * Automatically updates UI when states change
     */
    private fun observeFeatureStates() {
        lifecycleOwner?.let { owner ->
            // Observe app update requirement changes
            FeatureStateManager.isUpdateRequired.observe(owner) { updateRequired ->
                AppLogger.d("FeatureCardAdapter: Update required changed to: $updateRequired")
                refreshAllFeatureStates()
            }

            // Observe specific feature disabled changes
            FeatureStateManager.featuresDisabled.observe(owner) { disabledFeatures ->
                AppLogger.d("FeatureCardAdapter: Disabled features changed: $disabledFeatures")
                refreshAllFeatureStates()
            }
        }
    }

    /**
     * Refresh all feature disabled states
     * Only updates items that actually changed
     */
    private fun refreshAllFeatureStates() {
        var changeCount = 0
        features.forEachIndexed { index, feature ->
            val newDisabledState = feature.getCurrentDisabledState()
            if (feature.isDisabled != newDisabledState) {
                feature.isDisabled = newDisabledState
                notifyItemChanged(index, "disabled_state_changed")
                changeCount++
            }
        }

        if (changeCount > 0) {
            AppLogger.d("FeatureCardAdapter: Updated $changeCount feature states automatically")
        }
    }

    fun setFeatures(newFeatures: List<FeatureCard>) {
        // Update disabled states before setting features
        newFeatures.forEach { feature ->
            feature.isDisabled = feature.getCurrentDisabledState()
        }

        features = ArrayList(newFeatures)
        notifyDataSetChanged()
        AppLogger.d("FeatureCardAdapter: Set ${features.size} features")
    }

    fun updateCount(featureName: String, newCount: String) {
        val position = features.indexOfFirst {
            it.featureName == featureName && it.displayType == DisplayType.COUNT
        }
        if (position != -1) {
            val updatedFeature = features[position].copy(count = newCount)
            features[position] = updatedFeature
            notifyItemChanged(position)
        }
    }

    fun showLoadingForFeature(featureName: String) {
        val position = features.indexOfFirst {
            it.featureName == featureName && it.displayType == DisplayType.COUNT
        }
        if (position != -1) {
            notifyItemChanged(position, "show_loading")
        }
    }

    fun hideLoadingForFeature(featureName: String) {
        val position = features.indexOfFirst {
            it.featureName == featureName && it.displayType == DisplayType.COUNT
        }
        if (position != -1) {
            notifyItemChanged(position, "hide_loading")
        }
    }

    class FeatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_panen_tbs)
        val featureName: TextView = itemView.findViewById(R.id.feature_name)
        val featureNameBackground: CardView = itemView.findViewById(R.id.card_panen_tbs_border)
        val iconFeature: ImageView = itemView.findViewById(R.id.icon_feature)
        val countFeature: TextView = itemView.findViewById(R.id.count_feature_data)
        val functionDescription: TextView = itemView.findViewById(R.id.feature_function_description)
        val loadingDotsContainer: LinearLayout = itemView.findViewById(R.id.countLoadingDotsContainer)
        val dot1: TextView = itemView.findViewById(R.id.countDot1)
        val dot2: TextView = itemView.findViewById(R.id.countDot2)
        val dot3: TextView = itemView.findViewById(R.id.countDot3)
        val dot4: TextView = itemView.findViewById(R.id.countDot4)

        fun showLoadingAnimation() {
            val dots = listOf(dot1, dot2, dot3, dot4)
            countFeature.visibility = View.INVISIBLE
            loadingDotsContainer.visibility = View.VISIBLE

            dots.forEachIndexed { index, dot ->
                val animation = ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f, 0f)
                animation.duration = 500
                animation.repeatCount = ObjectAnimator.INFINITE
                animation.repeatMode = ObjectAnimator.REVERSE
                animation.startDelay = (index * 100).toLong()
                animation.start()
            }
        }

        fun hideLoadingAnimation() {
            loadingDotsContainer.visibility = View.GONE
            countFeature.visibility = View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_feature, parent, false)
        return FeatureViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: FeatureViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                "show_loading" -> holder.showLoadingAnimation()
                "hide_loading" -> holder.hideLoadingAnimation()
                "disabled_state_changed" -> {
                    // Only update the visual state, not the entire view - more efficient
                    updateFeatureVisualState(holder, features[position])
                    return
                }
                else -> super.onBindViewHolder(holder, position, payloads)
            }
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        val feature = features[position]
        val context = holder.itemView.context

        // Bind basic data
        holder.featureName.text = feature.featureName
        holder.functionDescription.text = feature.functionDescription

        // Handle display type
        when (feature.displayType) {
            DisplayType.ICON -> {
                holder.iconFeature.visibility = View.VISIBLE
                holder.countFeature.visibility = View.GONE
                feature.iconResource?.let {
                    holder.iconFeature.setImageResource(it)
                }
            }
            DisplayType.COUNT -> {
                holder.iconFeature.visibility = View.GONE
                holder.countFeature.visibility = View.VISIBLE
                holder.countFeature.text = feature.count
            }
        }

        // Update visual state and click behavior based on disabled status
        updateFeatureVisualState(holder, feature)
    }

    /**
     * Update only the visual appearance and click behavior
     * This method is called both in full bind and partial updates
     */
    private fun updateFeatureVisualState(holder: FeatureViewHolder, feature: FeatureCard) {
        val context = holder.itemView.context

        AppLogger.d("FeatureCardAdapter: Updating visual state for ${feature.featureName}, disabled: ${feature.isDisabled}")

        if (feature.isDisabled) {
            // Apply disabled styling
            holder.itemView.alpha = 0.5f

            // Gray border and background
            holder.featureNameBackground.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.graytextdark)
            )
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.white)
            )

            // Dim icon
            holder.iconFeature.alpha = 0.7f
            holder.iconFeature.setColorFilter(
                ContextCompat.getColor(context, R.color.graydarker)
            )



        } else {
            // Apply enabled styling
            holder.itemView.alpha = 1.0f

            // Restore original colors
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.white)
            )
            holder.featureNameBackground.setCardBackgroundColor(
                ContextCompat.getColor(context, feature.featureNameBackgroundColor)
            )

            // Restore text colors
            holder.functionDescription.setTextColor(
                ContextCompat.getColor(context, R.color.black)
            )
            holder.countFeature.setTextColor(
                ContextCompat.getColor(context, R.color.black)
            )

            // Restore icon
            holder.iconFeature.alpha = 1.0f
            holder.iconFeature.clearColorFilter()


        }

        holder.cardView.setOnClickListener {
            AppLogger.d("FeatureCardAdapter: Card clicked for: ${feature.featureName}")
            onFeatureClicked(feature)
        }

        // Keep these enabled for visual feedback
        holder.cardView.isClickable = true
        holder.cardView.isEnabled = true
    }

    override fun getItemCount() = features.size
}