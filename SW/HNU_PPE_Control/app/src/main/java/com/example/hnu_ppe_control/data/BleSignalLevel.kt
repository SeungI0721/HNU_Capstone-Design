// BLE RSSI 값을 앱 표시용 신호 등급으로 변환하는 파일
package com.example.hnu_ppe_control.data

// RSSI 숫자 대신 작업자용 신호 등급 표시
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
