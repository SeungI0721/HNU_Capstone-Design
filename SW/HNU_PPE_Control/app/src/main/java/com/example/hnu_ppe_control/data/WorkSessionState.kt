package com.example.hnu_ppe_control.data

// 작업 세션 흐름을 버튼 동작과 BLE 상태 표시에서 함께 사용합니다.
enum class WorkSessionState {
    IDLE,
    CONNECTING,
    WORKING,
    RECONNECTING,
    ENDED
}
