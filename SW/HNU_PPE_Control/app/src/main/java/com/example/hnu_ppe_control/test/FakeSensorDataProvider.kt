package com.example.hnu_ppe_control.test

object FakeSensorDataProvider {

    private val samples = listOf(
        "ID:0001,TEMP:36.5,HR:82,SPO2:98,ENV:28.5,HUM:55,LUX:8000,AX:0.12,AY:-0.08,AZ:9.78,POSTURE:NORMAL",
        "ID:0001,TEMP:37.6,HR:105,SPO2:96,ENV:31.5,HUM:72,LUX:32000,AX:0.42,AY:0.18,AZ:9.62,POSTURE:NORMAL",
        "ID:0001,TEMP:38.1,HR:125,SPO2:94,ENV:34.2,HUM:81,LUX:42000,AX:4.25,AY:1.90,AZ:8.72,POSTURE:WARNING",
        "ID:0001,TEMP:37.8,HR:130,SPO2:91,ENV:35.5,HUM:85,LUX:52000,AX:8.92,AY:5.40,AZ:1.25,POSTURE:FALL",
        "ID:0001,TEMP:38.5,HR:140,SPO2:89,ENV:36.0,HUM:88,LUX:65000,AX:7.85,AY:6.10,AZ:2.42,POSTURE:EMERGENCY"
    )

    private var index = 0

    fun nextPayload(): String {
        val payload = samples[index % samples.size]
        index += 1
        return payload
    }

    fun randomPayload(): String = samples.random()
}
