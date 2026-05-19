// 위험 단계를 ESP32로 보낼 RISK 제어 명령 문자열로 변환하는 파일
package com.example.hnu_ppe_control.risk

import com.example.hnu_ppe_control.data.RiskLevel

object RiskCommandMapper {

    // 앱 위험 단계를 ESP32 펌웨어가 처리하는 명령으로 변환
    fun toCommand(riskLevel: RiskLevel): String {
        return when (riskLevel) {
            RiskLevel.SAFE -> "RISK:SAFE"
            RiskLevel.CAUTION -> "RISK:CAUTION"
            RiskLevel.DANGER -> "RISK:DANGER"
            RiskLevel.EMERGENCY -> "RISK:EMERGENCY"
            RiskLevel.ERROR -> "RISK:SAFE"
        }
    }

    // 알고리즘 담당 코드의 정수 위험도 단계를 앱 RiskLevel로 변환
    fun fromDangerLevel(level: Int): RiskLevel {
        return when (level) {
            0 -> RiskLevel.SAFE
            1 -> RiskLevel.CAUTION
            2 -> RiskLevel.DANGER
            3 -> RiskLevel.EMERGENCY
            else -> RiskLevel.ERROR
        }
    }

    // 정수 위험도 단계를 ESP32 제어 명령으로 바로 변환
    fun toCommandFromDangerLevel(level: Int): String {
        return toCommand(fromDangerLevel(level))
    }
}
