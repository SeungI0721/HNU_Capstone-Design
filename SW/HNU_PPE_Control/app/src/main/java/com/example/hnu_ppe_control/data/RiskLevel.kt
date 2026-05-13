package com.example.hnu_ppe_control.data

// 앱 내부 위험 단계와 작업자 화면 표시 문구를 함께 관리합니다.
enum class RiskLevel(val label: String) {
    SAFE("정상"),
    CAUTION("주의"),
    DANGER("위험"),
    EMERGENCY("응급"),
    ERROR("오류")
}
