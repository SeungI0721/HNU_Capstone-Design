// 센서값을 조합해 작업자 온열·자세 위험 단계를 계산하는 파일
package com.example.hnu_ppe_control.risk

import com.example.hnu_ppe_control.data.RiskLevel
import com.example.hnu_ppe_control.data.SensorData
import kotlin.math.abs
import kotlin.math.sqrt

object HeatstrokeAnalyzer {

    // 짧은 순간의 심박 변화보다 최근 흐름을 보도록 고정 길이 이력을 사용합니다.
    private const val HR_HISTORY_SIZE = 5
    private var motionStartTime: Long = 0L
    private var spo2LowStartTime: Long = 0L
    private val hrHistory = ArrayDeque<Int>()

    // 온도, 심박, 움직임, 산소포화도, 환경값을 점수화해 최종 위험 단계를 산출합니다.
    fun analyze(
        data: SensorData,
        baselineHR: Int?
    ): RiskLevel {
        var riskIndex = 0
        val currentTimeMillis = System.currentTimeMillis()

        // 가속도 벡터 크기로 정지 상태를 판단해 장시간 무반응 여부를 추적합니다.
        val ax = data.ax ?: 0.0
        val ay = data.ay ?: 0.0
        val az = data.az ?: 0.0
        val svm = sqrt(ax * ax + ay * ay + az * az)
        val isCurrentlyStatic = abs(svm - 9.8) < 1.5

        // 정지 상태가 길어질수록 위험 점수를 높이고, 30초 이상이면 응급 후보로 봅니다.
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
    // 움직임이 다시 감지되면 무반응 누적 시간을 초기화합니다.
    motionStartTime = 0L
}
riskIndex += immobilityScore

        // 작업 시작 시 기준 심박과 최근 평균 심박의 차이를 사용해 일시적인 튐을 줄입니다.
        val currentHr = data.hr ?: 0
        val avgHeartRate = averageHeartRate(currentHr)
        val deltaHr = if (baselineHR != null) avgHeartRate - baselineHR else 0
        
        var tempHrScore = 0
        if (deltaHr >= 50) tempHrScore = 40
        else if (deltaHr >= 30) tempHrScore = 25
        else if (deltaHr >= 20) tempHrScore = 10

        // 격렬한 작업 중에는 심박 상승이 자연스러울 수 있어 심박 점수를 절반만 반영합니다.
        val highActivity = svm > 15.0
        if (!isCurrentlyStatic && highActivity) {
            tempHrScore /= 2
        }
        riskIndex += tempHrScore

        // 주변 온도와 습도를 단순 가중해 현장 체감 열 부담을 반영합니다.
        val heatIndex = (data.env ?: 0.0) + ((data.hum ?: 0).toDouble() * 0.1)
        var heatScore = 0
        if (heatIndex >= 38.0) heatScore = 20
        else if (heatIndex >= 35.0) heatScore = 10
        else if (heatIndex >= 33.0) heatScore = 5
        riskIndex += heatScore

        // 조도는 직사광선 노출 가능성을 보는 보조 지표로만 사용합니다.
        var luxScore = 0
        val currentLux = data.lux ?: 0
        if (currentLux >= 50000) luxScore = 15 // 한여름 직사광선 수준
        else if (currentLux >= 30000) luxScore = 5
        riskIndex += luxScore

        // 피부 온도는 비정상 범위 값을 제외하고 절대값 기준으로만 가산합니다.
        var bodyTempScore = 0
        val currentTemp = data.temp ?: 0.0
        if (currentTemp in 35.0..42.0) {
            if (currentTemp >= 39.0) bodyTempScore = 30
            else if (currentTemp >= 38.0) bodyTempScore = 15
        }
        riskIndex += bodyTempScore

        // 산소포화도 저하는 일시 오류가 많아 5초 이상 지속될 때만 응급 조건에 반영합니다.
        val currentSpo2 = data.spo2 ?: 0
        if (currentSpo2 in 1..84) {
            if (spo2LowStartTime == 0L) spo2LowStartTime = currentTimeMillis
        } else {
            spo2LowStartTime = 0L
        }
        val isSpo2Danger = (spo2LowStartTime != 0L) && ((currentTimeMillis - spo2LowStartTime) >= 5000)

        // 응급 조건은 위험 점수와 별도로 무반응과 산소포화도 저하가 동시에 지속될 때 우선 적용합니다.
        if (isUnresponsive && isSpo2Danger) {
            return RiskLevel.EMERGENCY
        }

        // 누적 점수 기준으로 일반 위험 단계를 결정합니다.
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

    // BLE 재연결 뒤 이전 세션의 무반응·SpO2 누적 시간이 남지 않도록 초기화합니다.
    fun resetTimers() {
        motionStartTime = 0L
        spo2LowStartTime = 0L
    }
}
