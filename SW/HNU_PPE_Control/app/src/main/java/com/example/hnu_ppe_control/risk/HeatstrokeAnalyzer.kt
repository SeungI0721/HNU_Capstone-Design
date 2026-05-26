package com.example.hnu_ppe_control.risk

import com.example.hnu_ppe_control.data.RiskLevel
import com.example.hnu_ppe_control.data.SensorData
import kotlin.math.max
import kotlin.math.sqrt

object HeatstrokeAnalyzer {

    private const val DEFAULT_BASELINE_HR = 80
    private const val MOTION_STOP_THRESHOLD_MS = 30_000L

    private var motionStartTime = 0L
    private var isMotionStopped = false

    fun analyze(data: SensorData): RiskLevel {
        return analyze(
            data = data,
            baselineTemp = null,
            baselineHR = DEFAULT_BASELINE_HR,
            tempBaselineReady = false,
            stableDeltaTemp = null
        )
    }

    fun analyze(
        data: SensorData,
        baselineTemp: Double?,
        baselineHR: Int?,
        tempBaselineReady: Boolean = false,
        stableDeltaTemp: Double? = null,
        motionAbnormal: Boolean = false,
        isHighActivity: Boolean = false
    ): RiskLevel {
        if (!isValidSensorData(data)) {
            resetMotionState()
            return RiskLevel.ERROR
        }

        val safeBaselineHr = sanitizeBaselineHr(baselineHR)
        val motionResult = analyzeMotion(data.accX, data.accY, data.accZ)
        val effectiveMotionAbnormal = motionAbnormal || motionResult.motionAbnormal
        val effectiveHighActivity = isHighActivity || motionResult.isHighActivity
        val deltaHR = data.hr - safeBaselineHr
        val environmentRiskIndicator = data.env + (0.05 * data.hum)
        val canUseTempRisk = data.tempValid &&
            tempBaselineReady &&
            baselineTemp != null &&
            stableDeltaTemp != null

        var riskIndex = 0

        var heartRateScore = when {
            deltaHR >= 50 -> 40
            deltaHR >= 30 -> 25
            deltaHR >= 20 -> 10
            else -> 0
        }
        if (effectiveHighActivity) heartRateScore /= 2
        riskIndex += heartRateScore

        if (canUseTempRisk) {
            riskIndex += when {
                stableDeltaTemp >= 2.0 -> 35
                stableDeltaTemp >= 1.0 -> 25
                stableDeltaTemp >= 0.5 -> 10
                else -> 0
            }
        }

        riskIndex += when {
            environmentRiskIndicator >= 38.0 -> 20
            environmentRiskIndicator >= 35.0 -> 10
            environmentRiskIndicator >= 33.0 -> 5
            else -> 0
        }

        if (effectiveMotionAbnormal) riskIndex += 40

        riskIndex += when {
            data.lux >= 50000 -> 15
            data.lux >= 30000 -> 5
            else -> 0
        }

        val spo2Emergency = data.spo2 != null && data.spo2 < 85
        val postureEmergency = data.posture == "FALL" || data.posture == "EMERGENCY"
        val tempEmergencyCandidate = canUseTempRisk &&
            stableDeltaTemp >= 2.5 &&
            (
                environmentRiskIndicator >= 35.0 ||
                    effectiveMotionAbnormal ||
                    deltaHR >= 30
            )

        if (postureEmergency || effectiveMotionAbnormal || spo2Emergency || tempEmergencyCandidate) {
            return RiskLevel.EMERGENCY
        }

        return when {
            riskIndex >= 80 -> RiskLevel.EMERGENCY
            riskIndex >= 55 -> RiskLevel.DANGER
            riskIndex >= 25 -> RiskLevel.CAUTION
            else -> RiskLevel.SAFE
        }
    }

    fun analyzeAsInt(
        data: SensorData,
        baselineTemp: Double? = null,
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

    fun analyzeCsvAsInt(
        csvData: String?,
        baselineTemp: Double? = null,
        baselineHR: Int = DEFAULT_BASELINE_HR
    ): Int {
        if (csvData.isNullOrBlank()) return 0

        return try {
            val values = csvData.split(",")
            if (values.size != 9) return 0

            val data = SensorData(
                id = "0001",
                temp = values[0].trim().toDouble(),
                tempValid = true,
                tempSource = "MEASURED_LEGACY",
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

    private fun analyzeMotion(accX: Double?, accY: Double?, accZ: Double?): MotionResult {
        if (accX == null || accY == null || accZ == null) {
            resetMotionState()
            return MotionResult(null, isStill = false, isHighActivity = false, motionAbnormal = false)
        }

        if (accX.isNaN() || accY.isNaN() || accZ.isNaN()) {
            resetMotionState()
            return MotionResult(null, isStill = false, isHighActivity = false, motionAbnormal = false)
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
            } else {
                val duration = currentTime - motionStartTime
                if (duration >= MOTION_STOP_THRESHOLD_MS) motionAbnormal = true
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

    fun resetMotionState() {
        isMotionStopped = false
        motionStartTime = 0L
    }

    private fun isValidSensorData(data: SensorData): Boolean {
        val spo2 = data.spo2
        val accX = data.accX
        val accY = data.accY
        val accZ = data.accZ

        if (data.temp.isNaN() || data.env.isNaN()) return false
        if (data.tempValid && data.temp !in -20.0..80.0) return false
        if (accX != null && accX.isNaN()) return false
        if (accY != null && accY.isNaN()) return false
        if (accZ != null && accZ.isNaN()) return false

        return data.hr in 31..220 &&
            data.env in -40.0..85.0 &&
            data.hum in 0..100 &&
            data.lux >= 0 &&
            (spo2 == null || spo2 in 51..100)
    }

    fun sanitizeBaselineTemp(value: Double?): Double? {
        return when {
            value == null -> null
            value.isNaN() -> null
            value !in -20.0..80.0 -> null
            else -> value
        }
    }

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
