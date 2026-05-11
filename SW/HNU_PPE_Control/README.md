# HNU_PPE_Control

## 1. 프로젝트 개요

`HNU_PPE_Control`은 Smart Shield 시스템의 작업자 전용 Android 애플리케이션이다.

이 앱은 건설 현장에서 작업자가 착용한 ESP32 기반 PPE 웨어러블 장치와 BLE로 연결되어 센서 데이터를 실시간으로 수신하고, 수신된 데이터를 기반으로 온열질환 및 이상 상태 위험도를 판단한다.

작업자 앱은 위험 상황 발생 시 화면 표시, 팝업, 스마트폰 진동으로 작업자에게 즉시 알림을 제공하며, BLE Write를 통해 ESP32에 위험 단계 제어 명령을 전송하여 LED, 진동 모터, 부저 등의 물리적 경고 장치를 동작시킨다.

또한 Firebase Realtime Database에 작업자의 현재 상태와 위험 로그를 저장하여 작업자의 상태 이력을 관리한다.

---

## 2. 핵심 목적

`HNU_PPE_Control`의 목적은 다음과 같다.

- 작업자의 생체 데이터, 환경 데이터, 자세 데이터를 실시간 수집
- 온열질환 및 이상 상태 위험도 계산
- 정상 / 주의 / 위험 / 응급 단계 분류
- 위험 상황 발생 시 작업자에게 즉시 알림 제공
- ESP32 웨어러블 장치의 경고 장치 제어
- Firebase Realtime Database에 작업자 상태 저장
- 위험 또는 응급 상황 발생 시 위험 로그 기록
- BLE 연결 유지 및 자동 재연결 지원
- 백그라운드 상태에서도 주요 기능 유지

---

## 3. 대상 사용자

이 앱의 대상 사용자는 건설 현장에서 PPE 웨어러블 장치를 착용하는 작업자이다.

주요 사용 대상은 다음과 같다.

- 건설 현장 작업자
- 일용직 단순노동자
- 야외 고온 환경에서 장시간 작업하는 사용자
- PPE 기반 안전관리 시스템을 착용하는 사용자

---

## 4. 개발 환경

| 항목 | 내용 |
|---|---|
| Platform | Android |
| Language | Kotlin |
| UI | XML |
| Architecture | Activity 중심 구조 |
| Database | Firebase Realtime Database |
| Communication | BLE |
| Hardware Target | ESP32 |
| IDE | Android Studio |
| Build Tool | Gradle |

---

## 5. 전체 동작 구조

작업자 앱의 전체 동작 흐름은 다음과 같다.

