package com.example.hnu_ppe_control

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WorkerDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_detail)

        val txtWorkerDetail: TextView = findViewById(R.id.txtWorkerDetail)
        val btnCloseDetail: Button = findViewById(R.id.btnCloseDetail)
        txtWorkerDetail.text = buildDetailText()
        btnCloseDetail.setOnClickListener { finish() }
    }

    private fun buildDetailText(): String {
        val extras = intent.extras
        fun value(key: String): String = extras?.getString(key)?.takeIf { it.isNotBlank() } ?: "-"

        return """
            장치 ID : ${value("workerId")}
            작업 위치 코드 : ${value("workLocationCode")}
            작업 위치 이름 : ${value("workLocationName")}
            작업 시작 시간 : ${value("workStartedAt")}
            BLE 상태 : ${value("bleState")}
            BLE 신호 등급 : ${value("bleSignalLevel")}
            RSSI 숫자 : ${value("bleRssi")}
            위험 단계 : ${value("riskLevel")}
            피부온도 : ${value("temp")}
            심박수 : ${value("hr")}
            SpO2 : ${value("spo2")}
            주변 온도 : ${value("env")}
            습도 : ${value("hum")}
            조도 : ${value("lux")}
            자세 : ${value("posture")}
            기준 자세 상태 : ${value("baselinePosture")}
            기상특보 : ${value("weatherAlert")}
            기상 지역 : ${value("weatherRegion")}
            오늘 최고기온 : ${value("todayMaxTemp")}
            마지막 업데이트 시간 : ${value("lastUpdatedAt")}
        """.trimIndent()
    }
}
