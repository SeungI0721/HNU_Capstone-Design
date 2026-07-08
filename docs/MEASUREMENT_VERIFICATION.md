# Smart Shield 실측 검증 체크리스트 템플릿

> 이 문서는 최종 발표 또는 제출 전 실제 장치로 확인해야 할 항목을 기록하기 위한 템플릿입니다.
> 저장소에 실측값이 없는 항목은 `TBD` 또는 `NOT RECORDED`로 남기며, 임의의 수치나 성공 결과를 작성하지 않습니다.
> 빌드 성공은 하드웨어, BLE, Firebase, 관리자 앱 표시가 실제로 동작했다는 의미가 아닙니다.

이 문서는 발표 또는 최종 제출 전 실제 장치로 측정해야 하는 항목을 정리한 검증 기록지입니다. 코드 빌드 성공만으로는 센서값, BLE 품질, Firebase 업로드, 관리자 앱 표시가 실제로 정상이라고 판단할 수 없습니다.

## 1. 테스트 환경 기록

| 항목 | 기록 |
|---|---|
| 테스트 일시 | TBD |
| 테스트 장소 | TBD |
| 테스트 담당자 | TBD |
| ESP32 보드 | TBD |
| 작업자 앱 기기/Android 버전 | TBD |
| 관리자 앱 기기/Android 버전 | TBD |
| ESP32 전원 방식 | USB / 배터리 / 기타 |
| 착용 위치 | 팔뚝 / 손목 / 기타 |
| Firebase 프로젝트 | TBD |
| 펌웨어 빌드 일시 | TBD |
| 작업자 앱 빌드 일시 | TBD |
| 관리자 앱 빌드 일시 | TBD |

## 2. 빌드 검증

| 번호 | 항목 | 수행 방법 | 성공 기준 | 결과 |
|---:|---|---|---|---|
| B1 | ESP32 컴파일 | `arduino-cli compile --fqbn esp32:esp32:esp32 "d:\HNU\HW\SmartShield_ESP32"` | 컴파일 성공 | PASS / FAIL |
| B2 | 작업자 앱 빌드 | `SW/HNU_PPE_Control`에서 `.\gradlew.bat :app:assembleDebug` | APK 빌드 성공 | PASS / FAIL |
| B3 | 관리자 앱 빌드 | `SW/HNU_PPE_Manager`에서 `.\gradlew.bat :app:assembleDebug` | APK 빌드 성공 | PASS / FAIL |
| B4 | ESP32 업로드 | 실제 COM 포트로 업로드 | 업로드 성공 및 재부팅 | PASS / FAIL |

## 3. I2C 센서 주소 검증

I2C Scanner 또는 ESP32 Serial Monitor 로그로 실제 주소를 확인합니다.

| 번호 | 센서 | 코드 기준 주소 | 실측 주소 | 성공 기준 | 결과 |
|---:|---|---|---|---|---|
| I1 | BME280 | `0x76` 또는 `0x77` | | 코드 감지 주소와 일치 | PASS / FAIL |
| I2 | BH1750 | `0x23` 또는 `0x5C` | | 코드 감지 주소와 일치 | PASS / FAIL |
| I3 | MPU6050 | `0x68` 또는 `0x69` | | 코드 감지 주소와 일치 | PASS / FAIL |
| I4 | MAX30102 | `0x57` | | 코드 감지 주소와 일치 | PASS / FAIL |
| I5 | MAX30205 / Fever Click | `0x48` | | 코드 감지 주소와 일치 | PASS / FAIL |

## 4. ESP32 Serial Monitor 로그 검증

| 번호 | 항목 | 확인할 로그 예시 | 성공 기준 | 결과 |
|---:|---|---|---|---|
| S1 | 부팅 로그 | `Smart Shield` 시작 로그 | 재부팅 후 멈춤 없음 | PASS / FAIL |
| S2 | MAX30205 감지 | `[MAX30205] detected at 0x48` | 감지 로그 출력 | PASS / FAIL |
| S3 | TEMP 정상 | `TEMP=34.xx C, VALID=1` | 실측값과 `VALID=1` 출력 | PASS / FAIL |
| S4 | TEMP 실패 | `read failed, VALID=0` | 센서 분리 시 `VALID=0` 출력 | PASS / FAIL |
| S5 | Notify payload | `[NOTIFY] ID:0001,...` | payload가 1초 주기로 출력 | PASS / FAIL |
| S6 | RISK 명령 수신 | `RISK:CAUTION` 등 | 앱 명령 수신 로그 출력 | PASS / FAIL |

## 5. BLE 연결 및 Notify 검증

| 번호 | 항목 | 수행 방법 | 성공 기준 | 결과 |
|---:|---|---|---|---|
| BL1 | 장치 검색 | 작업자 앱에서 BLE 검색 | `SS_0001` 표시 | PASS / FAIL |
| BL2 | GATT 연결 | `SS_0001` 선택 | 연결 성공 UI 표시 | PASS / FAIL |
| BL3 | Notify 구독 | 연결 후 대기 | 센서값이 계속 갱신 | PASS / FAIL |
| BL4 | Payload 파싱 | Logcat 확인 | 파싱 오류 없이 UI 반영 | PASS / FAIL |
| BL5 | Write 명령 | 위험도 변경 유도 | ESP32가 `RISK:*` 수신 | PASS / FAIL |
| BL6 | 연결 끊김 | ESP32 전원 차단 또는 거리 이탈 | 앱에 끊김 표시 | PASS / FAIL |
| BL7 | 재연결 | ESP32 재가동 | Notify 재수신 | PASS / FAIL |

## 6. 센서값 실측 검증

