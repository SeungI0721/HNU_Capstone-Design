# HNU_PPE_Control

Smart Shield 작업자 Android 앱입니다. ESP32 웨어러블 장치와 BLE로 연결해 센서 데이터를 수신하고, 위험도를 계산한 뒤 UI, Firebase, ESP32 경고 출력에 반영합니다.

## 주요 기능

- Android 버전별 BLE 권한 요청
- `SS_0001` 형식 BLE 장치 검색
- GATT 연결, MTU 요청, Notify 구독
- 센서 payload 파싱
- TEMP 유효성(`TEMP_VALID`, `TEMP_SOURCE`) 처리
- 작업 시작 후 TEMP 안정화/기준값 수집/지속 상승 판단
- 위험도 계산 및 `RISK:*` 명령 전송
- 작업자 UI 표시, 팝업, 진동 경고
- Firebase `workers/{workerId}/currentStatus` 업로드
- 위험/응급 상황 시 `riskLogs` 기록
- Foreground Service 기반 작업 세션 유지

## 핵심 파일

| 파일 | 역할 |
|---|---|
| `MainActivity.kt` | 작업 시작, BLE 수신, 위험도 계산, UI/Firebase 연동 |
| `ble/BleManager.kt` | BLE 스캔, 연결, Notify, Write, 재연결 |
| `parser/SensorDataParser.kt` | ESP32 payload 파싱 |
| `risk/HeatstrokeAnalyzer.kt` | 위험도 계산 |
| `risk/RiskCommandMapper.kt` | 위험도와 ESP32 명령 매핑 |
| `firebase/FirebaseStatusUploader.kt` | Firebase 업로드 |
| `ui/MainUiController.kt` | 화면 표시 |
| `service/SmartShieldForegroundService.kt` | 작업 중 BLE 유지용 Foreground Service |

## 빌드

```powershell
cd d:\HNU\SW\HNU_PPE_Control
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

## 주의 사항

- TEMP는 절대 체온 진단값이 아니라 작업 시작 기준 대비 피부 접촉 온도 변화 추세로 사용합니다.
- MAX30102 HR/SpO2 값은 착용 상태와 움직임 영향을 크게 받으므로 실제 시연 전 안정성을 확인해야 합니다.
- Firebase 보안 규칙과 인증은 소스 코드만으로 검증되지 않으므로 별도 확인이 필요합니다.
