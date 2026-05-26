// 실제 BLE 장치 없이 앱 처리 흐름을 점검할 가짜 센서 payload 생성 파일
package com.example.hnu_ppe_control.test

object FakeSensorDataProvider {

    private val samples = listOf(
        "ID:0001,TEMP:36.5,TEMP_VALID:1,TEMP_SOURCE:MEASURED,HR:82,SPO2:98,ENV:28.5,HUM:55,LUX:8000,AX:0.12,AY:-0.08,AZ:9.78,POSTURE:NORMAL",
        "ID:0001,TEMP:37.6,TEMP_VALID:1,TEMP_SOURCE:MEASURED,HR:95,SPO2:96,ENV:31.5,HUM:55,LUX:8000,AX:0.42,AY:0.18,AZ:9.62,POSTURE:NORMAL",
        "ID:0001,TEMP:38.0,TEMP_VALID:1,TEMP_SOURCE:MEASURED,HR:118,SPO2:96,ENV:33.2,HUM:65,LUX:20000,AX:2.20,AY:1.10,AZ:9.12,POSTURE:NORMAL",
        "ID:0001,TEMP:38.5,TEMP_VALID:1,TEMP_SOURCE:MEASURED,HR:140,SPO2:89,ENV:36.0,HUM:88,LUX:65000,AX:7.85,AY:6.10,AZ:2.42,POSTURE:EMERGENCY"
    )

    private var index = 0

    fun nextPayload(): String {
        val payload = samples[index % samples.size]
        index += 1
        return payload
    }

    fun randomPayload(): String = samples.random()
}
