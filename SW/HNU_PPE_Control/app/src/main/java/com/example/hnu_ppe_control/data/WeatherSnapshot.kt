package com.example.hnu_ppe_control.data

// 기상청 데이터는 지역 단위 참고 정보이며 현장 센서값이나 의료 판단으로 사용하지 않습니다.
data class WeatherSnapshot(
    val alert: String = "연결 전",
    val todayMaxTemp: Double? = null,
    val region: String = "미설정"
)
