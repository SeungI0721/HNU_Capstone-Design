// ESP32에서 받은 문자열 payload를 SensorData로 변환하고 유효성을 검사하는 파일
package com.example.hnu_ppe_control.parser

import com.example.hnu_ppe_control.data.SensorData

object SensorDataParser {

    private val allowedPostures = setOf(
        "NORMAL",
        "WARNING",
        "UNSTABLE",
        "FALL",
        "EMERGENCY"
    )

    fun parse(rawData: String): SensorData? {
        // 빈 문자열, 필드 누락, 타입 오류, 범위 오류 방어
        val cleanedData = rawData
            .trim()
            .removePrefix("<START>")
            .removeSuffix("<END>")
            .trim()

        if (cleanedData.isEmpty()) return null
        if (!hasRequiredKeys(cleanedData)) return null

        val dataMap = cleanedData
            .split(",")
            .mapNotNull { part ->
                val keyValue = part.split(":", limit = 2)
                if (keyValue.size != 2) return@mapNotNull null

                val key = keyValue[0].trim()
                val value = keyValue[1].trim()
                if (key.isEmpty() || value.isEmpty()) null else key to value
            }
            .toMap()

        val id = dataMap["ID"] ?: return null
        val temp = dataMap["TEMP"]?.toDoubleOrNull() ?: return null
        val hr = dataMap["HR"]?.toIntOrNull() ?: return null
        val spo2 = dataMap["SPO2"]?.toIntOrNull()
        val env = dataMap["ENV"]?.toDoubleOrNull() ?: return null
        val hum = dataMap["HUM"]?.toIntOrNull() ?: return null
        val lux = dataMap["LUX"]?.toIntOrNull() ?: return null
        val ax = dataMap["AX"]?.toDoubleOrNull()
        val ay = dataMap["AY"]?.toDoubleOrNull()
        val az = dataMap["AZ"]?.toDoubleOrNull()
        val posture = dataMap["POSTURE"]?.uppercase() ?: return null

        if (!isValidWorkerId(id)) return null
        if (!isValidSensorRange(temp, hr, spo2, env, hum, lux)) return null
        if (!isValidAxisRange(ax, ay, az)) return null
        if (!allowedPostures.contains(posture)) return null

        return SensorData(
            id = id,
            temp = temp,
            hr = hr,
            spo2 = spo2,
            env = env,
            hum = hum,
            lux = lux,
            ax = ax,
            ay = ay,
            az = az,
            posture = posture
        )
    }

    private fun hasRequiredKeys(cleanedData: String): Boolean {
        // 필수 키 확인, SPO2는 선택값
        return cleanedData.contains("ID:") &&
            cleanedData.contains("TEMP:") &&
            cleanedData.contains("HR:") &&
            cleanedData.contains("ENV:") &&
            cleanedData.contains("HUM:") &&
            cleanedData.contains("LUX:") &&
            cleanedData.contains("POSTURE:")
    }

    private fun isValidWorkerId(id: String): Boolean {
        // BLE 이름, payload, Firebase 경로 공통 ID 형식
        return id.length == 4 && id.all { it.isDigit() }
    }

    private fun isValidSensorRange(
        temp: Double,
        hr: Int,
        spo2: Int?,
        env: Double,
        hum: Int,
        lux: Int
    ): Boolean {
        // 위험도 계산 전 센서값 범위 검증
        if (temp.isNaN() || env.isNaN()) return false
        if (temp < 30.0 || temp > 43.0) return false
        if (hr < 30 || hr > 220) return false
        if (spo2 != null && (spo2 < 50 || spo2 > 100)) return false
        if (env < -20.0 || env > 60.0) return false
        if (hum < 0 || hum > 100) return false
        if (lux < 0 || lux > 200000) return false

        return true
    }

    private fun isValidAxisRange(ax: Double?, ay: Double?, az: Double?): Boolean {
        return listOf(ax, ay, az).all { value ->
            value == null || (!value.isNaN() && value >= -80.0 && value <= 80.0)
        }
    }
}
