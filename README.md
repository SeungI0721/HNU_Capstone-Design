# Smart Shield

Smart Shield는 ESP32 기반 PPE 웨어러블 안전 보조 시스템입니다. ESP32 펌웨어가 I2C 센서 값을 읽어 BLE Notify payload로 작업자 Android 앱에 전달하고, 작업자 앱은 위험도를 계산해 UI, Firebase Realtime Database, ESP32 경고 출력에 반영합니다. 관리자 Android 앱은 Firebase 데이터를 읽어 작업자 상태와 위험 작업자를 모니터링합니다.

> 이 프로젝트는 산업안전 보조용 프로토타입입니다. TEMP, HR, SpO2 값은 의료 진단 목적이 아니라 작업 환경 위험 추세를 판단하기 위한 참고 값으로만 사용합니다.

## 전체 구조

```text
HW/SmartShield_ESP32
  -> I2C 센서 초기화 및 측정
  -> BLE Notify payload 송신
  -> BLE Write 명령 수신
  -> RED LED / 진동 모터 / 부저 출력 제어

SW/HNU_PPE_Control
  -> BLE 스캔 및 연결
  -> 센서 payload 파싱
  -> 위험도 계산
  -> 작업자 UI 표시
  -> Firebase 업로드
  -> ESP32 위험도 명령 전송

SW/HNU_PPE_Manager
  -> Firebase workers 데이터 조회
  -> 작업자 목록 및 상세 상태 표시
  -> 위험 작업자 우선 표시
  -> 작업 구역별 상태 확인
```

## 주요 폴더

| 경로 | 설명 |
|---|---|
| `HW/SmartShield_ESP32/` | ESP32 최종 통합 펌웨어 |
| `HW/TestCode/` | 센서, 출력 모듈, 통합 검증용 테스트 코드 |
| `SW/HNU_PPE_Control/` | 작업자 Android 앱 |
| `SW/HNU_PPE_Manager/` | 관리자 Android 앱 |
| `docs/` | BLE, Firebase, 위험도, 측정 검증 문서 |
| `tools/` | 드라이버 등 보조 파일 |
| https://github.com/rhkdgns3412-tech/2026-capston | 학술제 최종 HW 구성 |

## 현재 코드 기준 통신 규격

| 항목 | 값 |
|---|---|
| BLE Device Name | `SS_0001` |
| Worker ID | `0001` |
| Service UUID | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Sensor Notify UUID | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Control Write UUID | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| CCCD UUID | `00002902-0000-1000-8000-00805F9B34FB` |
| Notify 주기 | 1초 |
| BLE MTU 설정 | 128 |
| Firebase 경로 | `workers/{workerId}/currentStatus`, `workers/{workerId}/riskLogs/{logId}` |

## Notify payload

ESP32는 다음 형식의 문자열을 Notify characteristic으로 전송합니다.

```text
ID:0001,TEMP:34.8,TEMP_VALID:1,TEMP_SOURCE:MEASURED,HR:82,SPO2:98,ENV:28.4,HUM:55,LUX:1200,AX:0.01,AY:0.02,AZ:9.80,POSTURE:NORMAL
```

필수 필드는 `ID`, `TEMP`, `HR`, `SPO2`, `ENV`, `HUM`, `LUX`, `POSTURE`입니다. 현재 펌웨어는 `TEMP_VALID`, `TEMP_SOURCE`, `AX`, `AY`, `AZ`도 함께 보냅니다. `USE_PACKET_MARKERS`가 `true`이면 `<START>`와 `<END>`가 붙지만, 현재 기본값은 `false`입니다.

MAX30205 피부 온도 센서가 없거나 읽기 실패 시에는 정상 fallback 온도를 보내지 않고 다음처럼 유효하지 않은 값으로 표시합니다.

```text
TEMP:0.0,TEMP_VALID:0,TEMP_SOURCE:INVALID
```

다른 센서 값은 앱 파싱이 끊기지 않도록 기본 fallback 값을 사용할 수 있습니다.

## 센서와 출력 장치

| 구분 | 장치 | 코드 기준 |
|---|---|---|
| 환경 | BME280 | `0x76` 또는 `0x77`, 주변 온도와 습도 |
| 조도 | BH1750 | `0x23` 또는 `0x5C`, 조도 lux |
| 자세 | MPU6050 | `0x68` 또는 `0x69`, 가속도와 자세 상태 |
| 심박/산소포화도 보조 | MAX30102 | `0x57`, IR/RED 기반 간이 HR/SpO2 추정 |
| 피부 온도 | MAX30205 / Fever Click | `0x48`, `TEMP_VALID` 판단 기준 |
| 출력 | RED LED | GPIO27, 위험 단계 시각 경고 |
| 출력 | 진동 모터 | GPIO23, MOSFET 또는 트랜지스터 구동 권장 |
| 출력 | 부저 | GPIO18, PWM tone 출력 |

## 위험도 명령

작업자 앱은 계산된 위험도를 ESP32 Write characteristic으로 전송합니다.

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

ESP32 펌웨어는 `RISK:ERROR`도 수신할 수 있으며, 이 경우 `CAUTION` 출력으로 매핑합니다. 현재 Android 작업자 앱의 `RiskCommandMapper`는 `ERROR`를 `RISK:SAFE`로 보냅니다.

## 빌드 검증

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 HW\SmartShield_ESP32

cd SW\HNU_PPE_Control
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest

cd ..\HNU_PPE_Manager
.\gradlew.bat :app:assembleDebug
```

ESP32 업로드와 시리얼 모니터는 실제 보드가 USB로 연결되어 있고 COM 포트가 정상 인식된 뒤 실행합니다.

```powershell
arduino-cli upload -p COM8 --fqbn esp32:esp32:esp32 HW\SmartShield_ESP32
arduino-cli monitor -p COM8 -c baudrate=115200
```

## 실측 검증

빌드 성공은 실제 하드웨어 동작을 보장하지 않습니다. 발표 또는 최종 점검 전에는 `docs/MEASUREMENT_VERIFICATION.md` 기준으로 I2C 주소, 센서 값 범위, BLE 연결 안정성, Firebase 업로드, 관리자 앱 표시, RED LED/진동/부저 출력을 실제 장치에서 확인해야 합니다.