```text
ESP32 웨어러블 장치
    ↓ BLE Notify
HNU_PPE_Control
    ↓ 데이터 파싱
위험도 계산
    ↓
작업자 알림
    ↓
ESP32 제어 명령 전송
    ↓
Firebase 상태 저장
````

세부 흐름은 다음과 같다.

```text
센서 데이터 수집
→ ESP32
→ BLE Notify
→ 작업자 앱
→ SensorDataParser
→ HeatstrokeAnalyzer
→ RiskCommandMapper
→ UI 표시
→ AlertManager
→ BLE Write
→ FirebaseStatusUploader
→ RiskLogPolicy
→ riskLogs 저장
```

---

## 6. 주요 기능

### 6.1 BLE 장치 스캔

앱은 Smart Shield 전용 ESP32 BLE 장치를 검색한다.

BLE 장치 이름은 다음 형식을 사용한다.

```text
SS_XXXX
```

예시:

```text
SS_0001
SS_0002
SS_0003
```

`XXXX`는 작업자 또는 장치 ID이며, 앱 내부의 workerId 및 Firebase 저장 경로와 동일하게 사용한다.

---

### 6.2 BLE 연결

앱은 사용자가 선택한 ESP32 장치와 BLE GATT 연결을 수행한다.

연결 후 다음 작업을 진행한다.

* GATT Service 탐색
* Notify Characteristic 확인
* Write Characteristic 확인
* CCCD 설정
* 센서 데이터 수신 준비
* 연결 상태 UI 표시

---

### 6.3 BLE Notify 수신

ESP32는 작업자 앱으로 센서 데이터를 BLE Notify 방식으로 전송한다.

앱은 수신된 문자열 payload를 읽고, 센서 데이터 파서로 전달한다.

---

### 6.4 센서 데이터 파싱

수신된 BLE payload는 `SensorDataParser`에서 분석된다.

앱이 사용하는 기본 payload 형식은 다음과 같다.

```text
ID:0001,TEMP:36.5,HR:82,SPO2:98,ENV:28.5,HUM:55,LUX:8000,POSTURE:NORMAL
```

필수 필드는 다음과 같다.

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

각 필드는 작업자 상태 판단에 사용되므로 ESP32에서 누락 없이 전송해야 한다.

---

### 6.5 위험도 계산

파싱된 센서 데이터는 `HeatstrokeAnalyzer`에서 위험도 계산에 사용된다.

위험도 계산에 사용하는 주요 데이터는 다음과 같다.

| 데이터     | 의미                   |
| ------- | -------------------- |
| TEMP    | 작업자 체온 또는 피부 온도 관련 값 |
| HR      | 심박수                  |
| SPO2    | 산소포화도                |
| ENV     | 주변 온도                |
| HUM     | 주변 습도                |
| LUX     | 조도값                  |
| POSTURE | 자세 상태                |

위험도 단계는 다음과 같이 분류한다.

| 내부 값      | 표시 값 | 의미              |
| --------- | ---- | --------------- |
| SAFE      | 정상   | 위험 없음           |
| CAUTION   | 주의   | 위험 가능성 존재       |
| DANGER    | 위험   | 위험 상태 감지        |
| EMERGENCY | 응급   | 즉각 대응 필요        |
| ERROR     | 오류   | 데이터 오류 또는 파싱 실패 |

---

### 6.6 작업자 알림

위험 단계에 따라 앱은 작업자에게 즉시 알림을 제공한다.

알림 방식은 다음과 같다.

* 화면 상태 표시
* 위험 단계 텍스트 표시
* 팝업 알림
* 스마트폰 진동

위험 단계별 기본 동작은 다음과 같다.

| 위험 단계 | 앱 동작               |
| ----- | ------------------ |
| 정상    | 상태 표시 유지           |
| 주의    | 팝업 알림, 짧은 진동       |
| 위험    | 팝업 알림, 강한 진동       |
| 응급    | 긴급 팝업 알림, 강한 반복 진동 |
| 오류    | 오류 상태 표시           |

---

### 6.7 ESP32 제어 명령 전송

앱은 계산된 위험 단계에 따라 ESP32로 제어 명령을 전송한다.

제어 명령은 BLE Write를 통해 전달된다.

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

ESP32는 해당 명령을 수신하여 LED, 진동 모터, 부저를 제어한다.

| 위험 단계 | 전송 명령          | ESP32 동작 |
| ----- | -------------- | -------- |
| 정상    | RISK:SAFE      | 경고 출력 없음 |
| 주의    | RISK:CAUTION   | 약한 경고    |
| 위험    | RISK:DANGER    | 강한 경고    |
| 응급    | RISK:EMERGENCY | 긴급 경고    |

---

### 6.8 Firebase 현재 상태 업로드

앱은 작업자의 현재 상태를 Firebase Realtime Database에 업로드한다.

저장 경로는 다음과 같다.

```text
workers/{workerId}/currentStatus
```

예시:

```text
workers/0001/currentStatus
```

`currentStatus`는 현재 상태를 나타내므로 새로운 데이터가 들어올 때마다 갱신된다.

---

### 6.9 위험 로그 저장

위험 또는 응급 상태가 발생하면 앱은 Firebase에 위험 로그를 저장한다.

저장 경로는 다음과 같다.

```text
workers/{workerId}/riskLogs/{logId}
```

예시:

```text
workers/0001/riskLogs/{logId}
```

`riskLogs`는 이벤트 기록이므로 push 방식으로 누적 저장된다.

---

### 6.10 위험 로그 중복 저장 방지

`RiskLogPolicy`는 같은 위험 상태가 반복 저장되는 것을 방지한다.

저장 정책은 다음과 같다.

* SAFE 상태는 riskLogs에 저장하지 않는다.
* CAUTION 상태는 riskLogs에 저장하지 않는다.
* DANGER 상태가 처음 발생하면 riskLogs에 저장한다.
* EMERGENCY 상태가 처음 발생하면 riskLogs에 저장한다.
* 같은 위험 단계가 반복되면 중복 저장하지 않는다.
* SAFE 또는 CAUTION 상태로 돌아가면 중복 저장 상태를 초기화한다.
* 이후 다시 DANGER 또는 EMERGENCY가 발생하면 새 로그를 저장한다.

---

### 6.11 Foreground Service

앱은 BLE 수신과 Firebase 업로드 안정성을 위해 Foreground Service를 사용한다.

Foreground Service의 목적은 다음과 같다.

* 앱이 백그라운드에 있어도 실행 상태 유지
* BLE 연결 유지 보조
* BLE Notify 수신 유지 보조
* Firebase 업로드 유지 보조
* 위험 상황 발생 시 알림 제공
* Android 시스템에 의한 임의 종료 가능성 감소

예시 알림 문구:

```text
Smart Shield 실행 중
BLE 센서 수신과 Firebase 업로드를 유지합니다.
```

---

### 6.12 자동 재연결

BLE 연결이 비정상적으로 끊어진 경우 앱은 자동 재연결을 시도한다.

자동 재연결 정책은 다음과 같다.

* 수동 연결 해제가 아닌 경우 자동 재연결 시도
* 기존 연결 장치에 재연결 시도
* 일정 간격으로 재연결 반복
* 최대 10분간 재연결 시도
* 10분 이상 재연결 실패 시 세션 종료 처리
* 세션 종료 상태를 Firebase에 반영

---

### 6.13 Fake 데이터 테스트

앱은 실제 ESP32 연결 없이도 테스트할 수 있도록 Fake 센서 데이터 기능을 제공한다.

Fake 데이터 테스트를 통해 다음 흐름을 검증할 수 있다.

* 정상 데이터 처리
* 주의 상태 처리
* 위험 상태 처리
* 응급 상태 처리
* 낙상 상태 처리
* 파싱 실패 처리
* UI 표시 확인
* 팝업 및 진동 확인
* Firebase 업로드 확인
* 위험 로그 중복 저장 방지 확인

---

## 7. 패키지 구조

앱의 기본 패키지는 다음과 같다.

```text
com.example.hnu_ppe_control
```

파일 구조는 다음과 같다.

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

---

## 8. 파일별 역할

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

## 9. 핵심 클래스 설명

### 9.1 MainActivity.kt

`MainActivity`는 앱의 중심 실행 흐름을 담당한다.

주요 역할은 다음과 같다.

* 앱 초기화
* UI 컨트롤러 초기화
* BLE Manager 초기화
* 알림 관리자 초기화
* Firebase 업로더 초기화
* 버튼 이벤트 연결
* BLE 수신 데이터 처리
* 위험도 계산 흐름 실행
* 앱 종료 시 리소스 정리

---

### 9.2 BleManager.kt

`BleManager`는 BLE 통신을 담당한다.

주요 역할은 다음과 같다.

* BLE 사용 가능 여부 확인
* BLE 스캔
* Smart Shield 장치 필터링
* GATT 연결
* Service 탐색
* Notify 활성화
* 센서 데이터 수신
* ESP32 명령 Write
* 연결 해제 처리
* 자동 재연결 처리

---

### 9.3 SensorDataParser.kt

`SensorDataParser`는 BLE 문자열 payload를 앱 내부 데이터 모델로 변환한다.

주요 역할은 다음과 같다.

* 문자열 payload 분리
* key-value 데이터 추출
* 필수 필드 검증
* 숫자 변환
* 센서값 유효 범위 확인
* `SensorData` 객체 생성

---

### 9.4 HeatstrokeAnalyzer.kt

`HeatstrokeAnalyzer`는 센서 데이터를 기반으로 위험도를 계산한다.

주요 역할은 다음과 같다.

* 환경 위험 분석
* 생체 위험 분석
* 산소포화도 저하 반영
* 조도 기반 직사광선 위험 반영
* 자세 이상 반영
* 낙상 및 응급 상태 즉시 판정
* 최종 위험 단계 반환

---

### 9.5 RiskCommandMapper.kt

`RiskCommandMapper`는 위험 단계를 ESP32 제어 명령으로 변환한다.

예시는 다음과 같다.

```text
SAFE      → RISK:SAFE
CAUTION   → RISK:CAUTION
DANGER    → RISK:DANGER
EMERGENCY → RISK:EMERGENCY
ERROR     → RISK:SAFE
```

---

### 9.6 FirebaseStatusUploader.kt

`FirebaseStatusUploader`는 Firebase Realtime Database 업로드를 담당한다.

주요 역할은 다음과 같다.

* 현재 작업자 상태 업로드
* BLE 연결 상태 업로드
* 앱 세션 활성 상태 업로드
* 위험 로그 저장
* 업로드 성공/실패 결과 전달

---

### 9.7 RiskLogPolicy.kt

`RiskLogPolicy`는 위험 로그 저장 여부를 판단한다.

주요 역할은 다음과 같다.

* 위험 로그 저장 대상 판별
* DANGER / EMERGENCY 상태 저장
* SAFE / CAUTION 상태 제외
* 같은 위험 단계 반복 저장 방지
* 위험 상태 초기화 처리

---

### 9.8 AlertManager.kt

`AlertManager`는 작업자 알림을 담당한다.

주요 역할은 다음과 같다.

* 위험 단계별 팝업 표시
* 스마트폰 진동 실행
* 같은 위험 단계 반복 알림 방지
* 앱 종료 시 알림 상태 초기화

---

### 9.9 SmartShieldForegroundService.kt

`SmartShieldForegroundService`는 앱의 백그라운드 실행 상태를 유지한다.

주요 역할은 다음과 같다.

* Foreground Service 알림 생성
* Android Notification Channel 생성
* 앱 실행 상태 유지
* 백그라운드 동작 안정성 확보

---

### 9.10 MainUiController.kt

`MainUiController`는 화면 표시와 UI 이벤트 연결을 담당한다.

주요 역할은 다음과 같다.

* BLE 상태 표시
* 연결 장치 표시
* 센서 데이터 표시
* 위험 단계 표시
* ESP32 명령 표시
* Firebase 업로드 상태 표시
* 마지막 업데이트 시간 표시
* 버튼 이벤트 연결

---

## 10. BLE 통신 규격

### 10.1 BLE 장치 이름

ESP32 BLE 장치 이름은 다음 형식을 사용한다.

```text
SS_XXXX
```

예시:

```text
SS_0001
```

`XXXX`는 4자리 작업자 또는 장치 ID이다.

---

### 10.2 BLE UUID

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
| Sensor Notify Characteristic | ESP32에서 앱으로 센서 데이터 전송      |
| Control Write Characteristic | 앱에서 ESP32로 위험 단계 명령 전송     |
| CCCD                         | Notify 활성화 설정              |

---

## 11. BLE Payload 규격

### 11.1 기본 형식

```text
ID:0001,TEMP:36.5,HR:82,SPO2:98,ENV:28.5,HUM:55,LUX:8000,POSTURE:NORMAL
```

### 11.2 필수 필드

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

### 11.3 필드 설명

| 필드      | 자료형    | 설명               | 예시     |
| ------- | ------ | ---------------- | ------ |
| ID      | String | 작업자 또는 장치 ID     | 0001   |
| TEMP    | Double | 체온 또는 피부 온도 관련 값 | 36.5   |
| HR      | Int    | 심박수              | 82     |
| SPO2    | Int    | 산소포화도            | 98     |
| ENV     | Double | 주변 온도            | 28.5   |
| HUM     | Int    | 주변 습도            | 55     |
| LUX     | Int    | 조도값              | 8000   |
| POSTURE | String | 자세 상태            | NORMAL |

---

### 11.4 POSTURE 값

`POSTURE`는 다음 값만 사용한다.

```text
NORMAL
WARNING
UNSTABLE
FALL
EMERGENCY
```

| 값         | 의미     |
| --------- | ------ |
| NORMAL    | 정상 자세  |
| WARNING   | 주의 자세  |
| UNSTABLE  | 불안정 자세 |
| FALL      | 낙상     |
| EMERGENCY | 응급 상태  |

---

## 12. SensorData 모델

앱 내부 센서 데이터 모델은 다음 값을 포함한다.

```kotlin
data class SensorData(
    val id: String,
    val temp: Double,
    val hr: Int,
    val spo2: Int,
    val env: Double,
    val hum: Int,
    val lux: Int,
    val posture: String
)
```

조도값을 기반으로 직사광선 여부를 계산한다.

```kotlin
val directSunlight: Boolean
    get() = lux >= 50000
