# HNU_PPE_Control

Smart Shield 작업자 Android 앱입니다. ESP32 웨어러블 장치와 BLE로 연결해 센서 데이터를 수신하고, 위험도를 계산한 뒤 UI, Firebase, ESP32 경고 출력에 반영합니다.

## 주요 기능

- Android 버전별 BLE 권한 요청
- ESP32 BLE 장치 스캔 및 연결
- GATT 연결, MTU 요청, Notify 구독
- Notify payload 파싱
- `TEMP_VALID`, `TEMP_SOURCE` 기반 피부 온도 유효성 처리
- 작업 시작 시 기준 온도 수집 및 이후 온도 변화 추적
- 온열 위험도와 자세 위험도 계산
- 위험도에 따른 작업자 UI 표시
- 위험 단계별 앱 알림 및 진동 처리
- ESP32로 `RISK:SAFE`, `RISK:CAUTION`, `RISK:DANGER`, `RISK:EMERGENCY` 명령 전송
- Firebase `workers/{workerId}/currentStatus` 업로드
- 위험/응급 상황 시 `workers/{workerId}/riskLogs/{logId}` 기록
- Foreground Service 기반 작업 세션 유지

## 주요 파일

| 파일 | 역할 |
|---|---|
| `MainActivity.kt` | 작업 시작, BLE 수신, 위험도 계산, UI/Firebase 연동 |
| `WorkerDetailActivity.kt` | 작업자 상세 상태 화면 |
| `ble/BleConstants.kt` | ESP32와 공유하는 BLE UUID |
| `ble/BleManager.kt` | BLE 스캔, 연결, Notify, Write, 재연결 |
| `parser/SensorDataParser.kt` | ESP32 payload 파싱과 값 검증 |
| `risk/HeatstrokeAnalyzer.kt` | 온열 및 자세 기반 위험도 계산 |
| `risk/RiskCommandMapper.kt` | 위험도를 ESP32 Write 명령으로 변환 |
| `firebase/FirebaseStatusUploader.kt` | Firebase 현재 상태와 로그 업로드 |
| `firebase/RiskLogPolicy.kt` | 위험 로그 기록 정책 |
| `ui/MainUiController.kt` | 메인 화면 표시 갱신 |
| `service/SmartShieldForegroundService.kt` | 작업 중 Foreground Service |

## ESP32 payload 입력

앱 파서는 다음 필드를 사용합니다.

```text
ID:0001,TEMP:34.8,TEMP_VALID:1,TEMP_SOURCE:MEASURED,HR:82,SPO2:98,ENV:28.4,HUM:55,LUX:1200,AX:0.01,AY:0.02,AZ:9.80,POSTURE:NORMAL
```

필수 필드는 `ID`, `TEMP`, `HR`, `SPO2`, `ENV`, `HUM`, `LUX`, `POSTURE`입니다. `TEMP_VALID`가 없던 과거 payload도 일부 호환하지만, 현재 펌웨어는 `TEMP_VALID`와 `TEMP_SOURCE`를 보냅니다.

## 위험도 명령

앱은 위험도를 ESP32 명령으로 변환해 Write characteristic에 씁니다.

| 앱 위험도 | ESP32 명령 |
|---|---|
| `SAFE` | `RISK:SAFE` |
| `CAUTION` | `RISK:CAUTION` |
| `DANGER` | `RISK:DANGER` |
| `EMERGENCY` | `RISK:EMERGENCY` |
| `ERROR` | `RISK:SAFE` |

## 빌드

```powershell
cd SW\HNU_PPE_Control
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

현재 단위 테스트 소스가 없으면 `:app:testDebugUnitTest`는 `NO-SOURCE`로 성공할 수 있습니다.

## 주의 사항

- TEMP는 의료용 체온 진단값이 아니라 작업 시작 기준 대비 피부 접촉 온도 변화 추적값입니다.
- MAX30102 HR/SpO2 값은 착용 상태와 움직임에 크게 영향을 받습니다.
- Firebase 보안 규칙과 인증 동작은 실제 배포 환경에서 별도 검증이 필요합니다.
- 앱이 ESP32에 연결되어 있지 않으면 위험도 계산 결과가 하드웨어 출력으로 전달되지 않습니다.
