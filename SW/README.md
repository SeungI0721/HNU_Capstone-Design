# Smart Shield SW

## 1. 개요

`SW` 폴더는 Smart Shield 캡스톤 프로젝트의 Android 애플리케이션 소프트웨어를 관리하는 공간이다.

Smart Shield는 건설 현장 작업자의 생체 데이터, 환경 데이터, 자세 데이터를 기반으로 온열질환 및 이상 상태를 실시간 감지하는 PPE 웨어러블 기반 위험 감지 시스템이다.

소프트웨어는 작업자 앱과 관리자 앱으로 분리하여 관리한다.

```text
SW/
├── HNU_PPE_Control/       # 작업자 앱
└── HNU_PPE_Supervisor/    # 관리자 앱
````

---

## 2. 앱 분리 기준

Smart Shield 시스템은 사용자 역할에 따라 앱을 분리한다.

| 앱                  | 대상 사용자         | 주요 역할                                     |
| ------------------ | -------------- | ----------------------------------------- |
| HNU_PPE_Control    | 작업자, 일용직 단순노동자 | ESP32 연결, 센서 수신, 위험도 계산, 경고, Firebase 업로드 |
| HNU_PPE_Supervisor | 관리자, 현장 감독자    | Firebase 데이터 읽기, 작업자 상태 모니터링              |

관리자는 현장 상황에 따라 두 앱을 모두 사용할 수 있다.

* `HNU_PPE_Control`: 특정 작업자 장치와 직접 연결하여 센서 데이터를 수집하는 `작업자` 앱
* `HNU_PPE_Supervisor`: 여러 작업자의 상태를 Firebase 기반으로 모니터링하는 `관리자` 앱

따라서 두 앱 모두 Foreground Service를 제공한다.

---

## 3. 전체 시스템 구조

Smart Shield의 전체 소프트웨어 흐름은 다음과 같다.

```text
ESP32 웨어러블 장치
    ↓ BLE Notify
HNU_PPE_Control
    ↓ Firebase Upload
Firebase Realtime Database
    ↓ Read Only
HNU_PPE_Supervisor
```

세부 흐름은 다음과 같다.

```text
센서 데이터 수집
→ ESP32
→ BLE Notify
→ 작업자 앱 HNU_PPE_Control
→ 데이터 파싱
→ 위험도 계산
→ 앱 알림
→ ESP32 제어 명령 전송
→ Firebase 업로드
→ 관리자 앱 HNU_PPE_Supervisor에서 상태 조회
```

---

## 4. HNU_PPE_Control

`HNU_PPE_Control`은 작업자 전용 Android 앱이다.

대상 사용자는 다음과 같다.

* 건설 현장 작업자
* 일용직 단순노동자
* PPE 웨어러블 장치를 착용하는 사용자

---

### 4.1 작업자 앱 주요 역할

`HNU_PPE_Control`의 주요 기능은 다음과 같다.

* ESP32 BLE 장치 스캔
* BLE 장치 연결 및 해제
* BLE Notify 기반 센서 데이터 수신
* 센서 payload 파싱
* 온열질환 및 이상 상태 위험도 계산
* 정상 / 주의 / 위험 / 응급 단계 분류
* 앱 화면에 센서값 및 위험 상태 표시
* 위험 상황 발생 시 앱 팝업 및 스마트폰 진동 알림
* BLE Write를 통해 ESP32로 위험 단계 제어 명령 전송
* Firebase Realtime Database에 현재 상태 업로드
* 위험 또는 응급 상황 발생 시 위험 로그 저장
* riskLogs 중복 저장 방지
* Foreground Service 기반 백그라운드 동작 유지
* BLE 연결 끊김 시 자동 재연결 시도
* 10분 이상 재연결 실패 시 세션 종료 처리

---

### 4.2 작업자 앱 내부 구조

`HNU_PPE_Control`은 현재 다음 구조를 기준으로 사용한다.

```text
com.example.hnu_ppe_control
├── MainActivity.kt
├── alert/
│   └── AlertManager.kt
├── ble/
│   ├── BleConstants.kt
│   ├── BleManager.kt
│   └── BlePermissionHelper.kt
├── data/
│   ├── RiskLevel.kt
│   └── SensorData.kt
├── firebase/
│   ├── FirebaseStatusUploader.kt
│   └── RiskLogPolicy.kt
├── parser/
│   └── SensorDataParser.kt
├── risk/
│   ├── HeatstrokeAnalyzer.kt
│   └── RiskCommandMapper.kt
├── service/
│   ├── ForegroundServiceController.kt
│   └── SmartShieldForegroundService.kt
├── test/
│   └── FakeSensorDataProvider.kt
└── ui/
    └── MainUiController.kt
