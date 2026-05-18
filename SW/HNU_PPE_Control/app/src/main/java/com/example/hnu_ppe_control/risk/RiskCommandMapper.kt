// 위험 단계를 ESP32 제어 명령 문자열로 변환하는 파일
package com.example.hnu_ppe_control.risk

import com.example.hnu_ppe_control.data.RiskLevel

object RiskCommandMapper {

    // 앱 위험 단계를 ESP32가 받는 RISK 명령으로 변환
    fun toCommand(riskLevel: RiskLevel): String {
        return when (riskLevel) {
            RiskLevel.SAFE -> "RISK:SAFE"
            RiskLevel.CAUTION -> "RISK:CAUTION"
            RiskLevel.DANGER -> "RISK:DANGER"
            RiskLevel.EMERGENCY -> "RISK:EMERGENCY"
            RiskLevel.ERROR -> "RISK:SAFE"
        }
    }

    // 정수 위험도 단계 변환
    fun fromDangerLevel(level: Int): RiskLevel {
        return when (level) {
            0 -> RiskLevel.SAFE
            1 -> RiskLevel.CAUTION
            2 -> RiskLevel.DANGER
            3 -> RiskLevel.EMERGENCY
            else -> RiskLevel.ERROR
        }
    }

    // 정수 위험도 단계에서 바로 ESP32 명령 생성
    fun toCommandFromDangerLevel(level: Int): String {
        return toCommand(fromDangerLevel(level))
    }
}
