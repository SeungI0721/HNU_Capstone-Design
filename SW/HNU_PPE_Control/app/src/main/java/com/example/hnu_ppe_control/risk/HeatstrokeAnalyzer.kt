package com.example.hnu_ppe_control.risk

import com.example.hnu_ppe_control.data.RiskLevel
import com.example.hnu_ppe_control.data.SensorData
import kotlin.math.abs
import kotlin.math.sqrt

object HeatstrokeAnalyzer {

    private const val HR_HISTORY_SIZE = 5
    private var motionStartTime: Long = 0L
    private var spo2LowStartTime: Long = 0L
    private val hrHistory = ArrayDeque<Int>()

    /**
     * 다중 센서 데이터를 융합한 위험도 산출
     */
    fun analyze(
        data: SensorData,
        baselineHR: Int?
    ): RiskLevel {
        var riskIndex = 0
        val currentTimeMillis = System.currentTimeMillis()

        // 1. 가속도(SVM) 정지 상태 확인 (오직 움직임 강도만 측정)
        val ax = data.ax ?: 0.0
        val ay = data.ay ?: 0.0
        val az = data.az ?: 0.0
        val svm = sqrt(ax * ax + ay * ay + az * az)
        val isCurrentlyStatic = abs(svm - 9.8) < 1.5

        // 2. 무반응 시간에 비례한 점진적 가중치 (시연용 완벽 버전)
var immobilityScore = 0
var isUnresponsive = false

if (isCurrentlyStatic) {
    if (motionStartTime == 0L) motionStartTime = currentTimeMillis
    val staticDuration = (currentTimeMillis - motionStartTime) / 1000

    when {
        staticDuration >= 30 -> {         
            immobilityScore = 100         
            isUnresponsive = true 
        }
        staticDuration >= 20 -> immobilityScore = 20
        staticDuration >= 10 -> immobilityScore = 10
    }
} else {
    // 움직임 감지 시 즉시 타이머 리셋
    motionStartTime = 0L
}
riskIndex += immobilityScore

        // 3. 심박수 노동 보정
        val currentHr = data.hr ?: 0
        val avgHeartRate = averageHeartRate(currentHr)
        val deltaHr = if (baselineHR != null) avgHeartRate - baselineHR else 0
        
        var tempHrScore = 0
        if (deltaHr >= 50) tempHrScore = 40
        else if (deltaHr >= 30) tempHrScore = 25
        else if (deltaHr >= 20) tempHrScore = 10

        // 격렬한 움직임(노동 중)이면 심박수 위험도 절반 삭감
        val highActivity = svm > 15.0
        if (!isCurrentlyStatic && highActivity) {
            tempHrScore /= 2
        }
        riskIndex += tempHrScore

        // 4-1. 온습도 기반 체감온도(Heat Index) 점수
        val heatIndex = (data.env ?: 0.0) + ((data.hum ?: 0).toDouble() * 0.1)
        var heatScore = 0
        if (heatIndex >= 38.0) heatScore = 20
        else if (heatIndex >= 35.0) heatScore = 10
        else if (heatIndex >= 33.0) heatScore = 5
        riskIndex += heatScore

        // 4-2. 조도(Lux) 기반 직사광선 노출 점수
        var luxScore = 0
        val currentLux = data.lux ?: 0
        if (currentLux >= 50000) luxScore = 15 // 한여름 직사광선 수준
        else if (currentLux >= 30000) luxScore = 5
        riskIndex += luxScore

        // 4-3. 체온(MAX30205) Sanity Check 필터 (기존 deltaTemp 대신 절대 체온 검사로 변경)
        var bodyTempScore = 0
        val currentTemp = data.temp ?: 0.0
        if (currentTemp in 35.0..42.0) {
            if (currentTemp >= 39.0) bodyTempScore = 30
            else if (currentTemp >= 38.0) bodyTempScore = 15
        }
        riskIndex += bodyTempScore

        // 5. 최상위 응급(EMERGENCY) 바이패스 판정
        val currentSpo2 = data.spo2 ?: 0
        if (currentSpo2 in 1..84) {
            if (spo2LowStartTime == 0L) spo2LowStartTime = currentTimeMillis
        } else {
            spo2LowStartTime = 0L // 정상 범위면 리셋
        }
        val isSpo2Danger = (spo2LowStartTime != 0L) && ((currentTimeMillis - spo2LowStartTime) >= 5000)

        // 응급 조건: 자세 무관하게 30초 무반응 + 산소포화도 5초 지속 저하
        if (isUnresponsive && isSpo2Danger) {
            return RiskLevel.EMERGENCY
        }

        // 6. 최종 위험도 판정 리턴
        return when {
            riskIndex >= 60 -> RiskLevel.DANGER
            riskIndex >= 30 -> RiskLevel.CAUTION
            else -> RiskLevel.SAFE
        }
    }

    private fun averageHeartRate(currentHr: Int): Int {
        if (hrHistory.size >= HR_HISTORY_SIZE) {
            hrHistory.removeFirst()
        }
        hrHistory.addLast(currentHr)
        return hrHistory.average().toInt()
    }

    // 블루투스 재연결 시 초기화용
    fun resetTimers() {
        motionStartTime = 0L
        spo2LowStartTime = 0L
    }
}
