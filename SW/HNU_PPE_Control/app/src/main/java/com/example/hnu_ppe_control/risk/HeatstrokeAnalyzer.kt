// 센서 데이터와 작업 시작 기준값으로 Smart Shield 위험 단계를 계산하는 파일
package com.example.hnu_ppe_control.risk

import com.example.hnu_ppe_control.data.RiskLevel
import com.example.hnu_ppe_control.data.SensorData
import kotlin.math.max
import kotlin.math.sqrt

object HeatstrokeAnalyzer {

    private const val DEFAULT_BASELINE_TEMP = 36.5
    private const val DEFAULT_BASELINE_HR = 80
    private const val MOTION_STOP_THRESHOLD_MS = 30_000L

    private var motionStartTime = 0L
    private var isMotionStopped = false

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
        baselineHR: Int
    ): RiskLevel {
        if (!isValidSensorData(data)) {
            resetMotionState()
            return RiskLevel.ERROR
        }

        val posture = data.posture.uppercase()

        // 낙상 또는 응급 자세는 즉시 응급 처리
        if (posture == "FALL" || posture == "EMERGENCY") {
            resetMotionState()
            return RiskLevel.EMERGENCY
        }

        val safeBaselineTemp = sanitizeBaselineTemp(baselineTemp)
        val safeBaselineHr = sanitizeBaselineHr(baselineHR)
        val motionResult = analyzeMotion(data.accX, data.accY, data.accZ)

        // 피부온도를 내부 위험 계산용 보정 피부온도로 변환
        val adjustedSkinTemp = data.temp + 0.1 * (data.temp - data.env) + 0.5
        val deltaTemp = adjustedSkinTemp - safeBaselineTemp
        val deltaHR = data.hr - safeBaselineHr
        val heatIndexLikeScore = data.env + (0.05 * data.hum)

        var riskIndex = 0

        // 심박수 변화량 점수
        var heartRateScore = when {
            deltaHR >= 50 -> 40
            deltaHR >= 30 -> 25
            deltaHR >= 20 -> 10
            else -> 0
        }

        // 큰 움직임 중에는 작업 강도에 의한 심박 상승 가능성을 반영
        if (motionResult.isHighActivity) {
            heartRateScore /= 2
        }
        riskIndex += heartRateScore

        // 피부온도 변화량 점수
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

        // 30초 이상 무반응 감지 점수
        if (motionResult.motionAbnormal) {
            riskIndex += 40
        }

        // 조도 기반 직사광선 보조 점수
        riskIndex += when {
            data.lux >= 50000 -> 15
            data.lux >= 30000 -> 5
            else -> 0
        }

        val spo2Emergency = data.spo2 != null && data.spo2 < 85

        // 급격한 온도 상승, 무반응 동반, 산소포화도 급락은 즉시 응급
        if (
            deltaTemp >= 3.0 ||
            (deltaTemp >= 2.0 && motionResult.motionAbnormal) ||
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

    // 알고리즘 담당 코드의 정수 위험도 반환 방식 호환
    fun analyzeAsInt(
        data: SensorData,
        baselineTemp: Double = DEFAULT_BASELINE_TEMP,
        baselineHR: Int = DEFAULT_BASELINE_HR
    ): Int {
        return when (
            analyze(
                data = data,
                baselineTemp = baselineTemp,
                baselineHR = baselineHR
            )
        ) {
            RiskLevel.SAFE -> 0
            RiskLevel.CAUTION -> 1
            RiskLevel.DANGER -> 2
            RiskLevel.EMERGENCY -> 3
            RiskLevel.ERROR -> 0
        }
    }

    // CSV 입력 테스트용 호환 함수
    fun analyzeCsvAsInt(
        csvData: String?,
        baselineTemp: Double = DEFAULT_BASELINE_TEMP,
        baselineHR: Int = DEFAULT_BASELINE_HR
    ): Int {
        if (csvData.isNullOrBlank()) return 0

        return try {
            val values = csvData.split(",")
            if (values.size != 9) return 0

            val data = SensorData(
                id = "0001",
                temp = values[0].trim().toDouble(),
                hr = values[1].trim().toInt(),
                env = values[2].trim().toDouble(),
                hum = values[3].trim().toInt(),
                ax = values[4].trim().toDouble(),
                ay = values[5].trim().toDouble(),
                az = values[6].trim().toDouble(),
                spo2 = values[7].trim().toInt(),
                lux = values[8].trim().toInt(),
                posture = "NORMAL"
            )

            analyzeAsInt(
                data = data,
                baselineTemp = baselineTemp,
                baselineHR = baselineHR
            )
        } catch (e: Exception) {
            0
        }
    }

    // MPU6050 가속도 벡터와 무반응 상태 분석
    private fun analyzeMotion(accX: Double?, accY: Double?, accZ: Double?): MotionResult {
        if (accX == null || accY == null || accZ == null) {
            resetMotionState()
            return MotionResult(
                accelVector = null,
                isStill = false,
                isHighActivity = false,
                motionAbnormal = false
            )
        }

        if (accX.isNaN() || accY.isNaN() || accZ.isNaN()) {
            resetMotionState()
            return MotionResult(
                accelVector = null,
                isStill = false,
                isHighActivity = false,
                motionAbnormal = false
            )
        }

        val accelVector = sqrt(accX * accX + accY * accY + accZ * accZ)
        val isStill = accelVector > 8.5 && accelVector < 11.0
        val isHighActivity = accelVector > 15.0
        var motionAbnormal = false
        val currentTime = System.currentTimeMillis()

        if (isStill) {
            if (!isMotionStopped) {
                motionStartTime = currentTime
                isMotionStopped = true
            } else if (currentTime - motionStartTime >= MOTION_STOP_THRESHOLD_MS) {
                motionAbnormal = true
            }
        } else {
            resetMotionState()
        }

        return MotionResult(
            accelVector = accelVector,
            isStill = isStill,
            isHighActivity = isHighActivity,
            motionAbnormal = motionAbnormal
        )
    }

    // 무반응 타이머 초기화
    fun resetMotionState() {
        isMotionStopped = false
        motionStartTime = 0L
    }

    // 알고리즘 입력값 유효성 검사
    private fun isValidSensorData(data: SensorData): Boolean {
        val spo2 = data.spo2
        val accX = data.accX
        val accY = data.accY
        val accZ = data.accZ

        if (data.temp.isNaN() || data.env.isNaN()) return false
        if (accX != null && accX.isNaN()) return false
        if (accY != null && accY.isNaN()) return false
        if (accZ != null && accZ.isNaN()) return false

        return data.temp in 20.0..50.0 &&
            data.hr in 30..220 &&
            data.env in -40.0..85.0 &&
            data.hum in 0..100 &&
            data.lux >= 0 &&
            (spo2 == null || spo2 in 50..100)
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

    private data class MotionResult(
        val accelVector: Double?,
        val isStill: Boolean,
        val isHighActivity: Boolean,
        val motionAbnormal: Boolean
    )
}
