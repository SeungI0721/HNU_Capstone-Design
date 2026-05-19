// Smart Shield 앱에서 사용하는 위험 단계 enum을 정의하는 파일
package com.example.hnu_ppe_control.data

// 앱 내부 위험 단계와 화면 표시 문구 관리
enum class RiskLevel(val label: String) {
    SAFE("정상"),
    CAUTION("주의"),
    DANGER("위험"),
    EMERGENCY("응급"),
    ERROR("오류")
}
