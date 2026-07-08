// 앱 위험 단계를 ESP32 BLE Write 명령 문자열로 변환하는 파일
package com.example.hnu_ppe_control.risk

import com.example.hnu_ppe_control.data.RiskLevel

object RiskCommandMapper {

    // 앱의 위험 단계를 ESP32가 처리하는 BLE 제어 명령으로 변환합니다.
    fun toCommand(riskLevel: RiskLevel): String {
        return when (riskLevel) {
            RiskLevel.SAFE -> "RISK:SAFE"
            RiskLevel.CAUTION -> "RISK:CAUTION"
            RiskLevel.DANGER -> "RISK:DANGER"
            RiskLevel.EMERGENCY -> "RISK:EMERGENCY"
            RiskLevel.ERROR -> "RISK:SAFE"
        }
    }

    // 예전 정수형 위험도 결과를 앱 공통 위험 단계로 변환합니다.
    fun fromDangerLevel(level: Int): RiskLevel {
        return when (level) {
            0 -> RiskLevel.SAFE
            1 -> RiskLevel.CAUTION
            2 -> RiskLevel.DANGER
            3 -> RiskLevel.EMERGENCY
            else -> RiskLevel.ERROR
        }
    }

    // 예전 정수형 위험도 결과를 ESP32 BLE 제어 명령으로 바로 변환합니다.
    fun toCommandFromDangerLevel(level: Int): String {
        return toCommand(fromDangerLevel(level))
    }
}
