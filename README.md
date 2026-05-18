
# Smart Shield

## 1. 프로젝트 개요

Smart Shield는 ESP32 기반 웨어러블 장치와 Android 앱을 BLE로 연결하여 건설 현장 작업자의 생체 신호, 환경 데이터, 자세 및 움직임 데이터를 수집하고, 온열질환 위험 가능성과 이상 상태를 조기에 감지하기 위한 PPE 안전 보조 시스템이다.

본 프로젝트는 작업자용 Android 앱, 관리자용 Android 앱, ESP32 웨어러블 장치, Firebase Realtime Database로 구성된다.

> 본 시스템은 의료기기가 아니며, 체온·심박수·산소포화도 값을 의료 진단 목적으로 사용하지 않는다.  
> 센서 데이터는 작업자의 위험 가능성을 조기에 감지하고 경고하기 위한 보조 지표로 사용한다.

---

## 2. 개발 목적

건설 현장과 같은 고온·고습 작업 환경에서는 작업자의 온열질환 위험, 낙상, 움직임 없음, 생체 신호 이상 등을 빠르게 감지하는 것이 중요하다.

Smart Shield는 다음 목적을 가진다.

- 작업자의 환경 데이터 수집
- 작업자의 생체 신호 변화 추적
- 작업자의 자세 및 움직임 상태 감지
- BLE 기반 실시간 데이터 전송
- Android 앱 기반 위험도 계산
- 위험 단계에 따른 작업자 경고
- Firebase 기반 관리자 모니터링

---

## 3. 전체 시스템 구성

```text
ESP32 웨어러블 장치
  ├─ 센서 데이터 수집
  ├─ BLE Notify로 작업자 앱에 데이터 전송
  └─ BLE Write 명령 수신 후 LED / 진동모터 / 부저 제어

Android 작업자 앱
  ├─ ESP32 BLE 연결
  ├─ 센서 payload 수신 및 파싱
  ├─ 위험도 계산
  ├─ 작업자 UI 표시
  ├─ 위험 단계에 따른 팝업 / 진동 처리
  ├─ ESP32로 위험도 명령 전송
  └─ Firebase에 현재 상태 및 위험 로그 업로드

Firebase Realtime Database
  ├─ 작업자 현재 상태 저장
  └─ 위험 로그 저장

Android 관리자 앱
  ├─ Firebase 데이터 읽기
  ├─ 전체 작업자 상태 모니터링
  ├─ 위험 작업자 우선 표시
  └─ 작업자 상세 상태 확인
````

---

## 4. 주요 기능

### 4-1. ESP32 웨어러블 장치

* BME280 기반 주변 온도, 습도, 기압 측정
* BH1750 기반 조도 측정
* MPU6050 기반 자세, 움직임, 낙상 가능성 추정
* MAX30102 / SEN0344 기반 심박수 및 산소포화도 추정
* MAX30205 / Fever Click 기반 피부 접촉 온도 변화 추적
* BLE Notify를 통한 센서 데이터 송신
* BLE Write 명령에 따른 RGB LED, 진동모터, 부저 제어

### 4-2. 작업자 앱

* ESP32 BLE 스캔 및 연결
* BLE Notify 수신
* 센서 payload 파싱
* 위험도 계산
* 현재 위험 상태 UI 표시
* 위험 단계별 팝업 및 스마트폰 진동
* Firebase 현재 상태 업로드
* Firebase 위험 로그 저장
* BLE 연결 끊김 시 재연결 처리

### 4-3. 관리자 앱

* Firebase 기반 작업자 목록 조회
* 작업자별 위험도 표시
* 응급 상태 작업자 우선 표시
* 작업자 상세 상태 확인
* 구역별 작업자 필터링
* 읽기 전용 모니터링

---

## 5. 폴더 구조

```text
HNU_Capstone-Design/
├─ README.md
├─ docs/
│  ├─ SYSTEM_ARCHITECTURE.md
│  ├─ BLE_PROTOCOL.md
│  ├─ FIREBASE_SCHEMA.md
│  ├─ RISK_ALGORITHM.md
│  ├─ TEST_PLAN.md
│  └─ FINAL_CHECKLIST.md
├─ HW/
│  ├─ README.md
│  ├─ SmartShield_ESP32/
│  │  ├─ README.md
│  │  └─ SmartShield_ESP32.ino
│  └─ TestCode/
│     ├─ README.md
│     ├─ Sensor/
│     ├─ Module/
│     ├─ Integration_Test_Code/
│     └─ Warning_Integration_Test_Code/
└─ SW/
   ├─ README.md
   ├─ HNU_PPE_Control/
   │  ├─ README.md
   │  └─ app/
   └─ HNU_PPE_Manager/
      ├─ README.md
      └─ app/