```

각 파일의 역할은 다음과 같다.

| 파일                              | 역할                                      |
| ------------------------------- | --------------------------------------- |
| MainActivity.kt                 | 앱 전체 흐름 제어                              |
| AlertManager.kt                 | 팝업 및 스마트폰 진동 알림                         |
| BleConstants.kt                 | BLE UUID 및 요청 코드 관리                     |
| BleManager.kt                   | BLE 스캔, 연결, Notify 수신, Write 전송, 자동 재연결 |
| BlePermissionHelper.kt          | Android 버전별 BLE 권한 처리                   |
| RiskLevel.kt                    | 위험 단계 enum 관리                           |
| SensorData.kt                   | 센서 데이터 모델                               |
| FirebaseStatusUploader.kt       | Firebase currentStatus 및 riskLogs 업로드   |
| RiskLogPolicy.kt                | 위험 로그 중복 저장 방지                          |
| SensorDataParser.kt             | BLE payload 문자열 파싱                      |
| HeatstrokeAnalyzer.kt           | 위험도 계산                                  |
| RiskCommandMapper.kt            | 위험 단계를 ESP32 명령으로 변환                    |
| ForegroundServiceController.kt  | Foreground Service 시작/종료 제어             |
| SmartShieldForegroundService.kt | 백그라운드 실행 유지                             |
| FakeSensorDataProvider.kt       | 테스트용 가상 센서 데이터 생성                       |
| MainUiController.kt             | 화면 표시 및 UI 이벤트 관리                       |

---

### 4.3 작업자 앱 데이터 처리 흐름

작업자 앱의 기본 처리 흐름은 다음과 같다.

```text
BLE Notify 수신
→ SensorDataParser
→ SensorData 객체 생성
→ HeatstrokeAnalyzer
→ RiskLevel 계산
→ RiskCommandMapper
→ ESP32 제어 명령 생성
→ MainUiController
→ 화면 갱신
→ AlertManager
→ 팝업 및 진동 알림
→ FirebaseStatusUploader
→ currentStatus 업로드
→ RiskLogPolicy
→ riskLogs 저장 여부 판단
→ riskLogs 업로드
```

ESP32 제어 흐름은 다음과 같다.

```text
RiskLevel 계산
→ RiskCommandMapper
→ BleManager.writeCommand()
→ BLE Write
→ ESP32
→ LED / 진동 모터 / 부저 동작
```

---

## 5. HNU_PPE_Supervisor

`HNU_PPE_Supervisor`는 관리자 전용 Android 앱이다.

관리자 앱은 Firebase Realtime Database에 저장된 작업자 상태를 읽어 현장 관리자가 작업자 상태를 모니터링할 수 있도록 한다.

관리자 앱은 읽기 전용이다.

---

### 5.1 관리자 앱 주요 역할

`HNU_PPE_Supervisor`의 주요 기능은 다음과 같다.

* Firebase Realtime Database 연결
* 전체 작업자 목록 조회
* 작업자별 currentStatus 조회
* 작업자별 riskLogs 조회
* 작업자 위험 단계 표시
* 위험 또는 응급 작업자 강조 표시
* 마지막 업데이트 시간 표시
* BLE 연결 여부 확인
* 앱 세션 활성 상태 확인
* Foreground Service 기반 모니터링 유지

---

### 5.2 관리자 앱에서 하지 않는 것

관리자 앱은 다음 기능을 수행하지 않는다.

* ESP32 BLE 직접 연결
* ESP32 직접 제어
* 센서 데이터 직접 수신
* 위험도 직접 계산
* Firebase 데이터 임의 수정
* 작업자 앱의 BLE 세션 제어
* ESP32의 LED / 진동 모터 / 부저 직접 제어

관리자 앱은 오직 Firebase에 저장된 데이터를 읽어 보여주는 모니터링 앱이다.

---

### 5.3 관리자 앱 내부 구조 예정

`HNU_PPE_Supervisor`는 다음 구조를 기준으로 개발한다.

```text
com.example.hnu_ppe_supervisor
├── MainActivity.kt
├── data/
│   ├── WorkerStatus.kt
│   └── RiskLog.kt
├── firebase/
│   └── FirebaseStatusReader.kt
├── service/
│   ├── ForegroundServiceController.kt
│   └── SupervisorForegroundService.kt
└── ui/
    ├── SupervisorUiController.kt
    ├── WorkerListAdapter.kt
    └── RiskLogAdapter.kt
