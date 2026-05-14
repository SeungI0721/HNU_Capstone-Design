// ESP32 Notify payload에서 파싱한 작업자 센서 데이터를 담는 모델 파일
package com.example.hnu_ppe_control.data

// Firebase currentStatus와 위험도 계산에 공통으로 사용하는 센서 데이터 구조입니다.
data class SensorData(
    val id: String,
    val temp: Double,
    val hr: Int,
    // MAX30102 기반 산소포화도 추정값입니다. HW 방향에서 선택값이므로 없으면 null로 둡니다.
    val spo2: Int?,
    val env: Double,
    val hum: Int,
    // BH1750 기반 조도값입니다. 직사광선/고조도 위험 판단과 앱 화면 확인에 사용합니다.
    val lux: Int,
    val ax: Double?,
    val ay: Double?,
    val az: Double?,
    val posture: String
) {
    // BH1750 LUX 값을 기반으로 앱에서 직사광선 가능성을 추정합니다.
    val directSunlight: Boolean
        get() = lux >= 50000
}
