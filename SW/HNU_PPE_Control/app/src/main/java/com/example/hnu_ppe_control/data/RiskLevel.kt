package com.example.hnu_ppe_control.data

// 앱 화면, Firebase 저장값, ESP32 제어 명령 매핑에서 공통으로 사용하는 위험 단계입니다.
enum class RiskLevel(val label: String) {
    SAFE("정상"),
    CAUTION("주의"),
    DANGER("위험"),
    EMERGENCY("응급"),
    ERROR("오류")
}