```

---

## 13. 위험도 계산 기준

위험도 계산은 센서 데이터 점수 기반 방식으로 수행한다.

### 13.1 즉시 응급 처리

다음 조건은 즉시 `EMERGENCY`로 판단한다.

```text
POSTURE == FALL
POSTURE == EMERGENCY
```

---

### 13.2 점수 기준

| 조건                  | 점수 |
| ------------------- | -- |
| TEMP ≥ 38.0         | +2 |
| TEMP ≥ 37.5         | +1 |
| HR ≥ 120            | +2 |
| HR ≥ 100            | +1 |
| SPO2 ≤ 90           | +3 |
| SPO2 ≤ 94           | +1 |
| ENV ≥ 35.0          | +3 |
| ENV ≥ 33.0          | +2 |
| ENV ≥ 31.0          | +1 |
| HUM ≥ 80            | +2 |
| HUM ≥ 70            | +1 |
| LUX ≥ 50000         | +2 |
| LUX ≥ 30000         | +1 |
| POSTURE == WARNING  | +2 |
| POSTURE == UNSTABLE | +2 |

---

### 13.3 최종 위험 단계

| 점수    | 위험 단계     |
| ----- | --------- |
| 0 ~ 1 | SAFE      |
| 2 ~ 3 | CAUTION   |
| 4 ~ 6 | DANGER    |
| 7 이상  | EMERGENCY |

---

## 14. Firebase Realtime Database 구조

### 14.1 기본 경로

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

### 14.2 currentStatus

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

예시:

```json
{
  "workerId": "0001",
  "deviceName": "SS_0001",
  "temp": 36.5,
  "hr": 82,
  "spo2": 98,
  "env": 28.5,
  "hum": 55,
  "lux": 8000,
  "directSunlight": false,
  "posture": "NORMAL",
  "riskLevel": "정상",
  "riskCommand": "RISK:SAFE",
  "bleConnected": true,
  "appSessionActive": true,
  "updatedAt": 1710000000000
}
```

---

### 14.3 riskLogs

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

예시:

```json
{
  "workerId": "0001",
  "riskLevel": "위험",
  "riskCommand": "RISK:DANGER",
  "temp": 38.1,
  "hr": 125,
  "spo2": 94,
  "env": 34.2,
  "hum": 81,
  "lux": 42000,
  "directSunlight": false,
  "posture": "WARNING",
  "message": "위험 상태 감지",
  "createdAt": 1710000000000
}
```

---

## 15. Android 권한

앱은 BLE, 진동, 인터넷, Foreground Service 동작을 위해 다음 권한을 사용한다.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.VIBRATE" />

<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

BLE 기능을 사용하기 위해 다음 feature를 선언한다.

```xml
<uses-feature
    android:name="android.hardware.bluetooth_le"
    android:required="true" />
