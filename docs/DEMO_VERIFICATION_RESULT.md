# Smart Shield Demo Verification Result

이 문서는 저장소에 남아 있는 코드와 문서 기준으로 확인 가능한 검증 상태를 정리한 결과 요약입니다. 실제 시연 로그, 측정값, 스크린샷, Firebase 캡처가 저장소에 없는 항목은 `TBD` 또는 `NOT RECORDED`로 표시합니다.

본 문서는 의료 정확도, 산업 현장 배포 준비 완료, 상용 수준 신뢰성을 주장하지 않습니다. 센서값은 산업안전 보조 판단을 위한 프로토타입 데이터이며, 실제 적용 전 별도 검증이 필요합니다.

## 1. verified by build

아래 항목은 저장소의 빌드 가능한 코드 구조 또는 문서화된 구현 기준으로 확인 가능한 항목입니다. 이 상태는 실제 장치 시연 성공을 의미하지 않습니다.

| 항목 | 근거 | 결과 |
|---|---|---|
| ESP32 통합 펌웨어 소스 존재 | `HW/SmartShield_ESP32/` | VERIFIED BY BUILD STRUCTURE |
| 작업자 Android 앱 소스 존재 | `SW/HNU_PPE_Control/` | VERIFIED BY BUILD STRUCTURE |
| 관리자 Android 앱 소스 존재 | `SW/HNU_PPE_Manager/` | VERIFIED BY BUILD STRUCTURE |
| BLE UUID 및 Notify/Write 규격 문서화 | `docs/BLE_PROTOCOL.md` | VERIFIED BY DOCUMENTATION |
| Firebase 저장 경로 문서화 | `docs/FIREBASE_SCHEMA.md` | VERIFIED BY DOCUMENTATION |
| 위험도 알고리즘 제한사항 문서화 | `docs/RISK_ALGORITHM.md` | VERIFIED BY DOCUMENTATION |

## 2. verified by demo

아래 항목은 실제 장치 또는 앱 시연 결과가 있어야 검증 완료로 표시할 수 있습니다.

| 항목 | 필요 증거 | 결과 |
|---|---|---|
| ESP32 실제 부팅 및 Serial Monitor 로그 | 로그 또는 사진 | NOT RECORDED |
| I2C 센서 실제 주소 인식 | I2C Scanner 로그 | NOT RECORDED |
| 작업자 앱 BLE 연결 | 화면 캡처 또는 시연 기록 | NOT RECORDED |
| BLE Notify payload 수신 | Logcat, 화면 캡처, 시리얼 로그 | NOT RECORDED |
| BLE Write 위험도 명령 전송 | ESP32 수신 로그 | NOT RECORDED |
| RED LED, 진동모터, 부저 출력 | 시연 영상 또는 체크 기록 | NOT RECORDED |
| Firebase `currentStatus` 갱신 | Firebase 콘솔 캡처 | NOT RECORDED |
| Firebase `riskLogs` 생성 | Firebase 콘솔 캡처 | NOT RECORDED |
| 관리자 앱 작업자 목록 및 상세 화면 표시 | 화면 캡처 또는 시연 기록 | NOT RECORDED |

## 3. partially verified

아래 항목은 코드와 문서상 구현 의도는 확인되지만, 실제 환경 검증 또는 정량 기록이 부족한 항목입니다.

| 항목 | 현재 상태 | 남은 확인 |
|---|---|---|
| MAX30102 HR/SpO2 추정값 사용 | 보조 지표로 문서화됨 | 손목 착용 안정성 실측 필요 |
| MAX30205 피부 접촉 온도 처리 | `TEMP_VALID`, `TEMP_SOURCE` 기준 문서화됨 | 실제 센서 연결 및 기준값 대비 변화 확인 필요 |
| BLE 재연결 및 RSSI 표시 | 문서와 앱 구조상 고려됨 | 거리·착용 위치별 반복 측정 필요 |
| Firebase 보안 규칙 | 보안 검토 필요성이 문서화됨 | 실제 Security Rules 검증 필요 |
| 착용형 전원 구성 | USB 5V 시연 방향 문서화됨 | 배터리 보호회로, 전류, 사용 시간 실측 필요 |

## 4. not verified

아래 항목은 현재 저장소만으로 검증 완료라고 주장하지 않습니다.

| 항목 | 상태 |
|---|---|
| 의료기기 수준 체온, 심박, 산소포화도 정확도 | NOT VERIFIED |
| 온열질환 진단 성능 | NOT VERIFIED |
| 낙상 100% 감지 성능 | NOT VERIFIED |
| 산업 현장 장시간 운용 안정성 | NOT VERIFIED |
| 상용 제품 수준 방수, 내구성, 전원 안전성 | NOT VERIFIED |
| Firebase 인증·권한 정책의 배포 보안성 | NOT VERIFIED |

## 5. manual tasks

| 작업 | 상태 |
|---|---|
| `MEASUREMENT_VERIFICATION.md`에 실제 테스트 일시, 담당자, 장비 기록 | TBD |
| ESP32 빌드 로그 또는 커밋 기준 기록 | TBD |
| Android 작업자 앱 빌드 로그 기록 | TBD |
| Android 관리자 앱 빌드 로그 기록 | TBD |
| 최종 시연 영상, 사진, Firebase 캡처 정리 | TBD |
| BLE 위치별 RSSI 및 끊김 횟수 기록 | TBD |