| 번호 | 조건 | 확인 항목 | 성공 기준 | 결과 |
|---:|---|---|---|---|
| D1 | 정상 착용 | TEMP | `TEMP_VALID=1`, 화면에 온도 표시 | PASS / FAIL |
| D2 | MAX30205 분리 | TEMP | `TEMP_VALID=0`, 앱에 측정 불가 표시 | PASS / FAIL |
| D3 | 손가락/착용 안정 | HR/SpO2 | 값이 급격히 튀지 않음 | PASS / FAIL |
| D4 | MAX30102 미착용 | HR/SpO2 | fallback 여부와 한계 기록 | PASS / FAIL |
| D5 | 밝은 조명 | LUX | 조도 증가 반영 | PASS / FAIL |
| D6 | 자세 변화 | POSTURE/AX/AY/AZ | 자세 변화 반영 | PASS / FAIL |
| D7 | 고온 환경 모의 | ENV/HUM | 값 변화 반영 | PASS / FAIL |

## 7. TEMP baseline 검증

| 번호 | 조건 | 성공 기준 | 결과 |
|---:|---|---|---|
| T1 | 작업 시작 직후 30초 | `tempBaselineStatus=STABILIZING` | PASS / FAIL |
| T2 | 안정화 후 60초 수집 | `COLLECTING`, 샘플 수 증가 | PASS / FAIL |
| T3 | 유효 샘플 20개 이상 | `baselineTempReady=true` | PASS / FAIL |
| T4 | TEMP 순간 튐 1회 | 즉시 위험/응급으로 상승하지 않음 | PASS / FAIL |
| T5 | TEMP 지속 상승 | `stableDeltaTemp` 기준 위험도 상승 | PASS / FAIL |
| T6 | TEMP 센서 실패 | TEMP 위험도 계산 제외 | PASS / FAIL |

## 8. 위험도 및 출력 검증

| 번호 | 조건 | 앱 기대 결과 | ESP32 기대 결과 | 결과 |
|---:|---|---|---|---|
| R1 | 정상 센서값 | 정상 | RED LED OFF, 진동/부저 OFF | PASS / FAIL |
| R2 | 주의 조건 | 주의 | RED LED 점멸, 짧은 경고 | PASS / FAIL |
| R3 | 위험 조건 | 위험 | RED LED ON, 반복 진동/부저 | PASS / FAIL |
| R4 | `POSTURE:FALL` | 응급 | 강한 진동/부저 | PASS / FAIL |
| R5 | `POSTURE:EMERGENCY` | 응급 | 강한 진동/부저 | PASS / FAIL |
| R6 | 알 수 없는 명령 | 앱/ESP32 멈춤 없음 | 안전하게 무시 | PASS / FAIL |

## 9. Firebase 업로드 검증

| 번호 | 항목 | Firebase 경로 | 성공 기준 | 결과 |
|---:|---|---|---|---|
| F1 | 현재 상태 | `workers/0001/currentStatus` | 센서값과 위험도 저장 | PASS / FAIL |
| F2 | TEMP 근거 | `currentStatus` | `tempValid`, `tempSource`, `baselineTempReady`, `stableDeltaTemp` 저장 | PASS / FAIL |
| F3 | 위험 로그 | `workers/0001/riskLogs` | 위험/응급 시 로그 생성 | PASS / FAIL |
| F4 | 연결 끊김 | `currentStatus` | `bleConnected=false` 또는 끊김 상태 반영 | PASS / FAIL |
| F5 | 작업 종료 | `currentStatus` | `appSessionActive=false` 반영 | PASS / FAIL |

## 10. 관리자 앱 검증

| 번호 | 항목 | 성공 기준 | 결과 |
|---:|---|---|---|
| M1 | 모니터링 시작 | 시작 이후 새 작업자 데이터 표시 | PASS / FAIL |
| M2 | 위험 작업자 | 위험/응급 작업자가 상단 영역에 표시 | PASS / FAIL |
| M3 | 구역 필터 | 구역별 작업자 필터 동작 | PASS / FAIL |
| M4 | 상세 화면 | 센서값, 위험도, 업데이트 시간 표시 | PASS / FAIL |
| M5 | 최초 응급 로그 | 위험 작업자 카드에 최초 응급 발생 시간 표시 | PASS / FAIL |

## 11. BLE 위치별 품질 측정

각 조건에서 최소 3분 이상 측정합니다. 패킷 순번이 없으면 누락률은 정밀 계산하지 말고, 마지막 수신 시각과 연결 유지 여부 중심으로 기록합니다.

| 조건 | 거리 | RSSI 평균 | RSSI 최솟값 | 연결 유지 | 끊김 횟수 | 재연결 시간 | 비고 |
|---|---:|---:|---:|---|---:|---:|---|
| 손에 든 상태 | | | | | | | |
| 같은 쪽 앞주머니 | | | | | | | |
| 반대쪽 앞주머니 | | | | | | | |
| 뒷주머니 | | | | | | | |
| 안전조끼 앞주머니 | | | | | | | |
| 가방 안 | | | | | | | |
| 책상 위 | | | | | | | |
| 몸통 차폐 | | | | | | | |
| 걷는 상태 | | | | | | | |
| 팔 움직임 | | | | | | | |

## 12. 발표 전 최종 판정

| 항목 | 판정 | 비고 |
|---|---|---|
| ESP32 실측 동작 | PASS / FAIL | |
| 작업자 앱 실측 동작 | PASS / FAIL | |
| 관리자 앱 실측 동작 | PASS / FAIL | |
| Firebase 업로드 | PASS / FAIL | |
| TEMP 유효성 처리 | PASS / FAIL | |
| RED LED/진동/부저 출력 | PASS / FAIL | |
| BLE 재연결 | PASS / FAIL | |
| 발표 가능 여부 | 가능 / 조건부 / 불가 | |
