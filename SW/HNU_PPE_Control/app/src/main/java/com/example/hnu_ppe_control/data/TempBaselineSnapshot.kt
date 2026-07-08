// 작업 시작 후 기준 체온 수집 상태와 온도 변화값을 전달하는 데이터 파일
package com.example.hnu_ppe_control.data

data class TempBaselineSnapshot(
    val baselineTemp: Double? = null,
    val baselineTempReady: Boolean = false,
    val deltaTemp: Double? = null,
    val stableDeltaTemp: Double? = null,
    val status: String = "SENSOR_ERROR"
)
