
# HNU_PPE_Manager

## 1. 앱 개요

`HNU_PPE_Manager`는 Smart Shield 프로젝트의 관리자용 Android 앱이다.

이 앱은 Firebase Realtime Database에 업로드된 작업자 상태를 읽어 전체 작업자의 위험도, 작업 위치, BLE 연결 상태, 센서값, 마지막 업데이트 시각을 모니터링한다.

> 관리자 앱은 읽기 전용 모니터링 앱이다.  
> BLE 연결, 센서 payload 파싱, 위험도 계산, ESP32 제어 명령 전송은 수행하지 않는다.

---

## 2. 주요 기능

```text
1. Firebase 작업자 목록 조회
2. 작업자 현재 상태 표시
3. 위험도별 작업자 구분
4. 응급 상태 작업자 우선 표시
5. 구역별 작업자 필터링
6. 작업자 상세 상태 확인
7. 위험 로그 확인
8. BLE 연결 상태 표시
9. 마지막 업데이트 시간 표시
````

---

## 3. 앱 역할

관리자 앱은 작업자 앱이 Firebase에 업로드한 데이터를 읽어 표시한다.

```text
ESP32
→ 작업자 앱
→ Firebase 업로드
→ 관리자 앱에서 Firebase 읽기
→ 관리자 화면 표시
```

관리자 앱은 작업자 상태를 수정하지 않는다.

---

## 4. 프로젝트 구조

```text
HNU_PPE_Manager/
├─ README.md
├─ build.gradle.kts
├─ settings.gradle.kts
├─ app/
│  ├─ build.gradle.kts
│  ├─ google-services.json
│  └─ src/
│     └─ main/
│        ├─ AndroidManifest.xml
│        ├─ java/
│        │  └─ com/example/hnu_ppe_manager/
│        │     ├─ MainActivity.kt
│        │     ├─ WorkerDetailActivity.kt
│        │     ├─ WorkerStatusRepository.kt
│        │     ├─ WorkerStatus.kt
│        │     └─ ...
│        └─ res/
│           ├─ layout/
│           ├─ drawable/
│           ├─ values/
│           └─ mipmap/
```

> 실제 파일명은 구현 상태에 따라 일부 다를 수 있다.
> README는 관리자 앱 구조를 설명하기 위한 기준 문서이다.

---

## 5. 주요 파일 역할

| 파일                           | 역할                       |
| ---------------------------- | ------------------------ |
| `MainActivity.kt`            | 작업자 목록, 위험도 요약, 구역 필터 표시 |
| `WorkerDetailActivity.kt`    | 선택한 작업자의 상세 상태 표시        |
| `WorkerStatusRepository.kt`  | Firebase 작업자 상태 읽기       |
| `WorkerStatus.kt`            | 작업자 상태 데이터 모델            |
| `AndroidManifest.xml`        | Activity 및 앱 설정          |
| `activity_main.xml`          | 관리자 메인 화면                |
| `activity_worker_detail.xml` | 작업자 상세 화면                |

---

## 6. 기술 스택

| 구분     | 내용                         |
| ------ | -------------------------- |
| 언어     | Kotlin                     |
| UI     | XML View                   |
| 구조     | Activity 중심 구조             |
| DB     | Firebase Realtime Database |
| 빌드 시스템 | Gradle                     |
| IDE    | Android Studio             |

---

## 7. 데이터 처리 원칙

관리자 앱은 Firebase 데이터를 읽기 전용으로 조회한다.

관리자 앱에서 수행하지 않는 기능은 다음과 같다.

```text
BLE 스캔
BLE 연결
BLE Notify 수신
센서 payload 파싱
위험도 계산
ESP32 제어 명령 전송
Firebase 데이터 수정
```

관리자 앱에서 표시하는 위험도는 작업자 앱이 계산하여 Firebase에 업로드한 `riskLevel` 값을 기준으로 한다.

---

## 8. Firebase 구조

관리자 앱은 다음 Firebase 경로를 읽는다.

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

## 9. currentStatus 예시

```json
{
  "workerId": "0001",
  "deviceName": "SS_0001",
  "workLocationCode": "ZONE_A",
  "workLocationName": "A구역",
  "temp": 36.5,
  "hr": 102,
  "spo2": 97,
  "env": 33.1,
  "hum": 71,
  "lux": 45000,
  "posture": "NORMAL",
  "riskLevel": "CAUTION",
  "riskCommand": "RISK:CAUTION",
  "bleConnected": true,
  "bleSignalLevel": "GOOD",
  "bleRssi": -58,
  "appSessionActive": true,
  "updatedAt": 1710000000000
}
```

---

## 10. riskLogs 예시

```json
{
  "workerId": "0001",
  "riskLevel": "DANGER",
  "riskCommand": "RISK:DANGER",
  "message": "고온·고습 환경과 심박수 상승이 함께 감지되었습니다.",
  "temp": 37.2,
  "hr": 118,
  "spo2": 96,
  "env": 35.1,
  "hum": 78,
  "lux": 52000,
  "posture": "NORMAL",
  "workLocationCode": "ZONE_A",
  "workLocationName": "A구역",
  "createdAt": 1710000000000
}
```

---

## 11. 작업자 ID 규칙

작업자 ID는 BLE 이름, payload ID, Firebase 경로에서 동일하게 사용한다.

```text
BLE Device Name: SS_0001
Payload ID: 0001
Firebase workerId: 0001
```

관리자 앱은 Firebase의 `workerId`를 기준으로 작업자를 구분한다.

---

## 12. 위험도 단계

관리자 앱은 작업자 앱이 업로드한 위험도 값을 표시한다.

| 단계 | 영문 값      | 의미           |
| -- | --------- | ------------ |
| 정상 | SAFE      | 위험 징후 없음     |
| 주의 | CAUTION   | 위험 가능성 증가    |
| 위험 | DANGER    | 위험 상태 가능성 높음 |
| 응급 | EMERGENCY | 즉시 확인 필요     |

관리자 앱은 위험도 계산을 직접 수행하지 않는다.

---

## 13. 표시 항목

관리자 앱은 작업자별로 다음 정보를 표시한다.

```text
작업자 ID
작업 위치
위험도 단계
BLE 연결 상태
BLE 신호 세기
피부 접촉 온도
심박수
산소포화도
주변 온도
주변 습도
조도
자세 상태
마지막 업데이트 시각
작업 세션 상태
```

---

## 14. 메인 화면 기능

메인 화면에서는 전체 작업자의 상태를 요약해서 보여준다.

권장 표시 항목:

```text
전체 작업자 수
정상 작업자 수
주의 작업자 수
위험 작업자 수
응급 작업자 수
BLE 연결 끊김 작업자 수
구역별 작업자 목록
```

작업자는 위험도가 높은 순서로 표시하는 것을 권장한다.

권장 정렬 순서:

```text
EMERGENCY
DANGER
CAUTION
SAFE
연결 끊김 또는 데이터 없음
```

---

## 15. 상세 화면 기능

작업자를 선택하면 상세 화면에서 더 많은 정보를 확인한다.

권장 표시 항목:

```text
작업자 ID
장치 이름
작업 위치 코드
작업 위치 이름
현재 위험도
위험도 명령
피부 접촉 온도
심박수
산소포화도
주변 온도
주변 습도
조도
자세 상태
BLE 연결 여부
BLE RSSI
BLE 신호 단계
마지막 업데이트 시각
최근 위험 로그
```

---

## 16. 구역 필터

관리자 앱은 작업 위치를 기준으로 작업자를 필터링할 수 있다.

예시:

```text
전체
A구역
B구역
C구역
실내
실외
```

실제 구역 이름은 작업자 앱에서 업로드하는 `workLocationCode`, `workLocationName` 값을 기준으로 한다.

---

## 17. 갱신 정책

Firebase 갱신 방식은 구현 상태에 따라 달라질 수 있다.

권장 정책:

```text
전체 작업자 목록: 15초 주기 갱신 또는 Firebase 리스너 사용
응급 상태 작업자: 실시간 리스너 사용 권장
작업자 상세 화면: 진입 시 즉시 조회 후 주기 갱신
```

전체 작업자 목록은 Firebase 부하를 줄이기 위해 일정 주기로 갱신할 수 있다.

응급 상태 작업자는 빠른 대응이 필요하므로 별도의 실시간 리스너로 감지하는 것이 좋다.

---

## 18. BLE 상태 표시

관리자 앱은 작업자 앱이 Firebase에 업로드한 BLE 상태를 표시한다.

| 값                              | 의미        |
| ------------------------------ | --------- |
| `bleConnected: true`           | BLE 연결 중  |
| `bleConnected: false`          | BLE 연결 끊김 |
| `bleSignalLevel: GOOD`         | 신호 좋음     |
| `bleSignalLevel: NORMAL`       | 신호 보통     |
| `bleSignalLevel: WEAK`         | 신호 약함     |
| `bleSignalLevel: DISCONNECTED` | 연결 끊김     |

관리자 앱은 BLE RSSI를 직접 측정하지 않는다.
RSSI 값은 작업자 앱이 측정하여 Firebase에 업로드한다.

---

## 19. 알림 및 대응 기준

관리자 앱에서 위험도 표시 시 다음 기준을 적용할 수 있다.

| 상태        | 관리자 확인 우선순위          |
| --------- | -------------------- |
| EMERGENCY | 즉시 확인                |
| DANGER    | 빠른 확인                |
| CAUTION   | 상태 관찰                |
| SAFE      | 일반 모니터링              |
| 연결 끊김     | 작업자 위치 및 장치 상태 확인 필요 |

연결 끊김이 오래 지속되면 작업자 상태를 확인해야 한다.

---

## 20. 의료기기 아님

관리자 앱은 작업자의 상태를 의료적으로 진단하지 않는다.

피해야 할 표현:

```text
온열질환 진단
정확한 체온 측정
의료급 산소포화도 측정
낙상 확정
SpO2 기반 열스트레스 판단
```

권장 표현:

```text
온열질환 위험 가능성
이상 상태 가능성
작업자 상태 확인 필요
피부 접촉 온도 변화
심박수 변화
자세 이상 가능성
```

---

## 21. Firebase 설정 파일

Firebase 연동을 위해 다음 파일이 필요하다.

```text
app/google-services.json
```

공개 저장소에 업로드할 경우 Firebase Database Rules와 API Key 제한 설정을 확인해야 한다. 인간은 보안 규칙을 나중에 고치려다 대체로 더 큰 일을 만든다.

---

## 22. 빌드 및 실행

### Android Studio

1. Android Studio에서 `HNU_PPE_Manager` 프로젝트 열기
2. `app/google-services.json` 존재 여부 확인
3. Gradle Sync 실행
4. Android 기기 연결
5. 앱 빌드 및 실행
6. Firebase 데이터 표시 확인

---

## 23. Gradle 명령

Windows 기준:

```bash
gradlew.bat assembleDebug
```

macOS / Linux 기준:

```bash
./gradlew assembleDebug
```

테스트:

```bash
gradlew.bat test
```

---

## 24. 검증 체크리스트

### Firebase

```text
Firebase 연결이 정상인가?
workers 경로를 읽을 수 있는가?
currentStatus 값을 읽을 수 있는가?
riskLogs 값을 읽을 수 있는가?
workerId가 정상 표시되는가?
workLocationCode / workLocationName이 정상 표시되는가?
riskLevel이 정상 표시되는가?
updatedAt이 정상 표시되는가?
```

### 메인 화면

```text
작업자 목록이 표시되는가?
위험도별 상태가 구분되는가?
EMERGENCY 작업자가 우선 표시되는가?
구역 필터가 정상 동작하는가?
BLE 연결 끊김 상태가 표시되는가?
마지막 업데이트 시간이 표시되는가?
```

### 상세 화면

```text
작업자 상세 정보가 표시되는가?
센서값이 정상 표시되는가?
위험 로그가 표시되는가?
BLE 상태가 표시되는가?
작업 위치가 표시되는가?
```

---

## 25. 제한사항

관리자 앱의 제한사항은 다음과 같다.

```text
BLE 장치와 직접 연결하지 않음
위험도 계산을 직접 수행하지 않음
ESP32 제어 명령을 직접 전송하지 않음
Firebase에 잘못된 데이터가 올라오면 그대로 표시될 수 있음
작업자 앱이 업로드하지 않은 값은 표시할 수 없음
네트워크 연결 상태에 따라 데이터 갱신이 지연될 수 있음
```

---

## 26. 관련 문서

| 경로                                     | 설명               |
| -------------------------------------- | ---------------- |
| `../../README.md`                      | 프로젝트 전체 README   |
| `../README.md`                         | SW 전체 README     |
| `../HNU_PPE_Control/README.md`         | 작업자 앱 README     |
| `../../HW/README.md`                   | HW 전체 README     |
| `../../HW/SmartShield_ESP32/README.md` | ESP32 펌웨어 README |

---

## 27. 최종 요약

`HNU_PPE_Manager`는 Smart Shield의 관리자용 Android 앱이다.

이 앱은 Firebase Realtime Database에 저장된 작업자 현재 상태와 위험 로그를 읽어 관리자에게 표시한다.

관리자 앱은 BLE 연결, 센서 payload 파싱, 위험도 계산, ESP32 제어를 수행하지 않으며, 작업자 앱이 Firebase에 업로드한 데이터를 읽기 전용으로 모니터링한다.
