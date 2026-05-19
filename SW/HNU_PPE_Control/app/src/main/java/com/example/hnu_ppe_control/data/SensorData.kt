// ESP32 Notify payload에서 파싱한 작업자 센서 데이터를 담는 파일
package com.example.hnu_ppe_control.data

// Firebase 업로드, UI 표시, 위험도 계산에서 함께 사용하는 센서 데이터 구조
data class SensorData(
    val id: String,
    val temp: Double,
    val hr: Int,
    val spo2: Int?,
    val env: Double,
    val hum: Int,
    val lux: Int,
    val ax: Double?,
    val ay: Double?,
    val az: Double?,
    val posture: String
) {
    // 알고리즘 담당 코드의 accX 이름과 기존 앱의 ax 이름을 함께 지원
    val accX: Double?
        get() = ax

    // 알고리즘 담당 코드의 accY 이름과 기존 앱의 ay 이름을 함께 지원
    val accY: Double?
        get() = ay

    // 알고리즘 담당 코드의 accZ 이름과 기존 앱의 az 이름을 함께 지원
    val accZ: Double?
        get() = az

    // BH1750 조도값을 기준으로 직사광선 가능성 판단
    val directSunlight: Boolean
        get() = lux >= 50000
}
