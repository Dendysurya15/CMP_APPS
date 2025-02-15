package com.cbi.cmp_project.ui.adapter

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R

data class FeatureCard(
    val cardBackgroundColor: Int,
    val featureName: String,
    val featureNameBackgroundColor: Int,
    val iconResource: Int? = null,
    val count: String? = null,
    val functionDescription: String,
    val displayType: DisplayType,
    val subTitle: String? = ""
)

enum class DisplayType {
    ICON,
    COUNT
}


class FeatureCardAdapter(private val onFeatureClicked: (FeatureCard) -> Unit) :
    RecyclerView.Adapter<FeatureCardAdapter.FeatureViewHolder>() {

    private var features = ArrayList<FeatureCard>()

    fun setFeatures(newFeatures: List<FeatureCard>) {
        features = ArrayList(newFeatures)
        notifyDataSetChanged()
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

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        val feature = features[position]
        val context = holder.itemView.context

        holder.featureName.text = feature.featureName
        holder.featureNameBackground.setCardBackgroundColor(
            ContextCompat.getColor(context, feature.featureNameBackgroundColor)
        )
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

        holder.functionDescription.text = feature.functionDescription
        holder.cardView.setOnClickListener {
            onFeatureClicked(feature)
        }
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
                else -> super.onBindViewHolder(holder, position, payloads)
            }
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun getItemCount() = features.size
}