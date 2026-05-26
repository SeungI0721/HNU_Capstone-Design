// MainActivity의 화면 요소 갱신과 버튼 이벤트 바인딩을 담당하는 파일
package com.example.hnu_ppe_control.ui

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.hnu_ppe_control.R
import com.example.hnu_ppe_control.data.BleSignalLevel
import com.example.hnu_ppe_control.data.RiskLevel
import com.example.hnu_ppe_control.data.SensorData
import com.example.hnu_ppe_control.data.TempBaselineSnapshot
import com.example.hnu_ppe_control.data.WorkSessionState

class MainUiController(
    private val activity: Activity
) {
    private val txtBleState: TextView = activity.findViewById(R.id.txtBleState)
    private val txtBleSignalLevel: TextView = activity.findViewById(R.id.txtBleSignalLevel)
    private val txtWorkLocation: TextView = activity.findViewById(R.id.txtWorkLocation)
    private val txtWorkStartedAt: TextView = activity.findViewById(R.id.txtWorkStartedAt)
    private val layoutRiskCard: View = activity.findViewById(R.id.layoutRiskCard)
    private val txtRiskState: TextView = activity.findViewById(R.id.txtRiskState)
    private val txtRiskMessage: TextView = activity.findViewById(R.id.txtRiskMessage)
    private val txtTempCard: TextView = activity.findViewById(R.id.txtTempCard)
    private val txtHrCard: TextView = activity.findViewById(R.id.txtHrCard)
    private val txtSpo2Card: TextView = activity.findViewById(R.id.txtSpo2Card)
    private val txtEnvCard: TextView = activity.findViewById(R.id.txtEnvCard)
    private val txtWeatherAlert: TextView = activity.findViewById(R.id.txtWeatherAlert)
    private val txtTodayMaxTemp: TextView = activity.findViewById(R.id.txtTodayMaxTemp)
    private val txtWeatherRegion: TextView = activity.findViewById(R.id.txtWeatherRegion)
    private val txtReconnectState: TextView = activity.findViewById(R.id.txtReconnectState)
    private val txtConnectedDevice: TextView = activity.findViewById(R.id.txtConnectedDevice)
    private val txtData: TextView = activity.findViewById(R.id.txtData)
    private val txtRiskCommand: TextView = activity.findViewById(R.id.txtRiskCommand)
    private val txtFirebaseState: TextView = activity.findViewById(R.id.txtFirebaseState)
    private val txtLastUpdate: TextView = activity.findViewById(R.id.txtLastUpdate)
    private val btnWorkToggle: Button = activity.findViewById(R.id.btnScan)
    private val btnDisconnect: Button = activity.findViewById(R.id.btnDisconnect)
    private val btnFakeData: Button = activity.findViewById(R.id.btnFakeData)
    private val btnDetailData: Button = activity.findViewById(R.id.btnDetailData)

    fun bindActions(
        onWorkButtonClicked: () -> Unit,
        onDisconnectClicked: () -> Unit,
        onFakeDataClicked: () -> Unit,
        onDetailClicked: () -> Unit
    ) {
        btnWorkToggle.setOnClickListener { onWorkButtonClicked() }
        btnDisconnect.setOnClickListener { onDisconnectClicked() }
        btnFakeData.setOnClickListener { onFakeDataClicked() }
        btnDetailData.setOnClickListener { onDetailClicked() }
    }

    fun showDefault(bleAvailable: Boolean) {
        txtBleState.text = if (bleAvailable) "연결 대기" else "사용 불가"
        txtBleSignalLevel.text = BleSignalLevel.NOT_CONNECTED.label
        txtWorkLocation.text = "미선택"
        txtWorkStartedAt.text = "-"
        txtTempCard.text = "- ℃"
        txtHrCard.text = "- bpm"
        txtSpo2Card.text = "- %"
        txtEnvCard.text = "- ℃ / - %"
        txtWeatherAlert.text = "연동 대기"
        txtTodayMaxTemp.text = "-"
        txtWeatherRegion.text = "미설정"
        txtReconnectState.text = "대기"
        txtConnectedDevice.text = "연결 장치 없음"
        txtData.text = "센서 데이터 없음"
        txtRiskCommand.text = "ESP32 명령 없음"
        txtFirebaseState.text = "Firebase 대기"
        txtLastUpdate.text = "없음"
        showFakeDataButton(false)
        showRisk(RiskLevel.SAFE)
        updateWorkSessionState(WorkSessionState.IDLE)
    }

    fun showFakeDataButton(visible: Boolean) {
        activity.runOnUiThread {
            btnFakeData.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    fun updateWorkSessionState(state: WorkSessionState) {
        activity.runOnUiThread {
            btnWorkToggle.text = when (state) {
                WorkSessionState.IDLE,
                WorkSessionState.ENDED -> "작업 시작"
                WorkSessionState.CONNECTING -> "연결 시도 중"
                WorkSessionState.WORKING,
                WorkSessionState.RECONNECTING -> "작업 종료"
            }
            btnWorkToggle.isEnabled = true
        }
    }

    fun showBleState(value: String) {
        activity.runOnUiThread { txtBleState.text = value }
    }

    fun showBleSignal(level: BleSignalLevel) {
        activity.runOnUiThread { txtBleSignalLevel.text = level.label }
    }

    fun showWorkLocation(name: String?) {
        activity.runOnUiThread { txtWorkLocation.text = name?.takeIf { it.isNotBlank() } ?: "미선택" }
    }

    fun showWorkStartedAt(value: String?) {
        activity.runOnUiThread { txtWorkStartedAt.text = value?.takeIf { it.isNotBlank() } ?: "-" }
    }

    fun showWeather(alert: String, todayMaxTemp: Double?, region: String) {
        activity.runOnUiThread {
            txtWeatherAlert.text = alert
            txtTodayMaxTemp.text = todayMaxTemp?.let { "%.1f ℃".format(it) } ?: "-"
            txtWeatherRegion.text = region
        }
    }

    fun showConnectedDevice(deviceName: String, address: String) {
        activity.runOnUiThread { txtConnectedDevice.text = "$deviceName / $address" }
    }

    fun showNoConnectedDevice() {
        activity.runOnUiThread { txtConnectedDevice.text = "연결 장치 없음" }
    }

    fun showReconnectStatus(message: String) {
        activity.runOnUiThread { txtReconnectState.text = message }
    }

    fun showSensorData(
        sensorData: SensorData,
        riskLevel: RiskLevel,
        formattedTime: String,
        tempSnapshot: TempBaselineSnapshot
    ) {
        val axisText = "X:${sensorData.ax?.let { "%.2f".format(it) } ?: "-"} " +
            "Y:${sensorData.ay?.let { "%.2f".format(it) } ?: "-"} " +
            "Z:${sensorData.az?.let { "%.2f".format(it) } ?: "-"}"
        val tempText = formatTempText(sensorData, tempSnapshot)
        val detailText = """
            workerId: ${sensorData.id}
            TEMP 피부온도: ${sensorData.temp}
            HR 심박수: ${sensorData.hr}
            SPO2 산소포화도: ${sensorData.spo2?.toString() ?: "-"}
            ENV 주변 온도: ${sensorData.env}
            HUM 습도: ${sensorData.hum}
            LUX 조도: ${sensorData.lux}
            MPU X/Y/Z: $axisText
            POSTURE 자세: ${sensorData.posture}
            위험 단계: ${riskLevel.label}
        """.trimIndent() + "\nTEMP 피부 접촉 온도: $tempText\nTEMP 상태: ${sensorData.tempSource} / ${tempSnapshot.status}"

        activity.runOnUiThread {
            txtTempCard.text = "%.1f ℃".format(sensorData.temp)
            txtTempCard.text = tempText
            txtHrCard.text = "${sensorData.hr} bpm"
            txtSpo2Card.text = sensorData.spo2?.let { "$it %" } ?: "-"
            txtEnvCard.text = "%.1f ℃ / %d %%".format(sensorData.env, sensorData.hum)
            txtData.text = detailText
            txtLastUpdate.text = formattedTime
        }
    }

    fun showRisk(riskLevel: RiskLevel) {
        activity.runOnUiThread {
            txtRiskState.text = riskLevel.label
            txtRiskMessage.text = riskMessageFor(riskLevel)
            layoutRiskCard.setBackgroundResource(backgroundForRisk(riskLevel))
        }
    }

    private fun formatTempText(sensorData: SensorData, tempSnapshot: TempBaselineSnapshot): String {
        if (!sensorData.tempValid) return "측정 불가"
        val stableDelta = tempSnapshot.stableDeltaTemp
        return if (tempSnapshot.baselineTempReady && stableDelta != null) {
            "%.1f ℃ / 기준 대비 %+.1f ℃".format(sensorData.temp, stableDelta)
        } else {
            "%.1f ℃ (기준값 측정 중)".format(sensorData.temp)
        }
    }

    fun showRiskCommand(command: String) {
        activity.runOnUiThread { txtRiskCommand.text = command }
    }

    fun showWriteResult(command: String, started: Boolean, reason: String?) {
        activity.runOnUiThread {
            txtRiskCommand.text = when {
                started -> "$command 전송 시작"
                command.isBlank() -> "Write 특성 없음"
                reason == "duplicate" -> "$command 중복 생략"
                else -> "$command 전송 실패($reason)"
            }
        }
    }

    fun showFirebaseState(message: String) {
        activity.runOnUiThread { txtFirebaseState.text = message }
    }

    fun showParseError(rawData: String) {
        activity.runOnUiThread {
            txtData.text = "센서 데이터 확인 필요\n$rawData"
            showRisk(RiskLevel.ERROR)
            txtFirebaseState.text = "파싱 실패로 업로드 생략"
        }
    }

    private fun riskMessageFor(riskLevel: RiskLevel): String {
        return when (riskLevel) {
            RiskLevel.SAFE -> "현재 작업 상태가 안정적입니다."
            RiskLevel.CAUTION -> "수분 섭취와 휴식을 권장합니다."
            RiskLevel.DANGER -> "위험 상태가 감지되었습니다. 즉시 휴식하세요."
            RiskLevel.EMERGENCY -> "응급 상태가 감지되었습니다. 즉시 구조를 요청하세요."
            RiskLevel.ERROR -> "센서 데이터 확인이 필요합니다."
        }
    }

    private fun backgroundForRisk(riskLevel: RiskLevel): Int {
        return when (riskLevel) {
            RiskLevel.SAFE -> R.drawable.bg_status_normal
            RiskLevel.CAUTION -> R.drawable.bg_status_warning
            RiskLevel.DANGER,
            RiskLevel.EMERGENCY -> R.drawable.bg_status_danger
            RiskLevel.ERROR -> R.drawable.bg_status_error
        }
    }
}
