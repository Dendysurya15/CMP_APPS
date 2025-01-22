package com.cbi.cmp_project.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R


class SelectedWorkerAdapter : RecyclerView.Adapter<SelectedWorkerAdapter.ViewHolder>() {
    private val selectedWorkers = mutableListOf<String>()
    private val allWorkers = mutableListOf<String>()  // Keep track of all workers

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val workerName: TextView = view.findViewById(R.id.worker_name)
        val removeButton: ImageView = view.findViewById(R.id.remove_worker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_worker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val worker = selectedWorkers[position]
        holder.workerName.text = worker.uppercase()
        holder.removeButton.setOnClickListener {
            removeWorker(position)
        }
    }

    override fun getItemCount() = selectedWorkers.size

    fun addWorker(worker: String) {
        if (!selectedWorkers.contains(worker)) {
            selectedWorkers.add(worker)
            notifyDataSetChanged()
        }
    }

    private fun removeWorker(position: Int) {
        selectedWorkers.removeAt(position)
        notifyDataSetChanged()
    }

    fun setAvailableWorkers(workers: List<String>) {
        allWorkers.clear()
        allWorkers.addAll(workers)
    }

    fun getAvailableWorkers(): List<String> {
        return allWorkers.filter { !selectedWorkers.contains(it) }
    }

    fun getSelectedWorkers(): List<String> = selectedWorkers

    fun clearAllWorkers() {
        selectedWorkers.clear()
        allWorkers.clear()
        notifyDataSetChanged()
    }
}