```

---

## 6. 하드웨어 구성

| 구분       | 부품                     | 역할                      |
| -------- | ---------------------- | ----------------------- |
| 메인 보드    | ESP32 DevKit           | 센서 수집, BLE 통신, 출력 장치 제어 |
| 환경 센서    | BME280                 | 주변 온도, 습도, 기압 측정        |
| 조도 센서    | BH1750 / GY-302        | 직사광선 노출 가능성 보조 판단       |
| 자세 센서    | MPU6050 / GY-521       | 자세 변화, 움직임, 낙상 가능성 판단   |
| 생체 센서    | MAX30102 / SEN0344     | 심박수, 산소포화도 추정           |
| 피부 온도 센서 | MAX30205 / Fever Click | 피부 접촉 온도 변화 추적          |
| 출력 장치    | RGB LED                | 위험 단계 시각 표시             |
| 출력 장치    | 진동모터                   | 작업자 촉각 경고               |
| 출력 장치    | Buzzer                 | 작업자 청각 경고               |

---

## 7. 소프트웨어 구성

| 앱               | 역할    |
| --------------- | ----- |
| HNU_PPE_Control | 작업자 앱 |
| HNU_PPE_Manager | 관리자 앱 |

### 작업자 앱

작업자 앱은 ESP32와 직접 BLE로 연결되는 앱이다. 센서 데이터를 수신하고, 위험도를 계산하며, Firebase에 작업자 상태를 업로드한다.

### 관리자 앱

관리자 앱은 Firebase 데이터를 읽어 작업자 상태를 모니터링하는 앱이다. BLE 연결, 위험도 계산, ESP32 제어는 수행하지 않는다.

---

## 8. BLE 통신 구조

Smart Shield는 BLE Notify / Write 구조를 사용한다.

```text
ESP32 → Android 작업자 앱
BLE Notify

