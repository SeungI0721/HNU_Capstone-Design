package com.example.hnu_ppe_control.data

data class WorkerDetailSnapshot(
    val workerId: String = "-",
    val workLocationCode: String = "-",
    val workLocationName: String = "-",
    val workStartedAt: String = "-",
    val bleState: String = "-",
    val bleSignalLevel: String = "-",
    val bleRssi: String = "-",
    val riskLevel: String = "-",
    val temp: String = "-",
    val hr: String = "-",
    val spo2: String = "-",
    val env: String = "-",
    val hum: String = "-",
    val lux: String = "-",
    val axis: String = "-",
    val posture: String = "-",
    val baselinePosture: String = "-",
    val weatherAlert: String = "-",
    val weatherRegion: String = "-",
    val todayMaxTemp: String = "-",
    val lastUpdatedAt: String = "-"
)