```

각 파일의 역할은 다음과 같다.

| 파일                             | 역할                                   |
| ------------------------------ | ------------------------------------ |
| MainActivity.kt                | 관리자 앱 전체 흐름 제어                       |
| WorkerStatus.kt                | 작업자 현재 상태 데이터 모델                     |
| RiskLog.kt                     | 위험 로그 데이터 모델                         |
| FirebaseStatusReader.kt        | Firebase currentStatus 및 riskLogs 읽기 |
| ForegroundServiceController.kt | Foreground Service 시작/종료 제어          |
| SupervisorForegroundService.kt | 관리자 앱 모니터링 서비스 유지                    |
| SupervisorUiController.kt      | 관리자 앱 화면 표시 제어                       |
| WorkerListAdapter.kt           | 작업자 목록 표시                            |
| RiskLogAdapter.kt              | 위험 로그 목록 표시                          |

---

### 5.4 관리자 앱 데이터 처리 흐름

관리자 앱의 기본 처리 흐름은 다음과 같다.

```text
Firebase Realtime Database
→ workers/{workerId}/currentStatus 읽기
→ WorkerStatus 객체 변환
→ 작업자 목록 UI 갱신
→ 위험 단계 표시
→ 위험/응급 작업자 강조
```

위험 로그 조회 흐름은 다음과 같다.

```text
Firebase Realtime Database
→ workers/{workerId}/riskLogs 읽기
→ RiskLog 객체 변환
→ 위험 로그 목록 UI 갱신
```

---

## 6. Foreground Service 기준

작업자 앱과 관리자 앱 모두 Foreground Service를 제공한다.

두 앱의 서비스 목적은 다르다.

| 앱                  | Foreground Service 목적                |
| ------------------ | ------------------------------------ |
| HNU_PPE_Control    | BLE 연결 유지, 센서 수신 유지, Firebase 업로드 유지 |
| HNU_PPE_Supervisor | Firebase 모니터링 유지, 위험 상태 알림 유지        |

---

### 6.1 작업자 앱 Foreground Service

작업자 앱의 Foreground Service는 BLE 기반 센서 수신과 Firebase 업로드를 유지하기 위한 목적이다.

주요 역할은 다음과 같다.

* 앱이 백그라운드에 있어도 실행 상태 유지
* BLE 연결 상태 유지 보조
* BLE Notify 수신 유지 보조
* Firebase currentStatus 업로드 유지
* 위험 상태 발생 시 알림 가능
* 시스템에 의해 앱이 임의 종료되는 상황 감소

예시 알림 문구:

```text
Smart Shield Worker 실행 중
BLE 센서 수신과 Firebase 업로드를 유지합니다.
```

---

### 6.2 관리자 앱 Foreground Service

관리자 앱의 Foreground Service는 Firebase 기반 작업자 상태 모니터링을 유지하기 위한 목적이다.

관리자는 현장 상황에 따라 관리자 앱을 백그라운드에 둔 상태에서도 위험 작업자 상태를 확인해야 할 수 있다.

주요 역할은 다음과 같다.

* 앱이 백그라운드에 있어도 관리자 모니터링 상태 유지
* Firebase workers 상태 구독 유지
* 위험 또는 응급 작업자 감지 시 알림 유지
* 현재 모니터링 중임을 시스템 알림으로 표시
* 시스템에 의해 앱이 임의 종료되는 상황 감소

예시 알림 문구:

```text
Smart Shield Supervisor 실행 중
작업자 상태를 실시간 모니터링합니다.
```

---

## 7. BLE 통신 규격

BLE 통신은 작업자 앱 `HNU_PPE_Control`에서만 사용한다.

관리자 앱 `HNU_PPE_Supervisor`는 BLE를 사용하지 않는다.

---

### 7.1 BLE 장치 이름

ESP32 BLE 장치 이름은 다음 형식을 사용한다.

```text
SS_XXXX
```

예시:

```text
SS_0001
SS_0002
SS_0003
```

`XXXX`는 작업자 또는 장치 ID이며, Firebase의 workerId와 동일하게 사용한다.

예시:

```text
BLE 이름: SS_0001
Payload ID: ID:0001
Firebase 경로: workers/0001
```

---

### 7.2 BLE UUID

Smart Shield BLE 통신에 사용하는 UUID는 다음과 같다.

```text
Service UUID:
089fca17-755f-4578-b8af-ee5e32526b0f

