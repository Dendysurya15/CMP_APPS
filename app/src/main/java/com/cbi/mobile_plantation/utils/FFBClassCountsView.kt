package com.cbi.mobile_plantation.utils

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class ClassCount(
    val className: String,
    val count: Int,
    val color: Int
)

class ClassCountsAdapter : RecyclerView.Adapter<ClassCountsAdapter.ViewHolder>() {

    private var classCounts = listOf<ClassCount>()

    fun updateCounts(newCounts: List<ClassCount>) {
        classCounts = newCounts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(classCounts[position])
    }

    override fun getItemCount() = classCounts.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(classCount: ClassCount) {
            text1.text = classCount.className
            text2.text = "Count: ${classCount.count}"

            when {
                classCount.count >= 5 -> {
                    text2.setTextColor(Color.parseColor("#FF6B35"))
                    text1.setTextColor(Color.parseColor("#FF6B35"))
                }
                classCount.count >= 3 -> {
                    text2.setTextColor(Color.parseColor("#FFC107"))
                    text1.setTextColor(Color.parseColor("#FFC107"))
                }
                else -> {
                    text2.setTextColor(Color.parseColor("#4CAF50"))
                    text1.setTextColor(Color.parseColor("#4CAF50"))
                }
            }
        }
    }
}

class FFBClassCountsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val adapter = ClassCountsAdapter()
    private var isExpanded = false
    private lateinit var headerLayout: LinearLayout
    private lateinit var headerText: TextView
    private lateinit var totalCountText: TextView
    private lateinit var classCountsRecyclerView: RecyclerView

    private val classColors = arrayOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN,
        Color.MAGENTA, Color.parseColor("#FF6B35"), Color.parseColor("#7209B7"),
        Color.parseColor("#FF1744"), Color.parseColor("#00E676")
    )

    init {
        orientation = VERTICAL
        setupView()
    }

    private fun setupView() {
        // Create header with proper styling
        headerLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(Color.parseColor("#E0000000")) // Semi-transparent black
            isClickable = true
            isFocusable = true
        }

        headerText = TextView(context).apply {
            text = "FFB Detection Results (tap to expand)"
            textSize = 14f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        totalCountText = TextView(context).apply {
            text = "Total: 0"
            textSize = 12f
            setTextColor(Color.YELLOW)
            setPadding(8, 0, 0, 0)
        }

        headerLayout.addView(headerText)
        headerLayout.addView(totalCountText)

        // Create RecyclerView with proper styling
        classCountsRecyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FFBClassCountsView.adapter
            visibility = GONE
            setBackgroundColor(Color.parseColor("#C0000000")) // Semi-transparent black
            setPadding(16, 8, 16, 8)
        }

        addView(headerLayout)
        addView(classCountsRecyclerView)

        // Setup click listener with visual feedback
        headerLayout.setOnClickListener {
            toggleExpansion()
        }

        // Add touch feedback
        headerLayout.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    view.alpha = 0.7f
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    view.alpha = 1.0f
                }
            }
            false
        }
    }

    private fun toggleExpansion() {
        isExpanded = !isExpanded

        classCountsRecyclerView.visibility = if (isExpanded) VISIBLE else GONE

        headerText.text = if (isExpanded)
            "FFB Detection Results (tap to collapse)" else
            "FFB Detection Results (tap to expand)"

        // Add animation
        if (isExpanded) {
            classCountsRecyclerView.alpha = 0f
            classCountsRecyclerView.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    fun updateClassCounts(classCounts: Map<String, Int>) {
        val totalDetections = classCounts.values.sum()
        totalCountText.text = "Total: $totalDetections"

        if (classCounts.isEmpty()) {
            classCountsRecyclerView.visibility = GONE
            return
        }

        val classCountList = classCounts.map { (className, count) ->
            val classIndex = className.hashCode().let { kotlin.math.abs(it) % classColors.size }
            val color = getColorForClass(classIndex)
            ClassCount(className, count, color)
        }.sortedByDescending { it.count }

        adapter.updateCounts(classCountList)

        if (isExpanded) {
            classCountsRecyclerView.visibility = VISIBLE
        }
    }

    private fun getColorForClass(classIndex: Int): Int {
        return classColors[classIndex % classColors.size]
    }

    fun clear() {
        totalCountText.text = "Total: 0"
        classCountsRecyclerView.visibility = GONE
        adapter.updateCounts(emptyList())
    }
}