```

---

## 16. Android 버전별 BLE 권한 기준

Android BLE 권한은 버전에 따라 다르게 처리한다.

| Android 버전    | 주요 권한                             |
| ------------- | --------------------------------- |
| Android 12 이상 | BLUETOOTH_SCAN, BLUETOOTH_CONNECT |
| Android 11 이하 | ACCESS_FINE_LOCATION              |
| 공통            | Bluetooth 사용 가능 여부 확인             |

Android 13 이상에서는 알림 표시를 위해 `POST_NOTIFICATIONS` 권한도 필요하다.

---

## 17. Foreground Service 등록

앱의 Foreground Service는 AndroidManifest.xml에 등록한다.

```xml
<service
    android:name=".service.SmartShieldForegroundService"
    android:exported="false"
    android:foregroundServiceType="connectedDevice" />
```

Foreground Service는 BLE 연결 및 센서 수신 상태를 유지하기 위한 목적으로 사용한다.

---

## 18. 앱 실행 흐름

### 18.1 실제 ESP32 연결 흐름

```text
1. 앱 실행
2. BLE 권한 확인
3. BLE 사용 가능 여부 확인
4. BLE 스캔 시작
5. SS_XXXX 형식의 ESP32 장치 검색
6. Target Service UUID 확인
7. 장치 목록 표시
8. 사용자가 장치 선택
9. GATT 연결
10. Service 탐색
11. Notify 활성화
12. 센서 payload 수신
13. SensorDataParser로 파싱
14. HeatstrokeAnalyzer로 위험도 계산
15. RiskCommandMapper로 ESP32 명령 생성
16. UI 갱신
17. AlertManager로 팝업 및 진동 처리
18. BLE Write로 ESP32 제어 명령 전송
19. Firebase currentStatus 업로드
20. RiskLogPolicy로 로그 저장 여부 판단
21. DANGER 또는 EMERGENCY 발생 시 riskLogs 저장
```

---

### 18.2 Fake 데이터 테스트 흐름

```text
1. Fake 데이터 테스트 실행
2. FakeSensorDataProvider가 테스트 payload 생성
3. SensorDataParser로 파싱
4. HeatstrokeAnalyzer로 위험도 계산
5. RiskCommandMapper로 명령 생성
6. UI 갱신
7. AlertManager 동작 확인
8. Firebase 업로드 확인
9. riskLogs 중복 저장 방지 확인
```

---

## 19. 테스트 항목

앱 테스트 시 다음 항목을 확인한다.

### 19.1 BLE 테스트

* BLE 권한 요청 정상 동작
* Bluetooth OFF 상태 처리
* BLE 스캔 정상 동작
* `SS_XXXX` 형식 장치 필터링
* Service UUID 필터링
* GATT 연결 성공 여부
* Notify 활성화 여부
* 센서 데이터 수신 여부
* BLE Write 명령 전송 여부
* 수동 연결 해제 여부
* 비정상 연결 해제 후 자동 재연결 여부
* 10분 재연결 실패 처리 여부

---

### 19.2 Parser 테스트

* 정상 payload 파싱
* ID 형식 오류 처리
* TEMP 범위 오류 처리
* HR 범위 오류 처리
* SPO2 범위 오류 처리
* ENV 범위 오류 처리
* HUM 범위 오류 처리
* LUX 범위 오류 처리
* POSTURE 허용값 검증
* 필수 필드 누락 처리

---

### 19.3 위험도 계산 테스트

* 정상 상태 계산
* 주의 상태 계산
* 위험 상태 계산
* 응급 상태 계산
* 낙상 상태 즉시 응급 처리
* 고온 환경 점수 반영
* 고습 환경 점수 반영
* 직사광선 조도 점수 반영
* 심박수 상승 점수 반영
* 산소포화도 저하 점수 반영
* 자세 이상 점수 반영

---

### 19.4 Firebase 테스트

* currentStatus 업로드 성공 여부
* riskLogs 저장 성공 여부
* DANGER 로그 저장 여부
* EMERGENCY 로그 저장 여부
* SAFE 상태에서 로그 미저장 여부
* CAUTION 상태에서 로그 미저장 여부
* 같은 위험 단계 반복 저장 방지 여부
* 위험 상태 해제 후 재발생 시 새 로그 저장 여부
* 앱 종료 시 세션 상태 반영 여부

---

### 19.5 UI 및 알림 테스트

* BLE 상태 표시
* 연결 장치 표시
* 센서값 표시
* 위험 단계 표시
* ESP32 명령 표시
* Firebase 업로드 상태 표시
* 마지막 업데이트 시간 표시
* 팝업 알림 표시
* 스마트폰 진동 동작
* 같은 위험 단계 반복 알림 방지

---

### 19.6 Foreground Service 테스트

* BLE 연결 시 Foreground Service 시작 여부
* 서비스 알림 표시 여부
* 백그라운드 상태에서 실행 유지 여부
* 화면 꺼짐 상태에서 동작 유지 여부
* 수동 연결 해제 시 서비스 종료 여부
* 앱 종료 시 리소스 정리 여부

---

## 20. 개발 시 주의사항

### 20.1 BLE UUID 일치

ESP32 코드와 앱의 BLE UUID는 반드시 일치해야 한다.

확인해야 할 항목은 다음과 같다.

* Service UUID
* Sensor Notify Characteristic UUID
* Control Write Characteristic UUID
* CCCD UUID

UUID가 하나라도 다르면 BLE 연결 또는 데이터 수신이 실패할 수 있다.

---

### 20.2 Payload 필드 일치

앱은 다음 필드를 기준으로 payload를 파싱한다.

```text
ID, TEMP, HR, SPO2, ENV, HUM, LUX, POSTURE
```

모든 필드는 필수이다.

따라서 ESP32에서 위 필드 중 하나라도 전송하지 않으면 파싱 실패가 발생할 수 있다.

---

### 20.3 workerId 일관성 유지

다음 값은 반드시 동일해야 한다.

```text
BLE 이름: SS_0001
Payload ID: ID:0001
Firebase 경로: workers/0001
```

BLE 장치 이름, payload ID, Firebase workerId가 일치해야 작업자 상태를 안정적으로 관리할 수 있다.

---

### 20.4 Android BLE 권한 처리

Android 12 이상과 Android 11 이하의 BLE 권한 체계가 다르다.

따라서 BLE 스캔 및 연결 전에는 반드시 버전별 권한을 확인해야 한다.

---

### 20.5 Foreground Service 정책

백그라운드에서도 BLE 수신과 Firebase 업로드를 안정적으로 유지하려면 Foreground Service가 필요하다.

단, Android 버전에 따라 Foreground Service 권한과 알림 권한이 필요할 수 있다.

---

### 20.6 센서값 이상 범위 처리

센서값은 실제 현장에서 노이즈가 발생할 수 있다.

따라서 파싱 단계에서 값의 범위를 검증하고, 비정상 데이터는 위험도 계산에 사용하지 않아야 한다.

---

### 20.7 중복 경고 방지

위험 단계가 바뀌지 않았는데 매초 팝업, 진동, 로그 저장이 반복되면 사용성이 떨어지고 데이터가 불필요하게 증가한다.

따라서 같은 위험 단계에 대해서는 반복 알림과 반복 로그 저장을 제한한다.

---

## 21. Git 관리 기준

이 앱에서 Git에 포함할 항목은 다음과 같다.

* Kotlin source code
* XML layout
* AndroidManifest.xml
* Gradle 설정 파일
* README 문서

Git에 포함하지 않을 항목은 다음과 같다.

* build/
* .gradle/
* local.properties
* 개인 PC 경로 정보
* 민감한 인증 정보
* 불필요한 IDE 캐시 파일

`.gitignore` 예시는 다음과 같다.

```gitignore
.gradle/
build/
local.properties
*.iml
.idea/caches/
.idea/libraries/
.idea/modules.xml
.idea/workspace.xml
.DS_Store
```

---

## 22. Commit Message 예시

### English

```text
docs: add worker app README
feat: implement BLE scan and connection
feat: add sensor data parser
feat: add heatstroke risk analyzer
feat: add risk command mapper
feat: upload current status to Firebase
feat: save danger and emergency risk logs
feat: prevent duplicate risk logs
feat: add foreground service
feat: add BLE auto reconnect
feat: add fake sensor data provider
fix: update BLE payload parsing
fix: require SPO2 field in sensor payload
refactor: separate worker app modules
```

### Korean

```text
문서: 작업자 앱 README 추가
기능: BLE 스캔 및 연결 구현
기능: 센서 데이터 파서 추가
기능: 온열질환 위험도 분석 로직 추가
기능: 위험 단계 명령 변환 기능 추가
기능: Firebase 현재 상태 업로드 추가
기능: 위험 및 응급 로그 저장 추가
기능: 위험 로그 중복 저장 방지 추가
기능: 포그라운드 서비스 추가
기능: BLE 자동 재연결 추가
기능: 테스트용 가상 센서 데이터 추가
수정: BLE payload 파싱 형식 수정
수정: SPO2 필수 필드 반영
개선: 작업자 앱 모듈 구조 분리
```

---

## 23. 최종 요약

`HNU_PPE_Control`은 Smart Shield 시스템의 작업자 전용 Android 애플리케이션이다.

이 앱은 ESP32 웨어러블 장치와 BLE로 연결되어 작업자의 체온 또는 피부 온도 관련 값, 심박수, 산소포화도, 주변 온도, 습도, 조도, 자세 상태를 실시간으로 수신한다.

수신된 데이터는 앱 내부에서 파싱되며, 온열질환 및 이상 상태 위험도 계산에 사용된다.

계산된 위험 단계는 작업자 앱 화면에 표시되며, 위험 상황에서는 팝업과 스마트폰 진동을 통해 작업자에게 즉시 알림을 제공한다.

또한 앱은 BLE Write를 통해 ESP32에 위험 단계 명령을 전송하여 LED, 진동 모터, 부저를 제어하고, Firebase Realtime Database에 작업자의 현재 상태와 위험 로그를 저장한다.

작업자 앱은 Smart Shield 시스템에서 센서 데이터 수신, 위험도 판단, 작업자 경고, ESP32 제어, Firebase 업로드를 담당하는 핵심 애플리케이션이다.
