// 선택한 작업자의 Firebase 상세 상태를 읽어 표시하는 파일
package com.example.hnu_ppe_manager

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

// 작업자 상세 데이터 화면 Activity
class AdminWorkerDetailActivity : AppCompatActivity() {

    // 상세 화면 View 참조
    private lateinit var txtDetailStatus: TextView
    private lateinit var txtWorkerId: TextView
    private lateinit var txtDeviceId: TextView
    private lateinit var txtWorkLocation: TextView
    private lateinit var txtRisk: TextView
    private lateinit var txtTemp: TextView
    private lateinit var txtHeartRate: TextView
    private lateinit var txtSpo2: TextView
    private lateinit var txtEnvTemp: TextView
    private lateinit var txtHumidity: TextView
    private lateinit var txtLux: TextView
    private lateinit var txtPosture: TextView
    private lateinit var txtBleState: TextView
    private lateinit var txtRssi: TextView
    private lateinit var txtLastUpdated: TextView

    // 상세 대상 작업자와 Firebase 리스너
    private var workerId: String = "-"
    private var statusListener: ValueEventListener? = null

    // 화면 초기화 진입점
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminFirebaseConfig.initialize(this)
        setContentView(R.layout.activity_admin_worker_detail)

        workerId = intent.getStringExtra(AdminMainActivity.EXTRA_WORKER_ID) ?: "-"
        bindViews()
        findViewById<TextView>(R.id.btnCloseDetail).setOnClickListener { finish() }
        showEmptyState()
    }

    // 상세 데이터 실시간 읽기 시작
    override fun onStart() {
        super.onStart()
        startStatusListener()
    }

    // 상세 데이터 리스너 정리
    override fun onStop() {
        stopStatusListener()
        super.onStop()
    }

    // XML View 연결
    private fun bindViews() {
        txtDetailStatus = findViewById(R.id.txtDetailStatus)
        txtWorkerId = findViewById(R.id.txtWorkerId)
        txtDeviceId = findViewById(R.id.txtDeviceId)
        txtWorkLocation = findViewById(R.id.txtWorkLocation)
        txtRisk = findViewById(R.id.txtRisk)
        txtTemp = findViewById(R.id.txtTemp)
        txtHeartRate = findViewById(R.id.txtHeartRate)
        txtSpo2 = findViewById(R.id.txtSpo2)
        txtEnvTemp = findViewById(R.id.txtEnvTemp)
        txtHumidity = findViewById(R.id.txtHumidity)
        txtLux = findViewById(R.id.txtLux)
        txtPosture = findViewById(R.id.txtPosture)
        txtBleState = findViewById(R.id.txtBleState)
        txtRssi = findViewById(R.id.txtRssi)
        txtLastUpdated = findViewById(R.id.txtLastUpdated)
    }

    // workers/{workerId}/currentStatus 읽기 전용 리스너
    private fun startStatusListener() {
        if (workerId == "-") {
            txtDetailStatus.text = "작업자 ID가 없습니다."
            return
        }

        val reference = FirebaseDatabase.getInstance()
            .reference
            .child("workers")
            .child(workerId)
            .child("currentStatus")

        statusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    txtDetailStatus.text = "작업자 데이터가 없습니다."
                    showEmptyState()
                    return
                }

                val status = AdminWorkerStatus.fromSnapshot(workerId, snapshot)
                txtDetailStatus.visibility = View.GONE
                renderStatus(status)
            }

            override fun onCancelled(error: DatabaseError) {
                txtDetailStatus.visibility = View.VISIBLE
                txtDetailStatus.text = "Firebase 읽기 실패: ${error.message}"
            }
        }

        reference.addValueEventListener(statusListener as ValueEventListener)
    }

    // 상세 데이터 리스너 제거
    private fun stopStatusListener() {
        val listener = statusListener ?: return
        FirebaseDatabase.getInstance()
            .reference
            .child("workers")
            .child(workerId)
            .child("currentStatus")
            .removeEventListener(listener)
        statusListener = null
    }

    // Firebase 상태값을 상세 화면에 표시
    private fun renderStatus(status: AdminWorkerStatus) {
        txtWorkerId.text = "workerId : ${status.workerId}"
        txtDeviceId.text = "deviceId : ${status.deviceId}"
        txtWorkLocation.text = "작업 위치 : ${status.displayLocation}"
        txtRisk.text = "현재 위험도 : ${status.riskKorean}"
        txtRisk.setTextColor(riskColor(status.riskLevel))
        txtTemp.text = "피부 온도 : ${status.temp.formatDouble("℃")}"
        txtHeartRate.text = "심박수 : ${status.hr.formatInt("bpm")}"
        txtSpo2.text = "산소포화도 : ${status.spo2.formatInt("%")}"
        txtEnvTemp.text = "주변 온도 : ${status.env.formatDouble("℃")}"
        txtHumidity.text = "습도 : ${status.hum.formatInt("%")}"
        txtLux.text = "조도 : ${status.lux.formatInt("lx")}"
        txtPosture.text = "자세 : ${status.posture}"
        txtBleState.text = "BLE 상태 : ${status.bleState}"
        txtRssi.text = "신호 세기 : ${status.rssiText}"
        txtLastUpdated.text = "마지막 업데이트 시간 : ${status.lastUpdatedRaw}"
    }

    // 데이터가 없을 때 기본값 표시
    private fun showEmptyState() {
        txtWorkerId.text = "workerId : $workerId"
        txtDeviceId.text = "deviceId : -"
        txtWorkLocation.text = "작업 위치 : -"
        txtRisk.text = "현재 위험도 : -"
        txtTemp.text = "피부 온도 : -"
        txtHeartRate.text = "심박수 : -"
        txtSpo2.text = "산소포화도 : -"
        txtEnvTemp.text = "주변 온도 : -"
        txtHumidity.text = "습도 : -"
        txtLux.text = "조도 : -"
        txtPosture.text = "자세 : -"
        txtBleState.text = "BLE 상태 : -"
        txtRssi.text = "신호 세기 : -"
        txtLastUpdated.text = "마지막 업데이트 시간 : -"
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
        return ContextCompat.getColor(this, colorRes)
    }

    // 실수 센서값 표시 형식
    private fun Double?.formatDouble(unit: String): String {
        return this?.let { "%.1f $unit".format(Locale.KOREA, it) } ?: "-"
    }

    // 정수 센서값 표시 형식
    private fun Int?.formatInt(unit: String): String {
        return this?.let { "$it $unit" } ?: "-"
    }
}
