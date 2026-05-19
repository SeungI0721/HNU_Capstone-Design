// 작업 지역 날씨 참고 정보를 UI와 Firebase에 전달하는 데이터 파일
package com.example.hnu_ppe_control.data

// 지역 단위 참고 정보, 현장 센서값이나 의료 판단 아님
data class WeatherSnapshot(
    val alert: String = "연결 전",
    val todayMaxTemp: Double? = null,
    val region: String = "미설정"
)
