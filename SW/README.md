
# Smart Shield SW

## 1. 폴더 개요

이 폴더는 Smart Shield 프로젝트의 Android 소프트웨어를 포함한다.

Smart Shield 소프트웨어는 작업자 앱과 관리자 앱으로 나뉜다.

- 작업자 앱: ESP32 웨어러블 장치와 BLE로 연결하여 센서 데이터를 수신하고 위험도를 계산한다.
- 관리자 앱: Firebase에 업로드된 작업자 상태를 읽어 전체 작업자 상태를 모니터링한다.

> 작업자 앱과 관리자 앱의 역할은 분리되어 있다.  
> 관리자 앱은 BLE 연결, 센서 데이터 파싱, 위험도 계산, ESP32 제어를 수행하지 않는다.

---

## 2. 폴더 구조

```text
SW/
├─ README.md
├─ HNU_PPE_Control/
│  ├─ README.md
│  └─ app/
│
└─ HNU_PPE_Manager/
   ├─ README.md
   └─ app/
````

---

## 3. 앱 구성

| 앱                 | 역할    |
| ----------------- | ----- |
| `HNU_PPE_Control` | 작업자 앱 |
| `HNU_PPE_Manager` | 관리자 앱 |

---

## 4. 작업자 앱

작업자 앱은 실제 작업자가 사용하는 앱이다.

ESP32 웨어러블 장치와 BLE로 연결하고, 센서 payload를 수신하여 위험도를 계산한다. 계산된 위험도는 앱 UI에 표시되고, ESP32로 BLE Write 명령을 전송하며, Firebase에 작업자 상태와 위험 로그를 업로드한다.

### 주요 기능

* ESP32 BLE 스캔
* ESP32 BLE 연결
* BLE Notify 수신
* 센서 payload 파싱
* 위험도 계산
* 작업자 현재 상태 UI 표시
* 위험 단계별 팝업 표시
* 스마트폰 진동 경고
* BLE Write로 ESP32 출력 장치 제어
* Firebase 현재 상태 업로드
* Firebase 위험 로그 저장
* BLE 연결 끊김 감지
* BLE 재연결 처리
* 작업 세션 관리

### 위치

```text
SW/HNU_PPE_Control/
```

---

## 5. 관리자 앱

관리자 앱은 관리자가 작업자 상태를 확인하기 위한 모니터링 앱이다.

Firebase Realtime Database에 저장된 작업자 상태를 읽어 위험도, 작업 위치, BLE 연결 상태, 센서값, 마지막 업데이트 시각 등을 표시한다.

관리자 앱은 읽기 전용 모니터링을 기준으로 한다.

### 주요 기능

* Firebase 작업자 목록 조회
* 작업자 현재 상태 표시
* 위험도별 작업자 구분
* 응급 상태 작업자 우선 표시
* 구역별 작업자 필터링
* 작업자 상세 정보 표시
* 위험 로그 확인
* BLE 연결 상태 표시

### 위치

```text
SW/HNU_PPE_Manager/
```

---

## 6. 앱 책임 분리

| 기능                 | 작업자 앱 | 관리자 앱 |
| ------------------ | ----- | ----- |
| BLE 스캔             | O     | X     |
| BLE 연결             | O     | X     |
| BLE Notify 수신      | O     | X     |
| 센서 payload 파싱      | O     | X     |
| 위험도 계산             | O     | X     |
| ESP32 제어 명령 전송     | O     | X     |
| 스마트폰 진동 경고         | O     | X     |
| Firebase 현재 상태 업로드 | O     | X     |
| Firebase 위험 로그 저장  | O     | X     |
| Firebase 데이터 읽기    | O     | O     |
| 전체 작업자 모니터링        | X     | O     |
| 응급 작업자 우선 표시       | X     | O     |
| 작업자 상세 상태 확인       | O     | O     |

---

## 7. 기술 스택

| 구분     | 내용                         |
| ------ | -------------------------- |
| 언어     | Kotlin                     |
| UI     | XML View                   |
| 구조     | Activity 중심 구조             |
| BLE    | Android Bluetooth LE GATT  |
| DB     | Firebase Realtime Database |
| 빌드 시스템 | Gradle                     |
| IDE    | Android Studio             |

---

## 8. Android 프로젝트 기준

두 앱은 Android Studio 프로젝트로 관리된다.

기본 개발 기준은 다음과 같다.

```text
Language: Kotlin
UI: XML Layout
Architecture: Activity-centered
Database: Firebase Realtime Database
BLE: Android BLE GATT API
```

작업자 앱과 관리자 앱은 별도 Android 프로젝트로 관리한다.

---

## 9. BLE 통신 구조

BLE 통신은 작업자 앱에서만 수행한다.

```text
ESP32
→ BLE Notify
→ 작업자 앱
→ 위험도 계산
→ BLE Write
→ ESP32 출력 장치 제어
```

관리자 앱은 ESP32와 BLE로 연결하지 않는다.

---

## 10. BLE UUID

작업자 앱과 ESP32는 동일한 UUID를 사용해야 한다.

| 항목                           | UUID                                   |
| ---------------------------- | -------------------------------------- |
| Service UUID                 | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Sensor Notify Characteristic | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Control Write Characteristic | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| CCCD                         | `00002902-0000-1000-8000-00805F9B34FB` |

---

## 11. BLE Payload 예시

작업자 앱은 ESP32에서 다음 형식의 payload를 수신한다.

```text
ID:0001,TEMP:36.5,HR:102,SPO2:97,ENV:33.1,HUM:71,LUX:45000,AX:0.12,AY:-0.08,AZ:9.78,POSTURE:NORMAL
```

---

## 12. BLE Write 명령

작업자 앱은 계산된 위험도에 따라 ESP32로 다음 명령을 전송한다.

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

| 명령               | 의미 |
| ---------------- | -- |
| `RISK:SAFE`      | 정상 |
| `RISK:CAUTION`   | 주의 |
| `RISK:DANGER`    | 위험 |
| `RISK:EMERGENCY` | 응급 |

---

## 13. Firebase 구조

작업자 앱은 Firebase에 현재 상태와 위험 로그를 업로드한다.

관리자 앱은 해당 데이터를 읽어 모니터링한다.

```text
workers/{workerId}/currentStatus
workers/{workerId}/riskLogs/{logId}
```

예시:

```text
workers/0001/currentStatus
workers/0001/riskLogs/{logId}
```

---

## 14. 작업자 ID 규칙

작업자 ID는 BLE 이름, payload ID, Firebase 경로에서 동일하게 사용한다.

```text
BLE Device Name: SS_0001
Payload ID: 0001
Firebase workerId: 0001
```

이 규칙을 유지해야 작업자 앱, 관리자 앱, Firebase 데이터가 같은 작업자를 기준으로 연결된다.

---

## 15. 위험도 단계

Smart Shield는 위험도를 4단계로 구분한다.

| 단계 | 영문 값      | ESP32 명령         | 설명           |
| -- | --------- | ---------------- | ------------ |
| 정상 | SAFE      | `RISK:SAFE`      | 위험 징후 없음     |
| 주의 | CAUTION   | `RISK:CAUTION`   | 위험 가능성 증가    |
| 위험 | DANGER    | `RISK:DANGER`    | 위험 상태 가능성 높음 |
| 응급 | EMERGENCY | `RISK:EMERGENCY` | 즉시 확인 필요     |

위험도는 작업자 앱에서 계산한다.

관리자 앱은 Firebase에 저장된 위험도 값을 읽어 표시한다.

---

## 16. 센서 데이터 해석 기준

본 프로젝트는 의료기기가 아니다.

따라서 센서값은 의료 진단이 아니라 산업안전 보조 판단에 사용한다.

| 데이터              | 해석 기준               |
| ---------------- | ------------------- |
| `TEMP`           | 피부 접촉 온도 변화 추적      |
| `HR`             | 심박수 변화 기반 생리적 부담 추정 |
| `SPO2`           | 응급 상태 보조 플래그        |
| `ENV`            | 주변 온도               |
| `HUM`            | 주변 습도               |
| `LUX`            | 직사광선 노출 가능성 보조 지표   |
| `AX`, `AY`, `AZ` | 활동량 및 자세 판단         |
| `POSTURE`        | 자세 이상, 낙상 가능성 판단    |

---

## 17. 주의해야 할 표현

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

## 18. Firebase 설정 파일

각 Android 앱은 Firebase 연동을 위해 `google-services.json` 파일이 필요하다.

일반적인 위치는 다음과 같다.

```text
SW/HNU_PPE_Control/app/google-services.json
SW/HNU_PPE_Manager/app/google-services.json
```

공개 저장소에 업로드할 경우 Firebase Database Rules와 API Key 제한 설정을 확인해야 한다.

---

## 19. 빌드 산출물 관리

Android 빌드 산출물은 Git에 포함하지 않는 것을 권장한다.

제외 권장 경로:

```text
SW/HNU_PPE_Control/app/build/
SW/HNU_PPE_Manager/app/build/
SW/HNU_PPE_Control/build/
SW/HNU_PPE_Manager/build/
```

`.gitignore`에 다음 항목이 포함되어 있는지 확인한다.

```gitignore
.gradle/
build/
**/build/
local.properties
*.iml
captures/
.externalNativeBuild/
.cxx/
.DS_Store
```

빌드 산출물까지 Git에 넣는 건 저장소를 무겁게 만들 뿐이다. 굳이 코드 저장소에 디지털 먼지를 보관할 이유는 없다.

---

## 20. 실행 순서

### 20-1. 작업자 앱

```text
SW/HNU_PPE_Control/
```

1. Android Studio에서 프로젝트 열기
2. Firebase 설정 파일 확인
3. 앱 빌드
4. Android 기기에서 실행
5. Bluetooth 및 위치 권한 허용
6. ESP32 BLE 장치 연결
7. 작업 시작

### 20-2. 관리자 앱

```text
SW/HNU_PPE_Manager/
```

1. Android Studio에서 프로젝트 열기
2. Firebase 설정 파일 확인
3. 앱 빌드
4. Android 기기에서 실행
5. Firebase 작업자 상태 확인

---

## 21. 검증 항목

### 작업자 앱 검증

* BLE 스캔 가능 여부
* ESP32 연결 가능 여부
* BLE Notify 수신 여부
* payload 파싱 여부
* 위험도 계산 여부
* BLE Write 명령 전송 여부
* Firebase 현재 상태 업로드 여부
* Firebase 위험 로그 저장 여부
* Foreground Service 동작 여부
* BLE 끊김 및 재연결 처리 여부

### 관리자 앱 검증

* Firebase 작업자 목록 읽기 여부
* 작업자 위험도 표시 여부
* 응급 작업자 우선 표시 여부
* 구역별 필터 동작 여부
* 작업자 상세 화면 표시 여부
* 마지막 업데이트 시간 표시 여부

---

## 22. 관련 문서

| 경로                                  | 설명             |
| ----------------------------------- | -------------- |
| `../README.md`                      | 프로젝트 전체 README |
| `../HW/README.md`                   | 하드웨어 전체 설명     |
| `../HW/SmartShield_ESP32/README.md` | ESP32 펌웨어 설명   |
| `HNU_PPE_Control/README.md`         | 작업자 앱 설명       |
| `HNU_PPE_Manager/README.md`         | 관리자 앱 설명       |

---

## 23. 최종 요약

`SW` 폴더는 Smart Shield의 Android 소프트웨어를 관리한다.

작업자 앱인 `HNU_PPE_Control`은 ESP32와 BLE로 연결하여 센서 데이터를 수신하고, 위험도를 계산한 뒤 Firebase에 작업자 상태를 업로드한다.

관리자 앱인 `HNU_PPE_Manager`는 Firebase 데이터를 읽어 전체 작업자 상태를 모니터링한다.

위험도 계산과 ESP32 제어는 작업자 앱에서만 수행하며, 관리자 앱은 읽기 전용 모니터링 앱으로 동작한다.