Sensor Notify Characteristic UUID:
0000FFF1-0000-1000-8000-00805F9B34FB

Control Write Characteristic UUID:
0000FFF2-0000-1000-8000-00805F9B34FB

CCCD UUID:
00002902-0000-1000-8000-00805F9B34FB
```

각 UUID의 역할은 다음과 같다.

| 항목                           | 역할                         |
| ---------------------------- | -------------------------- |
| Service UUID                 | Smart Shield 전용 BLE 서비스 식별 |
| Sensor Notify Characteristic | ESP32에서 작업자 앱으로 센서 데이터 전송  |
| Control Write Characteristic | 작업자 앱에서 ESP32로 위험 단계 명령 전송 |
| CCCD                         | Notify 활성화 설정              |

---

## 8. BLE Sensor Payload 규격

ESP32는 작업자 앱으로 다음 형식의 문자열 데이터를 전송한다.

```text
ID:0001,TEMP:36.5,HR:82,SPO2:98,ENV:28.5,HUM:55,LUX:8000,POSTURE:NORMAL
```

---

### 8.1 필수 필드

```text
ID
TEMP
HR
SPO2
ENV
HUM
LUX
POSTURE
```

---

### 8.2 필드 설명

| 필드      | 의미                   | 예시     |
| ------- | -------------------- | ------ |
| ID      | 작업자 또는 장치 ID         | 0001   |
| TEMP    | 작업자 체온 또는 피부 온도 관련 값 | 36.5   |
| HR      | 심박수                  | 82     |
| SPO2    | 산소포화도                | 98     |
| ENV     | 주변 온도                | 28.5   |
| HUM     | 주변 습도                | 55     |
| LUX     | 조도값                  | 8000   |
| POSTURE | 자세 상태                | NORMAL |

---

### 8.3 POSTURE 값

`POSTURE`는 다음 값만 사용한다.

```text
NORMAL
WARNING
UNSTABLE
FALL
EMERGENCY
```

---

## 9. 위험 단계

Smart Shield 작업자 앱은 위험 단계를 다음과 같이 분류한다.

| 내부 값      | 표시 값 | 의미              |
| --------- | ---- | --------------- |
| SAFE      | 정상   | 현재 위험 없음        |
| CAUTION   | 주의   | 위험 가능성 존재       |
| DANGER    | 위험   | 위험 상태 감지        |
| EMERGENCY | 응급   | 즉각 대응 필요        |
| ERROR     | 오류   | 데이터 오류 또는 파싱 실패 |

---

## 10. ESP32 제어 명령

작업자 앱은 위험 단계에 따라 ESP32로 다음 명령을 전송한다.

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

ESP32는 해당 명령을 수신한 뒤 LED, 진동 모터, 부저를 제어한다.

| 위험 단계 | 명령             | ESP32 동작 |
| ----- | -------------- | -------- |
| 정상    | RISK:SAFE      | 경고 출력 없음 |
| 주의    | RISK:CAUTION   | 약한 경고    |
| 위험    | RISK:DANGER    | 강한 경고    |
| 응급    | RISK:EMERGENCY | 긴급 경고    |

---

## 11. Firebase Realtime Database 구조

Smart Shield SW는 Firebase Realtime Database를 사용한다.

기본 저장 경로는 다음과 같다.

```text
workers/{workerId}/currentStatus
workers/{workerId}/riskLogs/{logId}
```

예시:

```text
workers
└── 0001
    ├── currentStatus
    └── riskLogs
