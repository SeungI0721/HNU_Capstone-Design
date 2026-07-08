# Smart Shield

Smart Shield는 ESP32 기반 PPE 웨어러블 장치와 Android 앱을 이용해 작업자 센서 상태와 위험도를 모니터링하는 산업안전 보조 프로토타입입니다.

## Portfolio Summary

Smart Shield는 ESP32 웨어러블 장치, Android 작업자 앱, Android 관리자 앱, Firebase Realtime Database를 연결한 임베디드/IoT 캡스톤 프로젝트입니다.  
ESP32는 센서 payload를 BLE Notify로 전송하고, 작업자 앱은 이를 파싱해 위험도를 계산한 뒤 BLE Write와 Firebase 업로드를 수행합니다.  
관리자 앱은 Firebase 데이터를 기반으로 작업자 현재 상태와 위험 로그를 모니터링합니다.  
본 저장소는 의료기기나 상용 안전장비가 아니라, 센서 융합·BLE 통신·모바일 앱·클라우드 연동을 검증하기 위한 학부 캡스톤 프로토타입입니다.

## 프로젝트 목적

작업자의 피부 접촉 온도, 심박 추정값, 산소포화도 추정값, 주변 환경, 자세 정보를 수집하고 위험 추세를 앱과 Firebase에 반영합니다. 위험 단계가 높아지면 작업자 앱이 ESP32로 제어 명령을 보내 RED LED, 진동 모터, 부저를 동작시킵니다.

이 프로젝트의 센서 값은 의료 진단 목적이 아닙니다. 실제 현장 적용 전에는 센서 정확도, 착용 안정성, 통신 안정성, Firebase 보안 규칙을 별도로 검증해야 합니다.

## My Role

제가 주로 담당한 범위는 Android 앱과 BLE/Firebase 연동 흐름 중심입니다.

- Android 작업자 앱 구현
- Android 관리자 앱 구현
- BLE 통신 payload 파싱 및 위험도별 제어 흐름 구현
- Firebase Realtime Database 구조 설계 및 업로드 흐름 구현
- 문서화, 발표 자료 정리, 최종 시연 흐름 조율
- ESP32 센서·출력 장치 연동 과정의 일부 하드웨어 통합 지원

하드웨어 회로, 센서 실측 정확도, 착용형 전원 안정성, 의료적 유효성, 산업 현장 배포 수준의 신뢰성은 별도 검증이 필요한 영역으로 분리했습니다.

## 전체 시스템 구조

```text
Smart Shield/
├─ README.md
├─ HW/
│  ├─ README.md
│  ├─ SmartShield_ESP32/
│  └─ TestCode/
├─ SW/
│  ├─ README.md
│  ├─ HNU_PPE_Control/
│  └─ HNU_PPE_Manager/
├─ docs/
│  ├─ README.md
│  ├─ SYSTEM_ARCHITECTURE.md
│  ├─ BLE_PROTOCOL.md
│  ├─ FIREBASE_SCHEMA.md
│  ├─ RISK_ALGORITHM.md
│  ├─ MEASUREMENT_VERIFICATION.md
│  ├─ DEMO_VERIFICATION_RESULT.md
│  ├─ TEST_PLAN.md
│  ├─ FINAL_CHECKLIST.md
│  └─ OwnedParts.md
└─ tools/
   └─ drivers/
```

| 경로 | 설명 |
|---|---|
| `HW/SmartShield_ESP32/` | ESP32 최종 통합 펌웨어 |
| `HW/TestCode/` | 센서, 출력 모듈, 통합 검증용 Arduino 테스트 코드 |
| `SW/HNU_PPE_Control/` | 작업자 Android 앱 |
| `SW/HNU_PPE_Manager/` | 관리자 Android 앱 |
| `docs/` | 통신 규격, Firebase 구조, 위험도 기준, 검증 문서 |
| `tools/` | 드라이버 등 보조 파일 |

ESP32 최종 빌드 기준 파일은 `HW/SmartShield_ESP32/SmartShield_ESP32.ino`입니다.

## 핵심 기능

- ESP32 I2C 센서값 수집
- BLE Notify payload 전송
- 작업자 앱의 BLE 연결, payload 파싱, 위험도 계산
- 작업자 앱 UI 표시와 위험 단계별 앱 알림
- ESP32 RED LED, 진동 모터, 부저 제어
- Firebase Realtime Database 현재 상태 및 위험 로그 업로드
- 관리자 앱의 작업자 목록, 위험 작업자, 상세 상태 표시

## 사용 기술

| 구분 | 현재 코드 기준 |
|---|---|
| 보드 | ESP32 Dev Module |
| 펌웨어 | Arduino C/C++ |
| Android 앱 | Kotlin, Android Gradle Plugin |
| 통신 | BLE GATT Notify/Write |
| 데이터 저장 | Firebase Realtime Database |
| 주요 센서 | BME280, BH1750, MPU6050, MAX30102, MAX30205 |
| 출력 장치 | RED LED, 진동 모터, 부저 |

