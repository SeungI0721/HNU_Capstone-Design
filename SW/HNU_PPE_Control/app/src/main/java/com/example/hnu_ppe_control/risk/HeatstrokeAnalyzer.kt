// 센서 데이터와 작업 시작 기준값으로 Smart Shield 위험 단계를 계산하는 파일
package com.example.hnu_ppe_control.risk

import com.example.hnu_ppe_control.data.RiskLevel
import com.example.hnu_ppe_control.data.SensorData
import kotlin.math.max

object HeatstrokeAnalyzer {

    private const val DEFAULT_BASELINE_TEMP = 36.5
    private const val DEFAULT_BASELINE_HR = 80

    // 기존 호출부 호환용 기본 분석
    fun analyze(data: SensorData): RiskLevel {
        return analyze(
            data = data,
            baselineTemp = DEFAULT_BASELINE_TEMP,
            baselineHR = DEFAULT_BASELINE_HR
        )
    }

    // 기준 피부온도와 기준 심박수 대비 변화량 중심 위험도 계산
    fun analyze(
        data: SensorData,
        baselineTemp: Double,
        baselineHR: Int,
        motionAbnormal: Boolean = false,
        isHighActivity: Boolean = false
    ): RiskLevel {
        if (!isValidSensorData(data)) {
            return RiskLevel.SAFE
        }

        val posture = data.posture.uppercase()

        // 낙상 또는 응급 자세는 즉시 응급
        if (posture == "FALL" || posture == "EMERGENCY") {
            return RiskLevel.EMERGENCY
        }

        val safeBaselineTemp = sanitizeBaselineTemp(baselineTemp)
        val safeBaselineHr = sanitizeBaselineHr(baselineHR)
        val deltaTemp = data.temp - safeBaselineTemp
        val deltaHR = data.hr - safeBaselineHr
        val heatIndexLikeScore = data.env + (0.05 * data.hum)

        var riskIndex = 0

        // 심박수 상승량 점수
        var heartRateScore = when {
            deltaHR >= 50 -> 40
            deltaHR >= 30 -> 25
            deltaHR >= 20 -> 10
            else -> 0
        }

        // 고강도 작업 중에는 심박 상승 점수 완화
        if (isHighActivity) {
            heartRateScore /= 2
        }
        riskIndex += heartRateScore

        // 피부온도 상승량 점수
        riskIndex += when {
            deltaTemp >= 2.0 -> 40
            deltaTemp >= 1.0 -> 25
            deltaTemp >= 0.5 -> 10
            else -> 0
        }

        // 주변 온습도 복합 점수
        riskIndex += when {
            heatIndexLikeScore >= 38.0 -> 20
            heatIndexLikeScore >= 35.0 -> 10
            heatIndexLikeScore >= 33.0 -> 5
            else -> 0
        }

        // 높은 습도 보조 점수
        riskIndex += when {
            data.hum >= 85 -> 10
            data.hum >= 75 -> 5
            else -> 0
        }

        // 강한 조도 보조 점수
        riskIndex += when {
            data.lux >= 50000 -> 15
            data.lux >= 30000 -> 5
            else -> 0
        }

        // 불안정 자세 보조 점수
        if (posture == "WARNING" || posture == "UNSTABLE") {
            riskIndex += 20
        }

        // 장시간 무반응 등 외부 모션 이상 보조 점수
        if (motionAbnormal) {
            riskIndex += 40
        }

        val spo2 = data.spo2
        val spo2Emergency = spo2 != null && spo2 in 50..84

        // 즉시 응급 조건
        if (
            deltaTemp >= 3.0 ||
            (deltaTemp >= 2.0 && motionAbnormal) ||
            spo2Emergency
        ) {
            return RiskLevel.EMERGENCY
        }

        return when {
            riskIndex >= 80 -> RiskLevel.EMERGENCY
            riskIndex >= 55 -> RiskLevel.DANGER
            riskIndex >= 25 -> RiskLevel.CAUTION
            else -> RiskLevel.SAFE
        }
    }

    // 정수 위험도 단계가 필요한 코드용 보조 함수
    fun analyzeAsInt(
        data: SensorData,
        baselineTemp: Double = DEFAULT_BASELINE_TEMP,
        baselineHR: Int = DEFAULT_BASELINE_HR,
        motionAbnormal: Boolean = false,
        isHighActivity: Boolean = false
    ): Int {
        return when (
            analyze(
                data = data,
                baselineTemp = baselineTemp,
                baselineHR = baselineHR,
                motionAbnormal = motionAbnormal,
                isHighActivity = isHighActivity
            )
        ) {
            RiskLevel.SAFE -> 0
            RiskLevel.CAUTION -> 1
            RiskLevel.DANGER -> 2
            RiskLevel.EMERGENCY -> 3
            RiskLevel.ERROR -> 0
        }
    }

    // 알고리즘 입력값 유효성 검사
    private fun isValidSensorData(data: SensorData): Boolean {
        if (data.temp.isNaN() || data.env.isNaN()) return false

        return data.temp >= 20.0 && data.temp <= 50.0 &&
            data.hr >= 30 && data.hr <= 220 &&
            data.env >= -40.0 && data.env <= 85.0 &&
            data.hum >= 0 && data.hum <= 100 &&
            data.lux >= 0 &&
            (data.spo2 == null || data.spo2 >= 50 && data.spo2 <= 100)
    }

    // 기준 피부온도 보정
    fun sanitizeBaselineTemp(value: Double?): Double {
        return when {
            value == null -> DEFAULT_BASELINE_TEMP
            value.isNaN() -> DEFAULT_BASELINE_TEMP
            value !in 20.0..45.0 -> DEFAULT_BASELINE_TEMP
            else -> value
        }
    }

    // 기준 심박수 보정
    fun sanitizeBaselineHr(value: Int?): Int {
        return when {
            value == null -> DEFAULT_BASELINE_HR
            value !in 30..180 -> DEFAULT_BASELINE_HR
            else -> max(value, 30)
        }
    }
}
