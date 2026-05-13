package com.example.hnu_ppe_control.data

// 메인 화면에는 RSSI 숫자 대신 작업자가 이해하기 쉬운 신호 등급만 표시합니다.
enum class BleSignalLevel(val label: String) {
    GOOD("좋음"),
    NORMAL("보통"),
    WEAK("약함"),
    DISCONNECTED("끊김"),
    NOT_CONNECTED("연결 전");

    companion object {
        fun fromRssi(rssi: Int?): BleSignalLevel {
            return when {
                rssi == null -> DISCONNECTED
                rssi >= -60 -> GOOD
                rssi >= -75 -> NORMAL
                else -> WEAK
            }
        }
    }
}
