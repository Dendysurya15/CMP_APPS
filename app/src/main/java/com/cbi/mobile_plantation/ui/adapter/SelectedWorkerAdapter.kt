package com.cbi.mobile_plantation.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.vibrate

data class Worker(val id: String, val name: String)  // Define a Worker model

class SelectedWorkerAdapter : RecyclerView.Adapter<SelectedWorkerAdapter.ViewHolder>() {
    private val selectedWorkers = mutableListOf<Worker>()
    private val allWorkers = mutableListOf<Worker>()
    private var isEnabled = true
    private var displayMode = DisplayMode.EDITABLE

    // Add callback for when worker is removed
    private var onWorkerRemovedListener: ((List<Worker>) -> Unit)? = null
    private var onWorkerActuallyRemovedListener: ((Worker) -> Unit)? = null

    enum class DisplayMode {
        EDITABLE,
        DISPLAY_ONLY
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val workerName: TextView = view.findViewById(R.id.worker_name)
        val removeButton: ImageView = view.findViewById(R.id.remove_worker)
        val context: Context = view.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_worker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val worker = selectedWorkers[position]
        holder.workerName.text = worker.name.uppercase()

        when (displayMode) {
            DisplayMode.DISPLAY_ONLY -> {
                holder.removeButton.visibility = View.GONE
                val layoutParams = holder.workerName.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.marginEnd = 0
                holder.workerName.layoutParams = layoutParams
            }
            DisplayMode.EDITABLE -> {
                holder.removeButton.visibility = View.VISIBLE
                val layoutParams = holder.workerName.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.marginEnd = holder.context.resources.getDimensionPixelSize(R.dimen.m)
                holder.workerName.layoutParams = layoutParams

                holder.removeButton.isEnabled = isEnabled
                holder.removeButton.alpha = if (isEnabled) 1.0f else 0.5f

                holder.removeButton.setOnClickListener {
                    if (isEnabled) {
                        removeWorker(position, holder.context)
                    }
                }
            }
        }
    }

    override fun getItemCount() = selectedWorkers.size

    fun addWorker(worker: Worker) {
        if (!selectedWorkers.any { it.id == worker.id }) {
            selectedWorkers.add(worker)
            notifyDataSetChanged()
            // Notify that available workers changed
            onWorkerRemovedListener?.invoke(getAvailableWorkers())
        }
    }

    private fun removeWorker(position: Int, context: Context) {
        AppLogger.d("removeWorker called for position: $position")

        if (position < 0 || position >= selectedWorkers.size) {
            AppLogger.e("Invalid position for removal: $position, size: ${selectedWorkers.size}")
            return
        }

        val removedWorker = selectedWorkers.removeAt(position)
        AppLogger.d("Removed worker: ${removedWorker.name} (ID: ${removedWorker.id})")

        context.vibrate()
        notifyDataSetChanged()

        // IMPORTANT: Call BOTH callbacks
        onWorkerRemovedListener?.invoke(getAvailableWorkers())

        // This is the important one that was missing!
        AppLogger.d("Calling onWorkerActuallyRemovedListener...")
        onWorkerActuallyRemovedListener?.invoke(removedWorker)
    }

    fun setAvailableWorkers(workers: List<Worker>) {
        allWorkers.clear()
        allWorkers.addAll(workers)
        // Notify that available workers changed
        onWorkerRemovedListener?.invoke(getAvailableWorkers())
    }

    fun getAvailableWorkers(): List<Worker> {
        return allWorkers.filter { worker -> selectedWorkers.none { it.id == worker.id } }
    }

    fun getSelectedWorkers(): List<Worker> = selectedWorkers

    fun clearAllWorkers() {
        selectedWorkers.clear()
        allWorkers.clear()
        notifyDataSetChanged()
        // Notify that available workers changed
        onWorkerRemovedListener?.invoke(getAvailableWorkers())
    }

    fun setEnabled(enabled: Boolean) {
        if (this.isEnabled != enabled) {
            this.isEnabled = enabled
            notifyDataSetChanged()
        }
    }

    fun setDisplayMode(mode: DisplayMode) {
        if (this.displayMode != mode) {
            this.displayMode = mode
            notifyDataSetChanged()
        }
    }

    fun setDisplayOnly(displayOnly: Boolean) {
        setDisplayMode(if (displayOnly) DisplayMode.DISPLAY_ONLY else DisplayMode.EDITABLE)
    }

    // New method to set callback for when workers change
    fun setOnWorkerRemovedListener(listener: (List<Worker>) -> Unit) {
        this.onWorkerRemovedListener = listener
        AppLogger.d("onWorkerRemovedListener set")
    }

    // New method to set callback for when worker is actually removed
    fun setOnWorkerActuallyRemovedListener(listener: (Worker) -> Unit) {
        this.onWorkerActuallyRemovedListener = listener
        AppLogger.d("onWorkerActuallyRemovedListener set")
    }
}