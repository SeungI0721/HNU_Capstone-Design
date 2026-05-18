// 작업자 상태 카드를 RecyclerView에 표시하는 어댑터 파일
package com.example.hnu_ppe_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

// 작업자 카드 목록 Adapter
class AdminWorkerAdapter(
    private val onItemClicked: (AdminWorkerStatus) -> Unit
) : RecyclerView.Adapter<AdminWorkerAdapter.WorkerViewHolder>() {

    // 현재 표시할 작업자 목록
    private val workers = ArrayList<AdminWorkerStatus>()

    fun submitList(newWorkers: List<AdminWorkerStatus>) {
        workers.clear()
        workers.addAll(newWorkers)
        notifyDataSetChanged()
    }

    // 카드 XML을 ViewHolder로 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_worker_device, parent, false)
        return WorkerViewHolder(view)
    }

    override fun getItemCount(): Int = workers.size

    // 작업자 데이터를 카드에 연결
    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        holder.bind(workers[position])
    }

    // 카드 내부 View 연결과 표시 처리
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

        // 위험도별 텍스트 색상 선택
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
