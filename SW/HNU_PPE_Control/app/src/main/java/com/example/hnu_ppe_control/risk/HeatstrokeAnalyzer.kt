package com.example.hnu_ppe_control.risk

import com.example.hnu_ppe_control.data.RiskLevel
import com.example.hnu_ppe_control.data.SensorData
import kotlin.math.abs
import kotlin.math.sqrt

object HeatstrokeAnalyzer {

    private const val HR_HISTORY_SIZE = 5
    private const val MOTION_STATIC_TIMEOUT_MS = 30_000L
    private const val GRAVITY = 9.8

    private var motionStartTime: Long = 0L
    private val hrHistory = ArrayDeque<Int>()
    private var spo2LowCount = 0

    // MainActivity에서 전달하는 기준값이 센서 범위를 벗어나면 위험도 계산에서 제외합니다.
    fun sanitizeBaselineTemp(value: Double?): Double? {
        return value?.takeIf { it in 20.0..50.0 }
    }

    // 작업 시작 직후 저장한 심박 기준값이 비정상 범위이면 심박 변화량 계산에서 제외합니다.
    fun sanitizeBaselineHr(value: Int?): Int? {
        return value?.takeIf { it in 30..220 }
    }

    fun analyze(
        data: SensorData,
        baselineTemp: Double?,
        baselineHR: Int?,
        tempBaselineReady: Boolean,
        stableDeltaTemp: Double?,
        motionAbnormal: Boolean = false,
        isHighActivity: Boolean = false
    ): RiskLevel {
        val posture = data.posture.uppercase()
        if (posture == "FALL" || posture == "EMERGENCY") {
            return RiskLevel.EMERGENCY
        }

        val motionState = analyzeMotion(data)
        val abnormalMotion = motionAbnormal || motionState.motionAbnormal || posture == "UNSTABLE"
        val highActivity = isHighActivity || motionState.highActivity
        val averagedHr = averageHeartRate(data.hr)
        val deltaHr = baselineHR?.let { averagedHr - it }
        val environmentRiskIndicator = data.env + (data.hum * 0.1)

        updateSpo2State(data.spo2)

        var riskScore = 0
        riskScore += heartRateScore(deltaHr, highActivity)
        riskScore += temperatureScore(
            data = data,
            baselineTemp = baselineTemp,
            tempBaselineReady = tempBaselineReady,
            stableDeltaTemp = stableDeltaTemp
        )
        riskScore += environmentScore(environmentRiskIndicator)
        riskScore += postureScore(posture, abnormalMotion)
        riskScore += sunlightScore(data.lux)
        riskScore += spo2Score(abnormalMotion)

        if (isTemperatureEmergencyCandidate(
                data = data,
                baselineTemp = baselineTemp,
                tempBaselineReady = tempBaselineReady,
                stableDeltaTemp = stableDeltaTemp,
                environmentRiskIndicator = environmentRiskIndicator,
                abnormalMotion = abnormalMotion,
                deltaHr = deltaHr
            )
        ) {
            return RiskLevel.EMERGENCY
        }

        if (spo2LowCount >= 5 && abnormalMotion) {
            return RiskLevel.EMERGENCY
        }

        return when {
            riskScore >= 70 -> RiskLevel.EMERGENCY
            riskScore >= 50 -> RiskLevel.DANGER
            riskScore >= 25 -> RiskLevel.CAUTION
            else -> RiskLevel.SAFE
        }
    }

    // 예전 테스트 코드가 정수 위험도를 호출할 수 있어 호환용으로 유지합니다.
    fun calculateDangerLevel(bleData: String?, baselineTemp: Double, baselineHR: Int): Int {
        if (bleData.isNullOrBlank()) return 0

        return try {
            val values = bleData.split(",")
            if (values.size != 9) return 0

            val data = SensorData(
                id = "0000",
                temp = values[0].toDouble(),
                tempValid = true,
                tempSource = "LEGACY",
                hr = values[1].toInt(),
                env = values[2].toDouble(),
                hum = values[3].toInt(),
                ax = values[4].toDouble(),
                ay = values[5].toDouble(),
                az = values[6].toDouble(),
                spo2 = values[7].toInt(),
                lux = values[8].toInt(),
                posture = "NORMAL"
            )

            when (
                analyze(
                    data = data,
                    baselineTemp = baselineTemp,
                    baselineHR = baselineHR,
                    tempBaselineReady = true,
                    stableDeltaTemp = data.temp - baselineTemp
                )
            ) {
                RiskLevel.SAFE -> 0
                RiskLevel.CAUTION -> 1
                RiskLevel.DANGER -> 2
                RiskLevel.EMERGENCY -> 3
                RiskLevel.ERROR -> 0
            }
        } catch (_: Exception) {
            0
        }
    }

