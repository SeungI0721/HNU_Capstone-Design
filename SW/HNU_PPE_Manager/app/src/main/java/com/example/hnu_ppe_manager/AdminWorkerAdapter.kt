package com.example.hnu_ppe_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AdminWorkerAdapter(
    private val onItemClicked: (AdminWorkerStatus) -> Unit
) : RecyclerView.Adapter<AdminWorkerAdapter.WorkerViewHolder>() {
    private val workers = ArrayList<AdminWorkerStatus>()

    fun submitList(newWorkers: List<AdminWorkerStatus>) {
        workers.clear()
        workers.addAll(newWorkers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_worker_device, parent, false)
        return WorkerViewHolder(view)
    }

    override fun getItemCount(): Int = workers.size

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        holder.bind(workers[position])
    }

    inner class WorkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtDeviceIdValue: TextView = itemView.findViewById(R.id.txtDeviceIdValue)
        private val txtWorkLocationValue: TextView = itemView.findViewById(R.id.txtWorkLocationValue)
        private val txtRiskValue: TextView = itemView.findViewById(R.id.txtRiskValue)

        fun bind(worker: AdminWorkerStatus) {
            txtDeviceIdValue.text = worker.deviceId
            txtWorkLocationValue.text = worker.displayLocation
            txtRiskValue.text = worker.riskKorean
            txtRiskValue.setTextColor(riskColor(worker.riskLevel))
            itemView.setOnClickListener { onItemClicked(worker) }
        }

        private fun riskColor(level: String): Int {
            val colorRes = when (level.uppercase(Locale.US)) {
                "SAFE", "정상" -> R.color.ss_green
                "CAUTION", "주의" -> R.color.ss_yellow
                "DANGER", "위험" -> R.color.ss_red
                "EMERGENCY", "응급" -> R.color.ss_emergency
                else -> R.color.ss_sub
            }
            return ContextCompat.getColor(itemView.context, colorRes)
        }
    }
}
