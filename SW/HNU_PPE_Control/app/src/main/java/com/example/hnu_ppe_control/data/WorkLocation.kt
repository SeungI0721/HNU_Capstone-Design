// 작업 위치 선택 항목과 Firebase 저장 코드를 정의하는 파일
package com.example.hnu_ppe_control.data

// 작업 위치는 Firebase 저장용 코드와 작업자 표시용 이름을 분리합니다.
data class WorkLocation(
    val code: String,
    val name: String
) {
    companion object {
        val OPTIONS = listOf(
            WorkLocation("A", "A동"),
            WorkLocation("B", "B동"),
            WorkLocation("PARKING", "주차장"),
            WorkLocation("FRONT", "정문"),
            WorkLocation("BACK", "후문")
        )
    }
}
