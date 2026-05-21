// 작업 위치 선택 항목과 Firebase 저장 코드를 정의하는 파일
package com.example.hnu_ppe_control.data

// Firebase 저장용 코드와 작업자 표시용 이름 분리
data class WorkLocation(
    val code: String,
    val name: String
) {
    companion object {
    val OPTIONS = listOf(
        WorkLocation("LOC_ROOF", "옥상 방수 작업 구역"),
        WorkLocation("LOC_WAREHOUSE", "실내 자재 창고"),
        WorkLocation("LOC_OUTDOOR_A", "외부 철근 조립 구역"),
        WorkLocation("LOC_BASEMENT", "지하 설비 점검 구역"),
        WorkLocation("LOC_SCAFFOLD", "외부 비계 작업 구역")
        )
    }
}
