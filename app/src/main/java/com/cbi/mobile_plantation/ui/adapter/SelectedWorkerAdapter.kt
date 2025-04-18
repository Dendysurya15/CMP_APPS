package com.cbi.mobile_plantation.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.vibrate

data class Worker(val id: String, val name: String)  // Define a Worker model

class SelectedWorkerAdapter : RecyclerView.Adapter<SelectedWorkerAdapter.ViewHolder>() {
    private val selectedWorkers = mutableListOf<Worker>()
    private val allWorkers = mutableListOf<Worker>()  // Keep track of all workers

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val workerName: TextView = view.findViewById(R.id.worker_name)
        val removeButton: ImageView = view.findViewById(R.id.remove_worker)
        val context: Context = view.context // Store the context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_worker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val worker = selectedWorkers[position]
        holder.workerName.text = worker.name.uppercase() // Display name
        holder.removeButton.setOnClickListener {
            removeWorker(position, holder.context)
        }
    }

    override fun getItemCount() = selectedWorkers.size

    fun addWorker(worker: Worker) {
        if (!selectedWorkers.any { it.id == worker.id }) {  // Prevent duplicates by ID
            selectedWorkers.add(worker)
            notifyDataSetChanged()
        }
    }

    private fun removeWorker(position: Int, context: Context) {
        selectedWorkers.removeAt(position)
        context.vibrate() // Use the extension function
        notifyDataSetChanged()
    }

    fun setAvailableWorkers(workers: List<Worker>) {
        allWorkers.clear()
        allWorkers.addAll(workers)
    }

    fun getAvailableWorkers(): List<Worker> {
        return allWorkers.filter { worker -> selectedWorkers.none { it.id == worker.id } }
    }

    fun getSelectedWorkers(): List<Worker> = selectedWorkers

    fun clearAllWorkers() {
        selectedWorkers.clear()
        allWorkers.clear()
        notifyDataSetChanged()
    }
}