## 주요 규격

| 항목 | 값 |
|---|---|
| BLE Device Name | `SS_0001` |
| Worker ID | `0001` |
| Service UUID | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Sensor Notify UUID | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Control Write UUID | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| Notify 주기 | `1000 ms` |
| BLE MTU | `128` |
| Firebase 경로 | `workers/{workerId}/currentStatus`, `workers/{workerId}/riskLogs/{logId}` |

## 실행 및 빌드

ESP32 펌웨어 컴파일은 저장소 루트에서 실행합니다.

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 HW\SmartShield_ESP32
```

작업자 앱은 다음 위치에서 빌드합니다.

```powershell
cd SW\HNU_PPE_Control
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

관리자 앱은 다음 위치에서 빌드합니다.

```powershell
cd SW\HNU_PPE_Manager
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

ESP32 업로드와 시리얼 모니터는 실제 보드가 USB로 연결되어 있고 COM 포트가 확인된 뒤 실행합니다.

```powershell
arduino-cli upload -p COM8 --fqbn esp32:esp32:esp32 HW\SmartShield_ESP32
arduino-cli monitor -p COM8 -c baudrate=115200
```

## 테스트 및 검증

빌드 성공은 실제 하드웨어 동작을 보장하지 않습니다. 최종 시연 전에는 다음 항목을 실제 장치에서 확인합니다.

- I2C Scanner 기준 센서 주소 인식
- ESP32 시리얼 로그의 센서 초기화 상태
- BLE 스캔, 연결, Notify 수신, Write 명령 전송
- 작업자 앱 UI와 위험도 계산 결과
- Firebase `currentStatus`, `riskLogs` 업로드
- 관리자 앱 목록과 상세 화면 표시
- RED LED, 진동 모터, 부저 출력 패턴

세부 검증 체크리스트 템플릿은 [MEASUREMENT_VERIFICATION.md](docs/MEASUREMENT_VERIFICATION.md)를 따릅니다. 현재 저장소 기준의 결과 요약은 [DEMO_VERIFICATION_RESULT.md](docs/DEMO_VERIFICATION_RESULT.md)에 `verified by build`, `verified by demo`, `partially verified`, `not verified`로 구분해 기록합니다.

## 주의사항

- MAX30102의 HR/SpO2 값은 간이 추정값이며 착용 상태와 움직임에 영향을 크게 받습니다.
- MAX30205 피부 온도 값은 의료용 체온 진단값이 아니라 작업 중 변화 추세 확인용입니다.
- 센서가 없거나 읽기 실패한 경우 일부 payload 값은 fallback으로 채워질 수 있습니다.
- Firebase 설정값과 보안 규칙은 배포 환경에서 별도 검토가 필요합니다.
- 현재 코드 기준 기본 Worker ID와 BLE 이름은 단일 장치 시연용 값입니다.

## 하위 문서

| 문서 | 설명 |
|---|---|
| [HW README](HW/README.md) | 하드웨어 폴더와 센서·출력 장치 기준 |
| [SmartShield_ESP32 README](HW/SmartShield_ESP32/README.md) | ESP32 펌웨어 파일, 설정, payload, 출력 패턴 |
| [SW README](SW/README.md) | Android 앱 구성 |
| [작업자 앱 README](SW/HNU_PPE_Control/README.md) | 작업자 앱 실행 흐름과 주요 파일 |
| [관리자 앱 README](SW/HNU_PPE_Manager/README.md) | 관리자 앱 Firebase 조회와 화면 구성 |
| [docs README](docs/README.md) | 문서 폴더 구성과 읽는 순서 |
| [SYSTEM_ARCHITECTURE.md](docs/SYSTEM_ARCHITECTURE.md) | 전체 시스템 구성과 데이터 흐름 |
| [BLE_PROTOCOL.md](docs/BLE_PROTOCOL.md) | BLE UUID, Notify, Write 규격 |
| [FIREBASE_SCHEMA.md](docs/FIREBASE_SCHEMA.md) | Firebase 저장 구조 |
| [RISK_ALGORITHM.md](docs/RISK_ALGORITHM.md) | 위험도 판단 기준 |
| [MEASUREMENT_VERIFICATION.md](docs/MEASUREMENT_VERIFICATION.md) | 최종 실측 검증 체크리스트 템플릿 |
| [DEMO_VERIFICATION_RESULT.md](docs/DEMO_VERIFICATION_RESULT.md) | 빌드·시연 기준 검증 결과 요약 |
| [TEST_PLAN.md](docs/TEST_PLAN.md) | 최종 시연 전 테스트 시나리오 |
| [FINAL_CHECKLIST.md](docs/FINAL_CHECKLIST.md) | 발표 전 최종 점검 항목 |
