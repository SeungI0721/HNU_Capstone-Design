// Firebase 작업자 상태 스냅샷을 관리자 화면 표시용 모델로 변환하는 파일
package com.example.hnu_ppe_manager

import com.google.firebase.database.DataSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AdminWorkerStatus(
    val workerId: String = "-",
    val deviceId: String = "-",
    val workLocationCode: String = "-",
    val workLocationName: String = "-",
    val riskLevel: String = "UNKNOWN",
    val riskText: String = "",
    val temp: Double? = null,
    val hr: Int? = null,
    val spo2: Int? = null,
    val env: Double? = null,
    val hum: Int? = null,
    val lux: Int? = null,
    val posture: String = "-",
    val bleState: String = "-",
    val rssiText: String = "-",
    val firstEmergencyLogText: String = "-",
    val firstEmergencyLogMillis: Long = 0L,
    val lastUpdatedRaw: String = "-",
    val lastUpdatedMillis: Long = 0L
) {
    val riskPriority: Int
        get() = riskPriorityOf(riskLevel)

    val riskKorean: String
        get() = riskKoreanOf(riskLevel, riskText)

    val displayLocation: String
        get() = when {
            workLocationName.isNotBlank() && workLocationName != "-" -> workLocationName
            workLocationCode.isNotBlank() && workLocationCode != "-" -> workLocationCode
            else -> "-"
        }

    companion object {
        fun fromSnapshot(
            workerKey: String,
            snapshot: DataSnapshot,
            riskLogsSnapshot: DataSnapshot? = null,
            riskLogSinceMillis: Long = 0L
        ): AdminWorkerStatus {
            // 작업자 앱 버전 차이로 필드명이 다를 수 있어 현재 코드에서 쓰는 대체 필드를 함께 읽습니다.
            val workerId = snapshot.child("workerId").stringValue(workerKey).ifBlank { workerKey }
            val lastUpdatedValue = snapshot.child("lastUpdated").value ?: snapshot.child("updatedAt").value
            val lastUpdatedMillis = parseLastUpdatedMillis(lastUpdatedValue)
            val firstEmergencyLog = firstEmergencyLogSummary(riskLogsSnapshot, riskLogSinceMillis)
            val bleStateValue = snapshot.child("bleState").stringValue(
                when (snapshot.child("bleConnected").value) {
                    true -> "연결됨"
                    false -> "연결 안됨"
                    else -> "-"
                }
            )
            val rssiValue = snapshot.child("rssiText").stringValue(
                snapshot.child("bleSignalLevel").stringValue(
                    snapshot.child("bleRssi").stringValue("-")
                )
            )

            return AdminWorkerStatus(
                workerId = workerId,
                deviceId = snapshot.child("deviceId").stringValue(
                    snapshot.child("deviceName").stringValue("-")
                ),
                workLocationCode = snapshot.child("workLocationCode").stringValue("-"),
                workLocationName = snapshot.child("workLocationName").stringValue("-"),
                riskLevel = snapshot.child("riskLevel").stringValue("UNKNOWN").uppercase(Locale.US),
                riskText = snapshot.child("riskText").stringValue(""),
                temp = snapshot.child("temp").doubleValue(),
                hr = snapshot.child("hr").intValue(),
                spo2 = snapshot.child("spo2").intValue(),
                env = snapshot.child("env").doubleValue(),
                hum = snapshot.child("hum").intValue(),
                lux = snapshot.child("lux").intValue(),
                posture = snapshot.child("posture").stringValue("-"),
                bleState = bleStateValue,
                rssiText = rssiValue,
                firstEmergencyLogText = firstEmergencyLog?.displayText ?: "-",
                firstEmergencyLogMillis = firstEmergencyLog?.createdAtMillis ?: 0L,
                lastUpdatedRaw = formatLastUpdated(lastUpdatedValue, lastUpdatedMillis),
                lastUpdatedMillis = lastUpdatedMillis
            )
        }

        fun riskPriorityOf(level: String): Int {
            return when (level.uppercase(Locale.US)) {
                "EMERGENCY", "응급" -> 4
                "DANGER", "위험" -> 3
                "CAUTION", "주의" -> 2
                "SAFE", "정상" -> 1
                else -> 0
            }
        }

        fun riskKoreanOf(level: String, fallback: String = ""): String {
            return when (level.uppercase(Locale.US)) {
                "EMERGENCY", "응급" -> "응급"
                "DANGER", "위험" -> "위험"
                "CAUTION", "주의" -> "주의"
                "SAFE", "정상" -> "정상"
                else -> fallback.ifBlank { "알 수 없음" }
            }
        }

        fun compareByRiskAndTime(): Comparator<AdminWorkerStatus> {
            return compareByDescending<AdminWorkerStatus> { it.riskPriority }
                .thenByDescending { it.lastUpdatedMillis }
        }

        private fun parseLastUpdatedMillis(value: Any?): Long {
            return when (value) {
                is Long -> value
                is Int -> value.toLong()
                is Double -> value.toLong()
                is Float -> value.toLong()
                is String -> parseLastUpdatedText(value)
                else -> 0L
            }
        }

        private fun parseLastUpdatedText(text: String): Long {
            text.toLongOrNull()?.let { return it }

            val patterns = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss"
            )
            for (pattern in patterns) {
                runCatching {
                    koreaDateFormat(pattern).parse(text)?.time
                }.getOrNull()?.let { return it }
            }
            return 0L
        }

        private fun formatLastUpdated(value: Any?, millis: Long): String {
            if (value is String && value.isNotBlank() && value.toLongOrNull() == null) return value
            if (millis <= 0L) return "-"
            return koreaDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(millis))
        }

        private fun firstEmergencyLogSummary(
            snapshot: DataSnapshot?,
            sinceMillis: Long
        ): EmergencyLogSummary? {
            // 모니터링 시작 이후의 최초 EMERGENCY 로그만 요약해 위험 작업자 목록에 표시합니다.
            return snapshot
                ?.children
                ?.mapNotNull { logSnapshot ->
                    val level = logSnapshot.child("riskLevel").stringValue("").uppercase(Locale.US)
                    if (level != "EMERGENCY") return@mapNotNull null

                    val createdAtValue = logSnapshot.child("createdAt").value
                        ?: logSnapshot.child("updatedAt").value
                    val createdAtMillis = parseLastUpdatedMillis(createdAtValue)
                    if (sinceMillis > 0L && createdAtMillis <= 0L) {
                        return@mapNotNull null
                    }
                    if (sinceMillis > 0L && createdAtMillis in 1L until sinceMillis) {
                        return@mapNotNull null
                    }
                    val message = logSnapshot.child("message").stringValue("")
                    val formattedTime = formatLastUpdated(createdAtValue, createdAtMillis)
                    EmergencyLogSummary(createdAtMillis, formattedTime, message)
                }
                ?.filter { it.createdAtMillis > 0L || it.message.isNotBlank() }
                ?.minWithOrNull(compareBy<EmergencyLogSummary> {
                    if (it.createdAtMillis > 0L) it.createdAtMillis else Long.MAX_VALUE
                }.thenBy { it.formattedTime })
        }

        private data class EmergencyLogSummary(
            val createdAtMillis: Long,
            val formattedTime: String,
            val message: String
        ) {
            val displayText: String
                get() = listOf(formattedTime, message)
                    .filter { it.isNotBlank() && it != "-" }
                    .joinToString(" / ")
                    .ifBlank { "-" }
        }

        private fun koreaDateFormat(pattern: String): SimpleDateFormat {
            return SimpleDateFormat(pattern, Locale.KOREA).apply {
                timeZone = TimeZone.getTimeZone("Asia/Seoul")
            }
        }
    }
}

private fun DataSnapshot.stringValue(defaultValue: String): String {
    return value?.toString()?.takeIf { it.isNotBlank() } ?: defaultValue
}

private fun DataSnapshot.doubleValue(): Double? {
    return when (val current = value) {
        is Double -> current
        is Float -> current.toDouble()
        is Long -> current.toDouble()
        is Int -> current.toDouble()
        is String -> current.toDoubleOrNull()
        else -> null
    }
}

private fun DataSnapshot.intValue(): Int? {
    return when (val current = value) {
        is Long -> current.toInt()
        is Int -> current
        is Double -> current.toInt()
        is Float -> current.toInt()
        is String -> current.toIntOrNull()
        else -> null
    }
}
