package com.example.hnu_ppe_control.data

data class TempBaselineSnapshot(
    val baselineTemp: Double? = null,
    val baselineTempReady: Boolean = false,
    val deltaTemp: Double? = null,
    val stableDeltaTemp: Double? = null,
    val status: String = "SENSOR_ERROR"
)