```

---

### 11.1 currentStatus

`currentStatus`는 작업자의 현재 상태를 저장한다.

```text
workers/{workerId}/currentStatus
```

저장 필드는 다음과 같다.

| 필드               | 설명               |
| ---------------- | ---------------- |
| workerId         | 작업자 ID           |
| deviceName       | BLE 장치 이름        |
| temp             | 체온 또는 피부 온도 관련 값 |
| hr               | 심박수              |
| spo2             | 산소포화도            |
| env              | 주변 온도            |
| hum              | 주변 습도            |
| lux              | 조도값              |
| directSunlight   | 직사광선 노출 여부       |
| posture          | 자세 상태            |
| riskLevel        | 위험 단계            |
| riskCommand      | ESP32 제어 명령      |
| bleConnected     | BLE 연결 여부        |
| appSessionActive | 앱 세션 활성 상태       |
| updatedAt        | 마지막 업데이트 시간      |

---

### 11.2 riskLogs

`riskLogs`는 위험 또는 응급 상황 발생 시 저장되는 이벤트 로그이다.

```text
workers/{workerId}/riskLogs/{logId}
```

저장 필드는 다음과 같다.

| 필드             | 설명               |
| -------------- | ---------------- |
| workerId       | 작업자 ID           |
| riskLevel      | 위험 단계            |
| riskCommand    | ESP32 제어 명령      |
| temp           | 체온 또는 피부 온도 관련 값 |
| hr             | 심박수              |
| spo2           | 산소포화도            |
| env            | 주변 온도            |
| hum            | 주변 습도            |
| lux            | 조도값              |
| directSunlight | 직사광선 노출 여부       |
| posture        | 자세 상태            |
| message        | 위험 로그 메시지        |
| createdAt      | 로그 생성 시간         |

---

## 12. riskLogs 저장 정책

현재 SW 기준에서 `riskLogs`는 모든 상태를 저장하지 않는다.

저장 대상은 다음과 같다.

```text
DANGER
EMERGENCY
```

저장 정책은 다음과 같다.

* SAFE 상태는 riskLogs에 저장하지 않는다.
* CAUTION 상태는 currentStatus에는 반영하지만 riskLogs에는 저장하지 않는다.
* DANGER 상태가 처음 발생하면 riskLogs에 저장한다.
* EMERGENCY 상태가 처음 발생하면 riskLogs에 저장한다.
* 같은 위험 단계가 반복되면 중복 저장하지 않는다.
* SAFE 또는 CAUTION 상태로 돌아가면 중복 저장 상태를 초기화한다.
* 이후 다시 DANGER 또는 EMERGENCY가 발생하면 새 로그를 저장한다.

---

## 13. 관리자 앱 읽기 기준

관리자 앱은 Firebase에서 다음 경로를 읽는다.

```text
workers
```

작업자별 현재 상태는 다음 경로에서 읽는다.

```text
workers/{workerId}/currentStatus
```

작업자별 위험 로그는 다음 경로에서 읽는다.

```text
workers/{workerId}/riskLogs
```

관리자 앱은 이 데이터를 읽어 화면에 표시한다.

예상 표시 항목은 다음과 같다.

* 작업자 ID
* BLE 장치명
* 현재 위험 단계
* 체온 또는 피부 온도 관련 값
* 심박수
* 산소포화도
* 주변 온도
* 습도
* 조도
* 직사광선 노출 여부
* 자세 상태
* BLE 연결 여부
* 앱 세션 활성 여부
* 마지막 업데이트 시간
* 위험 로그 목록

---

## 14. 개발 환경

현재 SW 개발 기준은 다음과 같다.

| 항목              | 내용                         |
| --------------- | -------------------------- |
| Platform        | Android                    |
| Language        | Kotlin                     |
| UI              | XML                        |
| Architecture    | Activity 중심 구조             |
| Database        | Firebase Realtime Database |
| Communication   | BLE, Firebase              |
| Hardware Target | ESP32                      |
| Build Tool      | Gradle                     |
| IDE             | Android Studio             |

---

## 16. 개발 시 주의사항

### 16.1 Control과 Supervisor 역할 분리

작업자 앱과 관리자 앱의 역할은 반드시 분리한다.

`HNU_PPE_Control`은 작업자 앱으로서 BLE 연결, 위험도 계산, ESP32 제어, Firebase 업로드를 담당한다.

`HNU_PPE_Supervisor`는 관리자 앱으로서 Firebase 읽기 전용 모니터링만 담당한다.

관리자가 두 앱을 모두 사용할 수는 있지만, 앱의 기능적 책임은 섞지 않는다.

---

### 16.2 HNU_PPE_Control은 기존 작업자 앱 기준 유지

`HNU_PPE_Control`은 기존에 개발하던 앱 내용을 그대로 사용한다.

따라서 현재 구현된 다음 구조를 유지한다.

* BLE Manager
* SensorDataParser
* HeatstrokeAnalyzer
* RiskCommandMapper
* FirebaseStatusUploader
* RiskLogPolicy
* AlertManager
* Foreground Service
* MainUiController

---

### 16.3 Supervisor는 읽기 전용

`HNU_PPE_Supervisor`는 Firebase 데이터를 조회하는 앱이다.

`Supervisor`라는 이름은 현장 감독자 또는 관리자가 작업자 상태를 감시한다는 의미이며, 직접 제어 기능을 의미하지 않는다.

따라서 관리자 앱에는 ESP32 제어, BLE 연결, 위험도 계산 기능을 넣지 않는다.

---

### 16.4 ESP32와 앱의 BLE UUID 일치

ESP32 코드와 Android 작업자 앱의 BLE UUID가 반드시 일치해야 한다.

다음 항목이 서로 다르면 앱이 ESP32를 찾거나 연결할 수 없다.

* Service UUID
* Notify Characteristic UUID
* Write Characteristic UUID
* CCCD UUID

---

### 16.5 Payload 필드 일치

작업자 앱은 현재 다음 필드를 기준으로 파싱한다.

```text
ID, TEMP, HR, SPO2, ENV, HUM, LUX, POSTURE
```

따라서 ESP32가 `LUX` 값을 보내지 않으면 파싱 실패가 발생할 수 있다.

---

### 16.6 workerId 일관성 유지

다음 값은 반드시 동일해야 한다.

```text
SS_0001
ID:0001
workers/0001
```

즉, BLE 이름, payload ID, Firebase workerId가 일치해야 한다.

---

### 16.7 MLX90614 및 DFPlayer Mini 제외

현재 최종 SW 기준에서는 MLX90614와 DFPlayer Mini를 사용하지 않는다.

따라서 앱 설명, 발표자료, README, 코드 주석에서 다음 표현은 제거하거나 수정해야 한다.

* MLX90614 기반 비접촉 피부온도 측정
* DFPlayer Mini 기반 음성 안내
* 음성 경고 중심 시스템

현재 기준의 경고 출력은 다음과 같다.

* 앱 팝업
* 스마트폰 진동
* ESP32 LED
* ESP32 진동 모터
* ESP32 부저

---

## 17. 향후 작업 계획

SW 영역의 향후 작업은 다음 순서로 진행한다.

1. `HNU_PPE_Control` 작업자 앱 기존 코드 정리
2. 작업자 앱 BLE UUID 및 payload 규격 재확인
3. 작업자 앱 SensorDataParser 동작 확인
4. 작업자 앱 HeatstrokeAnalyzer 동작 확인
5. 작업자 앱 RiskCommandMapper 동작 확인
6. 작업자 앱 BLE Notify 수신 확인
7. 작업자 앱 BLE Write 명령 전송 확인
8. 작업자 앱 Firebase currentStatus 업로드 확인
9. 작업자 앱 riskLogs 저장 및 중복 방지 확인
10. 작업자 앱 Foreground Service 동작 확인
11. 작업자 앱 자동 재연결 정책 확인
12. 작업자 앱 Fake 데이터 테스트 기능 확인
13. `HNU_PPE_Supervisor` 관리자 앱 신규 생성 또는 분리
14. 관리자 앱 Firebase 읽기 기능 구현
15. 관리자 앱 Foreground Service 적용
16. 관리자 앱 작업자 상태 목록 표시 구현
17. 관리자 앱 위험 로그 조회 구현
18. 두 앱의 Firebase 데이터 구조 연동 확인
19. 실제 ESP32와 작업자 앱 통합 테스트
20. 작업자 앱과 관리자 앱 동시 운용 테스트

---

## 18. 최종 요약

`SW` 폴더는 Smart Shield 프로젝트의 Android 앱 소프트웨어를 관리하는 공간이다.

`HNU_PPE_Control`은 작업자, 특히 건설 현장의 일용직 단순노동자가 사용하는 앱이다. ESP32와 BLE로 연결되어 센서 데이터를 수신하고, 위험도를 계산하며, 작업자에게 경고하고 Firebase에 데이터를 업로드한다.

`HNU_PPE_Supervisor`는 관리자 또는 현장 감독자가 사용하는 앱이다. Firebase에 저장된 작업자 상태와 위험 로그를 읽기 전용으로 조회한다.

관리자는 현장 운영상 두 앱을 모두 사용할 수 있으므로, 작업자 앱과 관리자 앱 모두 Foreground Service를 제공한다.

단, 기능적 책임은 명확히 분리한다.

작업자 앱은 데이터 수신, 위험 판단, 제어, 업로드를 담당한다.

관리자 앱은 Firebase 읽기 전용 모니터링을 담당한다.