    private fun averageHeartRate(currentHr: Int): Int {
        if (hrHistory.size >= HR_HISTORY_SIZE) {
            hrHistory.removeFirst()
        }
        hrHistory.addLast(currentHr)
        return hrHistory.average().toInt()
    }

    private fun updateSpo2State(spo2: Int?) {
        if (spo2 != null && spo2 in 50..84) {
            spo2LowCount++
        } else {
            spo2LowCount = 0
        }
    }

    private fun heartRateScore(deltaHr: Int?, highActivity: Boolean): Int {
        if (deltaHr == null) return 0

        val score = when {
            deltaHr >= 50 -> 40
            deltaHr >= 30 -> 25
            deltaHr >= 20 -> 10
            else -> 0
        }

        // 활동량이 큰 순간에는 심박 상승을 일부 완화해 오탐을 줄입니다.
        return if (highActivity) score / 2 else score
    }

    private fun temperatureScore(
        data: SensorData,
        baselineTemp: Double?,
        tempBaselineReady: Boolean,
        stableDeltaTemp: Double?
    ): Int {
        if (!canUseTemperatureRisk(data, baselineTemp, tempBaselineReady, stableDeltaTemp)) {
            return 0
        }

        return when {
            stableDeltaTemp!! >= 2.0 -> 35
            stableDeltaTemp >= 1.0 -> 25
            stableDeltaTemp >= 0.5 -> 10
            else -> 0
        }
    }

    private fun environmentScore(environmentRiskIndicator: Double): Int {
        return when {
            environmentRiskIndicator >= 38.0 -> 20
            environmentRiskIndicator >= 35.0 -> 10
            environmentRiskIndicator >= 33.0 -> 5
            else -> 0
        }
    }

    private fun postureScore(posture: String, abnormalMotion: Boolean): Int {
        return when {
            abnormalMotion -> 35
            posture == "WARNING" -> 10
            else -> 0
        }
    }

    private fun sunlightScore(lux: Int): Int {
        return when {
            lux >= 50_000 -> 15
            lux >= 30_000 -> 5
            else -> 0
        }
    }

    private fun spo2Score(abnormalMotion: Boolean): Int {
        return if (spo2LowCount >= 3 && abnormalMotion) 15 else 0
    }

    private fun canUseTemperatureRisk(
        data: SensorData,
        baselineTemp: Double?,
        tempBaselineReady: Boolean,
        stableDeltaTemp: Double?
    ): Boolean {
        return data.tempValid &&
            baselineTemp != null &&
            tempBaselineReady &&
            stableDeltaTemp != null
    }

    private fun isTemperatureEmergencyCandidate(
        data: SensorData,
        baselineTemp: Double?,
        tempBaselineReady: Boolean,
        stableDeltaTemp: Double?,
        environmentRiskIndicator: Double,
        abnormalMotion: Boolean,
        deltaHr: Int?
    ): Boolean {
        if (!canUseTemperatureRisk(data, baselineTemp, tempBaselineReady, stableDeltaTemp)) {
            return false
        }

        val hasCombinedRisk = environmentRiskIndicator >= 35.0 ||
            abnormalMotion ||
            (deltaHr != null && deltaHr >= 30)

        return stableDeltaTemp!! >= 2.5 && hasCombinedRisk
    }

    private fun analyzeMotion(data: SensorData): MotionState {
        val ax = data.ax ?: return MotionState()
        val ay = data.ay ?: return MotionState()
        val az = data.az ?: return MotionState()
        val svm = sqrt(ax * ax + ay * ay + az * az)
        val staticPosture = abs(svm - GRAVITY) < 1.5
        val highActivity = svm > 15.0

        if (!staticPosture) {
            motionStartTime = 0L
            return MotionState(motionAbnormal = false, highActivity = highActivity)
        }

        if (motionStartTime == 0L) {
            motionStartTime = System.currentTimeMillis()
            return MotionState(highActivity = highActivity)
        }

        val motionAbnormal = System.currentTimeMillis() - motionStartTime > MOTION_STATIC_TIMEOUT_MS &&
            abs(ay) < 5.0

        return MotionState(motionAbnormal = motionAbnormal, highActivity = highActivity)
    }

    private data class MotionState(
        val motionAbnormal: Boolean = false,
        val highActivity: Boolean = false
    )
}
