// 작업자의 최신 센서, BLE, 위험 상태 상세 정보를 표시하는 화면 파일
package com.example.hnu_ppe_control

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.hnu_ppe_control.data.WorkerDetailSnapshot
import com.example.hnu_ppe_control.data.WorkerStatusStore

class WorkerDetailActivity : AppCompatActivity() {

    private lateinit var txtWorkerDetail: TextView

    private val detailListener: (WorkerDetailSnapshot) -> Unit = { snapshot ->
        runOnUiThread {
            txtWorkerDetail.text = buildDetailText(snapshot)
        }
    }

    private val detailReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != WorkerStatusStore.ACTION_WORKER_DETAIL_UPDATED) return
            txtWorkerDetail.text = buildDetailText(WorkerStatusStore.latestSnapshot ?: WorkerDetailSnapshot())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_detail)

        txtWorkerDetail = findViewById(R.id.txtWorkerDetail)
        val btnCloseDetail: Button = findViewById(R.id.btnCloseDetail)

        txtWorkerDetail.text = buildDetailText(WorkerStatusStore.latestSnapshot ?: WorkerDetailSnapshot())
        btnCloseDetail.setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        WorkerStatusStore.addListener(detailListener)
        val filter = IntentFilter(WorkerStatusStore.ACTION_WORKER_DETAIL_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(detailReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(detailReceiver, filter)
        }
    }

    override fun onStop() {
        unregisterReceiver(detailReceiver)
        WorkerStatusStore.removeListener(detailListener)
        super.onStop()
    }

    private fun buildDetailText(snapshot: WorkerDetailSnapshot): String {
        return """
            장치 ID : ${snapshot.workerId}
            작업 위치 코드 : ${snapshot.workLocationCode}
            작업 위치 이름 : ${snapshot.workLocationName}
            작업 시작 시간 : ${snapshot.workStartedAt}
            BLE 상태 : ${snapshot.bleState}
            BLE 신호 등급 : ${snapshot.bleSignalLevel}
            RSSI 값 : ${snapshot.bleRssi}
            위험 단계 : ${snapshot.riskLevel}
            피부온도 : ${snapshot.temp}
            심박수 : ${snapshot.hr}
            SpO2 : ${snapshot.spo2}
            주변 온도 : ${snapshot.env}
            습도 : ${snapshot.hum}
            조도 : ${snapshot.lux}
            MPU X/Y/Z : ${snapshot.axis}
            자세 : ${snapshot.posture}
            기준 자세 상태 : ${snapshot.baselinePosture}
            기상 알림 : ${snapshot.weatherAlert}
            기상 지역 : ${snapshot.weatherRegion}
            오늘 최고기온 : ${snapshot.todayMaxTemp}
            마지막 업데이트 시간 : ${snapshot.lastUpdatedAt}
        """.trimIndent()
    }
}
