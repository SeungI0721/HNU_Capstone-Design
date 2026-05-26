# Smart Shield

Smart Shield는 ESP32 기반 PPE 웨어러블 산업안전 보조 시스템입니다. ESP32가 I2C 센서 데이터를 수집해 BLE Notify로 작업자 앱에 전달하고, 작업자 앱은 위험 가능성을 계산한 뒤 Firebase Realtime Database에 상태를 업로드합니다. 관리자 앱은 Firebase 데이터를 읽어 작업자 상태와 위험 작업자를 확인합니다.

> 이 프로젝트는 의료기기가 아닙니다. TEMP, HR, SpO2 값은 진단 목적이 아니라 산업안전 보조 판단을 위한 추세 지표로만 사용합니다.

## 전체 구조

```text
HW/SmartShield_ESP32
  -> 센서 수집
  -> BLE Notify
  -> BLE Write 명령 수신
  -> RED LED / 진동모터 / 부저 출력

SW/HNU_PPE_Control
  -> BLE 스캔/연결
  -> 센서 payload 파싱
  -> 위험도 계산
  -> UI 표시
  -> Firebase 업로드
  -> ESP32 위험 명령 전송

SW/HNU_PPE_Manager
  -> Firebase 읽기
  -> 위험 작업자 표시
  -> 구역별 작업자 상태 확인
```

## 주요 폴더

| 경로 | 설명 |
|---|---|
| `HW/SmartShield_ESP32/` | ESP32 최종 통합 펌웨어 |
| `HW/TestCode/` | 센서/모듈/통합 검증용 테스트 코드 |
| `SW/HNU_PPE_Control/` | 작업자 Android 앱 |
| `SW/HNU_PPE_Manager/` | 관리자 Android 앱 |
| `docs/` | BLE, Firebase, 위험도, 실측 검증 문서 |

## 현재 코드 기준 핵심 규격

| 항목 | 값 |
|---|---|
| BLE Device Name | `SS_0001` |
| workerId | `0001` |
| Service UUID | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Sensor Notify UUID | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Control Write UUID | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| CCCD UUID | `00002902-0000-1000-8000-00805F9B34FB` |
| Firebase 경로 | `workers/{workerId}/currentStatus`, `workers/{workerId}/riskLogs/{logId}` |

## 센서와 출력 장치

| 구분 | 장치 | 용도 |
|---|---|---|
| 환경 | BME280 | 주변 온도, 습도 |
| 조도 | BH1750 | 직사광선 노출 가능성 보조 판단 |
| 자세 | MPU6050 | 자세, 움직임, 낙상 가능성 추정 |
| 생체 보조 | MAX30102 | 심박수, SpO2 추정 |
| 피부 접촉 온도 | MAX30205 / Fever Click | 작업 시작 기준 대비 피부 접촉 온도 변화 추적 |
| 출력 | RED LED | 위험 단계 시각 경고 |
| 출력 | 진동모터 | 촉각 경고 |
| 출력 | 부저 | 청각 경고 |

## 빌드 확인 명령

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 "d:\HNU\HW\SmartShield_ESP32"

cd d:\HNU\SW\HNU_PPE_Control
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug

cd d:\HNU\SW\HNU_PPE_Manager
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

## 반드시 실측해야 하는 항목

코드 빌드 성공은 실제 장치 동작 보장을 의미하지 않습니다. 발표 전에는 `docs/MEASUREMENT_VERIFICATION.md`의 체크리스트를 기준으로 I2C 주소, BLE 연결 안정성, Firebase 업로드, 관리자 앱 표시, RED LED/진동/부저 출력을 실제 장치에서 확인해야 합니다.