Android 작업자 앱 → ESP32
BLE Write
```

### BLE 역할

| 장치            | BLE 역할                   |
| ------------- | ------------------------ |
| ESP32         | Peripheral / GATT Server |
| Android 작업자 앱 | Central / GATT Client    |

### BLE 이름 규칙

```text
SS_0001
```

### 작업자 ID 규칙

```text
BLE Device Name: SS_0001
Sensor Payload ID: 0001
Firebase workerId: 0001
```

### BLE UUID

| 항목                           | UUID                                   |
| ---------------------------- | -------------------------------------- |
| Service UUID                 | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Sensor Notify Characteristic | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Control Write Characteristic | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| CCCD                         | `00002902-0000-1000-8000-00805F9B34FB` |

### BLE Notify Payload 예시

```text
ID:0001,TEMP:36.5,HR:102,SPO2:97,ENV:33.1,HUM:71,LUX:45000,AX:0.12,AY:-0.08,AZ:9.78,POSTURE:NORMAL
```

### BLE Write 명령

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

---

## 9. Firebase 구조

Firebase Realtime Database는 작업자의 현재 상태와 위험 로그를 저장한다.

```text
workers/{workerId}/currentStatus
workers/{workerId}/riskLogs/{logId}
```

예시:

```text
workers/0001/currentStatus
workers/0001/riskLogs/{logId}
```

### 주요 저장 데이터

* 작업자 ID
* 작업 위치
* BLE 연결 상태
* BLE 신호 세기
* 센서 측정값
* 자세 상태
* 위험도 단계
* 위험도 명령
* 마지막 업데이트 시각
* 작업 세션 상태

---

## 10. 위험도 단계

Smart Shield는 위험도를 4단계로 구분한다.

| 단계 | 명령               | 의미           | 출력                   |
| -- | ---------------- | ------------ | -------------------- |
| 정상 | `RISK:SAFE`      | 위험 징후 없음     | LED 초록               |
| 주의 | `RISK:CAUTION`   | 위험 가능성 증가    | LED 노랑, 짧은 진동        |
| 위험 | `RISK:DANGER`    | 위험 상태 가능성 높음 | LED 빨강, 반복 진동        |
| 응급 | `RISK:EMERGENCY` | 즉시 확인 필요     | LED 빨강 점멸, 강한 진동, 부저 |

위험도는 단일 센서값 하나로 결정하지 않는다. 환경 데이터, 피부 접촉 온도 변화, 심박수 변화, 활동량, 자세 상태, SpO2 보조 플래그를 종합하여 판단한다.

---

## 11. 센서 데이터 해석 기준

본 프로젝트의 센서값은 의료 진단값이 아니라 안전 보조 판단값이다.

### TEMP

* MAX30205 기반 피부 접촉 온도
* 심부체온이 아님
* 절대값보다 baseline 대비 변화량과 지속 상승 추세를 사용

### HR

* 심박수 추정값
* 절대값 하나로 위험도를 단정하지 않음
* 개인 기준값 대비 상승량을 사용

### SPO2

* 산소포화도 추정값
* 열스트레스 핵심 지표가 아님
* 응급 상태 보조 플래그로만 사용

### ENV / HUM

* 주변 온도와 습도
* 온열질환 위험 가능성 판단의 환경 조건으로 사용

### LUX

* 조도값
* 직사광선 노출 가능성 판단의 보조 지표
* 복사열이나 체감온도를 직접 측정하지 않음

### POSTURE

* MPU6050 기반 자세 상태
* 낙상 가능성, 움직임 없음, 이상 자세 판단에 사용

---

## 12. 주의해야 할 표현

### 사용 가능한 표현

* 산업안전 보조 시스템
* 온열질환 위험 가능성 조기 감지
* 피부 접촉 온도 변화 추적
* 심박수 변화 기반 생리적 부담 추정
* IMU 기반 자세 이상 및 낙상 가능성 추정
* 다중 센서 융합 기반 위험도 판단

### 피해야 할 표현

* 정확한 체온 측정
* 의료급 산소포화도 측정
* 온열질환 진단
* 낙상 100% 감지
* SpO2 기반 열스트레스 판단
* HR > 120이면 무조건 위험
* TEMP > 37.5이면 무조건 위험

---

## 13. 실행 순서

### 13-1. ESP32 펌웨어 업로드

```text
HW/SmartShield_ESP32/SmartShield_ESP32.ino
```

Arduino IDE 또는 Arduino CLI를 사용하여 ESP32에 업로드한다.

### 13-2. 작업자 앱 실행

```text
SW/HNU_PPE_Control/
```

작업자 앱을 실행하고 ESP32 BLE 장치에 연결한다.

### 13-3. 작업 시작

작업자 앱에서 작업 위치를 선택하고 작업을 시작한다.

### 13-4. 관리자 앱 실행

```text
SW/HNU_PPE_Manager/
```

관리자 앱에서 Firebase에 업로드된 작업자 상태를 모니터링한다.

---

## 14. 검증 항목

최종 통합 전 다음 항목을 확인한다.

* ESP32 업로드 성공 여부
* I2C 센서 주소 인식 여부
* 각 센서 단독 측정 여부
* RGB LED 출력 여부
* 진동모터 동작 여부
* 부저 동작 여부
* BLE Notify 수신 여부
* BLE Write 명령 수신 여부
* 작업자 앱 UI 표시 여부
* Firebase 업로드 여부
* 관리자 앱 Firebase 읽기 여부
* 위험 단계별 출력 연동 여부
* BLE 끊김 및 재연결 여부

---

## 15. 관련 README

| 경로                               | 설명              |
| -------------------------------- | --------------- |
| `HW/README.md`                   | 하드웨어 전체 설명      |
| `HW/SmartShield_ESP32/README.md` | ESP32 최종 펌웨어 설명 |
| `HW/TestCode/README.md`          | 하드웨어 테스트 코드 설명  |
| `SW/README.md`                   | 소프트웨어 전체 설명     |
| `SW/HNU_PPE_Control/README.md`   | 작업자 앱 설명        |
| `SW/HNU_PPE_Manager/README.md`   | 관리자 앱 설명        |

---

## 16. 최종 요약

Smart Shield는 ESP32 웨어러블 장치와 Android 앱을 BLE로 연결하여 작업자의 환경 데이터, 생체 신호, 자세 및 움직임 데이터를 수집하고 위험 가능성을 판단하는 PPE 안전 보조 시스템이다.

ESP32는 센서값을 수집하여 작업자 앱으로 전송하고, 작업자 앱은 위험도를 계산하여 Firebase에 업로드하며, 관리자 앱은 Firebase 데이터를 읽어 작업자 상태를 모니터링한다.

본 프로젝트는 의료 진단 시스템이 아니라, 작업자의 온열질환 및 이상 상태 위험 가능성을 조기에 감지하고 경고하기 위한 산업안전 보조 시스템이다.

세부 설계 문서는 `docs/` 폴더에 정리되어 있으며, BLE 프로토콜, Firebase 구조, 위험도 알고리즘, 테스트 계획, 최종 체크리스트를 별도 문서로 관리한다.
