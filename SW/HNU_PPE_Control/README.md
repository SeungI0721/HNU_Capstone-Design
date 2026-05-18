
# HNU_PPE_Control

## 1. 앱 개요

`HNU_PPE_Control`은 Smart Shield 프로젝트의 작업자용 Android 앱이다.

이 앱은 ESP32 웨어러블 장치와 BLE로 연결하여 센서 데이터를 수신하고, 수신된 데이터를 기반으로 작업자의 위험도를 계산한다. 계산된 위험도는 앱 UI에 표시되며, BLE Write 명령으로 ESP32에 전달되어 RGB LED, 진동모터, 부저 출력 제어에 사용된다.

또한 작업자 상태와 위험 로그를 Firebase Realtime Database에 업로드하여 관리자 앱에서 모니터링할 수 있도록 한다.

> 본 앱은 의료 진단용 앱이 아니다.  
> 체온, 심박수, 산소포화도 값은 의료 진단값이 아니라 산업안전 보조 판단값으로 사용한다.

---

## 2. 주요 기능

```text
1. ESP32 BLE 장치 검색
2. ESP32 BLE 연결
3. BLE Notify 수신
4. 센서 payload 파싱
5. 위험도 계산
6. 현재 상태 UI 표시
7. 위험 단계별 팝업 표시
8. 스마트폰 진동 경고
9. ESP32로 BLE Write 명령 전송
10. Firebase 현재 상태 업로드
11. Firebase 위험 로그 저장
12. Foreground Service 기반 작업 세션 유지
13. BLE 연결 끊김 감지 및 재연결 처리
````

---

## 3. 앱 역할

작업자 앱은 Smart Shield 시스템에서 가장 많은 역할을 담당한다.

```text
ESP32 센서 데이터 수신
→ 센서 payload 파싱
→ 위험도 계산
→ 작업자 UI 표시
→ ESP32 출력 제어 명령 전송
→ Firebase 업로드
→ 관리자 앱에서 모니터링
```

관리자 앱은 Firebase 데이터를 읽기만 하며, 위험도 계산과 ESP32 제어는 이 작업자 앱에서 수행한다.

---

## 4. 프로젝트 구조

실제 파일 구조는 Android Studio 프로젝트 구조를 따른다.

```text
HNU_PPE_Control/
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
│        │  └─ com/example/hnu_ppe_control/
│        │     ├─ MainActivity.kt
│        │     ├─ MainUiController.kt
│        │     ├─ BleManager.kt
│        │     ├─ SensorPayloadParser.kt
│        │     ├─ HeatstrokeAnalyzer.kt
│        │     ├─ RiskCommandMapper.kt
│        │     ├─ FirebaseStatusRepository.kt
│        │     ├─ SmartShieldForegroundService.kt
│        │     └─ ...
│        └─ res/
│           ├─ layout/
│           ├─ drawable/
│           ├─ values/
│           └─ mipmap/
```

> 실제 파일명은 구현 상태에 따라 일부 다를 수 있다.
> README는 현재 앱 구조를 설명하기 위한 기준 문서이다.

---

## 5. 주요 파일 역할

| 파일                                | 역할                                           |
| --------------------------------- | -------------------------------------------- |
| `MainActivity.kt`                 | 작업자 앱의 중심 Activity, BLE 연결, UI 이벤트, 작업 세션 제어 |
| `MainUiController.kt`             | 메인 화면 UI 상태 업데이트                             |
| `BleManager.kt`                   | BLE 스캔, 연결, Notify 수신, Write 전송 처리           |
| `SensorPayloadParser.kt`          | ESP32에서 받은 문자열 payload 파싱                    |
| `HeatstrokeAnalyzer.kt`           | 위험도 계산                                       |
| `RiskCommandMapper.kt`            | 위험도 단계와 ESP32 명령 매핑                          |
| `FirebaseStatusRepository.kt`     | Firebase 현재 상태 및 위험 로그 업로드                   |
| `SmartShieldForegroundService.kt` | 작업 세션 중 백그라운드 동작 유지                          |
| `AndroidManifest.xml`             | 권한, Activity, Service 선언                     |
| `activity_main.xml`               | 메인 화면 레이아웃                                   |

---

## 6. 기술 스택

| 구분       | 내용                         |
| -------- | -------------------------- |
| 언어       | Kotlin                     |
| UI       | XML View                   |
| 구조       | Activity 중심 구조             |
| BLE      | Android Bluetooth LE GATT  |
| Database | Firebase Realtime Database |
| 빌드 시스템   | Gradle                     |
| IDE      | Android Studio             |

---

## 7. BLE 통신 구조

작업자 앱은 BLE Central / GATT Client로 동작한다.

ESP32는 BLE Peripheral / GATT Server로 동작한다.

```text
ESP32
→ BLE Notify
→ 작업자 앱
→ 위험도 계산
→ BLE Write
→ ESP32 출력 장치 제어
```

---

## 8. BLE UUID

작업자 앱과 ESP32 펌웨어는 동일한 UUID를 사용해야 한다.

| 항목                           | UUID                                   |
| ---------------------------- | -------------------------------------- |
| Service UUID                 | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Sensor Notify Characteristic | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Control Write Characteristic | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| CCCD                         | `00002902-0000-1000-8000-00805F9B34FB` |

---

## 9. BLE 장치 이름 규칙

ESP32 BLE 장치 이름은 다음 형식을 사용한다.

```text
SS_0001
```

작업자 ID는 BLE 장치 이름, payload ID, Firebase workerId에서 동일하게 유지한다.

```text
BLE Device Name: SS_0001
Payload ID: 0001
Firebase workerId: 0001
```

이 규칙을 유지해야 작업자 앱, ESP32, Firebase, 관리자 앱이 동일한 작업자를 기준으로 동작한다.

---

## 10. BLE Notify Payload

ESP32는 센서 데이터를 문자열 payload로 구성하여 작업자 앱으로 전송한다.

### Payload 예시

```text
ID:0001,TEMP:36.5,HR:102,SPO2:97,ENV:33.1,HUM:71,LUX:45000,AX:0.12,AY:-0.08,AZ:9.78,POSTURE:NORMAL
```

### 필드 설명

| 필드        | 의미        |
| --------- | --------- |
| `ID`      | 작업자 ID    |
| `TEMP`    | 피부 접촉 온도  |
| `HR`      | 심박수 추정값   |
| `SPO2`    | 산소포화도 추정값 |
| `ENV`     | 주변 온도     |
| `HUM`     | 주변 습도     |
| `LUX`     | 조도        |
| `AX`      | X축 가속도    |
| `AY`      | Y축 가속도    |
| `AZ`      | Z축 가속도    |
| `POSTURE` | 자세 상태     |

### POSTURE 값

```text
NORMAL
WARNING
UNSTABLE
FALL
EMERGENCY
```

---

## 11. BLE Write 명령

작업자 앱은 계산된 위험도에 따라 ESP32로 BLE Write 명령을 전송한다.

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

| 위험도       | 명령               |
| --------- | ---------------- |
| SAFE      | `RISK:SAFE`      |
| CAUTION   | `RISK:CAUTION`   |
| DANGER    | `RISK:DANGER`    |
| EMERGENCY | `RISK:EMERGENCY` |

`RISK:ERROR`는 공식 위험도 명령이 아니다.
필요하다면 앱 내부 예외 처리 또는 디버깅용 보조 상태로만 사용한다.

---

## 12. 작업 세션 흐름

작업자 앱의 기본 동작 흐름은 다음과 같다.

```text
앱 실행
→ 작업 시작 버튼 클릭
→ 작업 위치 선택
→ BLE 장치 검색
→ ESP32 연결
→ Notify 수신 시작
→ 센서 데이터 파싱
→ 위험도 계산
→ UI 업데이트
→ Firebase 업로드
→ ESP32로 위험도 명령 전송
→ 작업 종료 시 세션 종료
```

---

## 13. 작업 위치

작업자는 작업 시작 시 현재 작업 위치를 선택한다.

작업 위치는 코드와 이름을 함께 관리한다.

예시:

```text
workLocationCode: ZONE_A
workLocationName: A구역
```

Firebase에는 작업자 상태와 함께 작업 위치가 업로드된다.

관리자 앱은 작업 위치를 기준으로 작업자를 필터링하거나 상태를 확인할 수 있다.

---

## 14. 위험도 단계

Smart Shield의 위험도는 4단계이다.

| 단계 | 영문 값      | ESP32 명령         | 의미           |
| -- | --------- | ---------------- | ------------ |
| 정상 | SAFE      | `RISK:SAFE`      | 위험 징후 없음     |
| 주의 | CAUTION   | `RISK:CAUTION`   | 위험 가능성 증가    |
| 위험 | DANGER    | `RISK:DANGER`    | 위험 상태 가능성 높음 |
| 응급 | EMERGENCY | `RISK:EMERGENCY` | 즉시 확인 필요     |

---

## 15. 위험도 계산 기준

위험도 계산은 `HeatstrokeAnalyzer.kt`에서 수행한다.

위험도는 단일 센서값 하나로 결정하지 않는다.
환경 데이터, 피부 접촉 온도, 심박수, 활동량, 자세 상태, SpO2 보조 플래그를 종합하여 판단한다.

### 입력 데이터

| 데이터              | 역할               |
| ---------------- | ---------------- |
| `ENV`            | 주변 온도            |
| `HUM`            | 주변 습도            |
| `LUX`            | 직사광선 노출 가능성 보조   |
| `TEMP`           | 피부 접촉 온도         |
| `HR`             | 심박수              |
| `SPO2`           | 응급 보조 플래그        |
| `AX`, `AY`, `AZ` | 활동량 및 자세 판단      |
| `POSTURE`        | 자세 이상, 낙상 가능성 판단 |

---

## 16. 기준값 처리

현재 구현은 작업 시작 후 정상 상태에서 수신된 초기 데이터를 기준값으로 사용할 수 있다.

다만 실제 착용 환경에서는 센서 안정화 시간이 필요하다. 향후 개선 시에는 다음 방식이 권장된다.

```text
1. 장치 착용 및 BLE 연결 완료
2. 첫 30초는 센서 안정화 시간으로 제외
3. 이후 60~120초 동안 HR / TEMP 수집
4. MPU6050 기준 움직임이 적은 구간만 사용
5. 튀는 값 제거
6. 남은 값의 중앙값을 baselineHR, baselineTemp로 설정
```

### 권장 계산

```text
baselineHR = median(stable HR samples during pre-work period)
deltaHR = currentHR - baselineHR

baselineTemp = median(stable TEMP samples during pre-work period)
deltaTemp = currentTemp - baselineTemp
```

baselineHR은 의료적 안정시 심박수가 아니라, 당일 작업 전 기준 심박수이다.
baselineTemp 역시 심부체온 기준값이 아니라, 착용 상태에서의 피부 접촉 온도 기준값이다.

---

## 17. Heat Stress Risk

Heat Stress Risk는 온열질환 위험 가능성을 판단하기 위한 점수이다.

사용 데이터:

```text
ENV
HUM
LUX
TEMP
HR
ACTIVITY
```

판단 원칙:

```text
1. 주변 온도가 높을수록 위험 가능성 증가
2. 습도가 높을수록 위험 가능성 증가
3. 피부 접촉 온도가 baseline보다 지속적으로 상승하면 위험 가능성 증가
4. 심박수가 baseline보다 많이 상승하면 위험 가능성 증가
5. 활동량이 낮은데 심박수가 높으면 위험 가능성 증가
6. 활동량이 높은 상태의 심박 상승은 작업 강도 영향으로 일부 보정
7. 조도가 높으면 직사광선 노출 가능성으로 약하게 가중
```

나쁜 방식:

```text
HR > 120이면 무조건 위험
TEMP > 37.5이면 무조건 위험
```

좋은 방식:

```text
고온·고습 + 피부 접촉 온도 상승 + 활동량 대비 HR 과상승
→ 위험도 상승
```

---

## 18. Abnormal State Risk

Abnormal State Risk는 낙상, 움직임 없음, 생체 신호 급변, SpO2 저하 등을 판단하기 위한 점수이다.

사용 데이터:

```text
POSTURE
ACTIVITY
HR 급변
SPO2 저하 플래그
BLE 연결 상태
```

판단 원칙:

```text
1. 낙상 감지 시 즉시 위험도 상승
2. 일정 시간 이상 움직임이 없으면 이상 상태 후보
3. HR이 갑자기 급상승 또는 급저하하면 이상 상태 후보
4. SpO2는 단독 판단하지 않고 응급 보조 플래그로 사용
5. BLE 장기 끊김은 상태 확인 필요 플래그로 사용
```

예시:

```text
SpO2 저하 + 움직임 없음 + HR 이상
→ EMERGENCY 후보

SpO2만 낮고 손목 움직임이 심함
→ 측정 오류 가능성 우선
```

---

## 19. 센서값 신뢰도 처리

센서값은 항상 신뢰할 수 있는 값으로 처리하지 않는다.

### HR / SpO2 신뢰도 낮음 조건

```text
손목 밴드가 헐거움
움직임이 큼
HR이 갑자기 비정상적으로 튐
SpO2가 낮지만 움직임이 심함
센서 접촉 상태가 불안정함
외부광 영향이 큼
```

처리:

```text
HR / SpO2를 위험도 계산에서 제외하거나 가중치 감소
이전 안정값 유지
재측정 필요 상태 표시
```

---

### TEMP 신뢰도 낮음 조건

```text
피부 접촉 센서가 피부에서 떨어짐
값이 갑자기 외기온과 비슷해짐
ESP32 또는 배터리 열에 영향을 받음
접촉 상태가 불안정함
```

처리:

```text
TEMP 가중치 감소
상승 추세가 일정 시간 이상 지속될 때만 반영
```

---

### LUX 신뢰도 낮음 조건

```text
센서가 옷이나 팔에 가려짐
RGB LED 빛이 직접 들어감
장치 방향이 계속 바뀜
```

처리:

```text
LUX는 보조 점수로만 사용
LUX 단독 위험 판단 금지
```

---

## 20. 의료기기 아님

본 앱은 의료기기가 아니다.

따라서 다음 표현을 사용하지 않는다.

```text
정확한 체온 측정
의료급 산소포화도 측정
온열질환 진단
낙상 100% 감지
SpO2 기반 열스트레스 판단
HR > 120이면 무조건 위험
TEMP > 37.5이면 무조건 위험
```

권장 표현은 다음과 같다.

```text
산업안전 보조 시스템
온열질환 위험 가능성 조기 감지
피부 접촉 온도 변화 추적
심박수 변화 기반 생리적 부담 추정
IMU 기반 자세 이상 및 낙상 가능성 추정
다중 센서 융합 기반 위험도 판단
```

---

## 21. Firebase 구조

작업자 앱은 Firebase Realtime Database에 현재 상태와 위험 로그를 저장한다.

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

## 22. currentStatus 예시

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

## 23. riskLogs 예시

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

## 24. BLE 연결 및 재연결 정책

작업자 앱은 ESP32와 BLE GATT 연결을 유지한다.

연결이 끊기면 앱은 일정 시간 동안 자동 재연결을 시도한다.

권장 정책:

```text
1. BLE 연결 끊김 감지
2. UI에 연결 끊김 상태 표시
3. Firebase에 BLE 연결 상태 업로드
4. 마지막 연결 장치 정보를 기준으로 재연결 시도
5. 10분 이내 재연결을 목표로 반복 시도
6. 장시간 재연결 실패 시 관리자 확인 필요 상태로 표시
```

기록 권장 항목:

```text
BLE 연결 상태
RSSI
마지막 데이터 수신 시각
연결 끊김 횟수
재연결 횟수
재연결 성공 시간
```

---

## 25. BLE 신호 세기 표시

작업자 앱은 RSSI를 사람이 읽기 쉬운 단계로 표시한다.

예시:

| RSSI 기준    | 표시 |
| ---------- | -- |
| -60 dBm 이상 | 좋음 |
| -75 dBm 이상 | 보통 |
| -90 dBm 이상 | 약함 |
| 연결 끊김      | 끊김 |

RSSI 기준은 실제 테스트 결과에 따라 조정할 수 있다.

---

## 26. Foreground Service

작업 세션 중에는 `SmartShieldForegroundService`를 사용하여 앱의 작업 상태를 유지한다.

Foreground Service 사용 목적:

```text
BLE 연결 상태 유지
작업 세션 상태 유지
백그라운드 상태에서도 작업 중임을 표시
Android 시스템에 의해 앱이 임의 종료되는 상황 완화
```

Android 정책상 백그라운드 BLE 동작은 기기와 OS 버전에 따라 제한될 수 있다.

따라서 작업 시작 후에는 알림을 통해 서비스 실행 상태를 표시한다.

---

## 27. Android 권한

BLE 사용을 위해 Android 버전별 권한이 필요하다.

### Android 12 이상

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### Android 11 이하

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### Foreground Service

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

필요 시 Android 14 이상에서는 Foreground Service Type 관련 설정도 확인해야 한다.

---

## 28. Firebase 설정 파일

Firebase 연동을 위해 다음 파일이 필요하다.

```text
app/google-services.json
```

공개 저장소에 업로드할 경우 Firebase Database Rules와 API Key 제한 설정을 확인해야 한다.

---

## 29. 빌드 및 실행

### Android Studio

1. Android Studio에서 `HNU_PPE_Control` 프로젝트 열기
2. `app/google-services.json` 존재 여부 확인
3. Gradle Sync 실행
4. Android 기기 연결
5. 앱 빌드 및 실행
6. Bluetooth / 위치 권한 허용
7. ESP32 전원 ON
8. 앱에서 BLE 장치 검색
9. `SS_0001` 장치 연결
10. 작업 시작

---

## 30. Gradle 명령

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

## 31. 검증 체크리스트

### BLE

```text
BLE 권한이 정상 요청되는가?
ESP32 장치가 검색되는가?
SS_0001 이름이 표시되는가?
GATT 연결이 성공하는가?
Service UUID가 발견되는가?
Notify Characteristic이 발견되는가?
CCCD 설정 후 Notify가 수신되는가?
Write Characteristic에 명령 전송이 가능한가?
```

### Payload

```text
ID 필드가 파싱되는가?
TEMP 필드가 파싱되는가?
HR 필드가 파싱되는가?
SPO2 필드가 파싱되는가?
ENV 필드가 파싱되는가?
HUM 필드가 파싱되는가?
LUX 필드가 파싱되는가?
AX / AY / AZ 필드가 파싱되는가?
POSTURE 필드가 파싱되는가?
누락 필드가 있을 때 앱이 죽지 않는가?
비정상 숫자값이 들어와도 앱이 죽지 않는가?
```

### 위험도

```text
SAFE 상태가 정상 표시되는가?
CAUTION 상태가 정상 표시되는가?
DANGER 상태가 정상 표시되는가?
EMERGENCY 상태가 정상 표시되는가?
위험도에 따라 ESP32 명령이 정확히 전송되는가?
낙상 상태에서 EMERGENCY 후보로 처리되는가?
센서값 누락 시 fallback 또는 예외 처리가 되는가?
```

### Firebase

```text
currentStatus가 업로드되는가?
riskLogs가 생성되는가?
workerId가 BLE ID와 일치하는가?
workLocationCode가 저장되는가?
workLocationName이 저장되는가?
updatedAt이 갱신되는가?
관리자 앱에서 읽을 수 있는 구조인가?
```

### Foreground Service

```text
작업 시작 시 서비스가 시작되는가?
작업 종료 시 서비스가 종료되는가?
알림이 표시되는가?
앱을 백그라운드로 보내도 작업 상태가 유지되는가?
```

---

## 32. 위치별 BLE 안정성 테스트

BLE 성능은 휴대폰 위치와 인체 차폐 영향을 받는다.

테스트 조건:

| 조건           | 확인 항목       |
| ------------ | ----------- |
| 휴대폰 손에 든 상태  | 기준선 수신률     |
| 휴대폰 앞주머니     | 일반 사용 조건    |
| 휴대폰 뒷주머니     | 인체 차폐 영향    |
| 휴대폰 가방 안     | 장애물 영향      |
| 휴대폰 책상 위     | 기준선 성능      |
| ESP32와 같은 몸쪽 | 수신 안정성      |
| ESP32와 반대 몸쪽 | 몸통 차폐 영향    |
| 걷기           | 움직임 중 수신률   |
| 팔 흔들기        | 착용 위치 변화 영향 |

기록 항목:

```text
RSSI
BLE 연결 상태
Notify 수신 성공률
누락 패킷 수
연결 끊김 횟수
재연결 성공 시간
마지막 데이터 수신 시각
휴대폰 위치
작업자 자세
```

---

## 33. 제한사항

현재 앱은 캡스톤 구현을 위한 작업자용 앱이다.

제한사항:

```text
의료 진단 불가
센서값 절대 정확도 보장 불가
BLE 연결 안정성은 기기와 환경에 따라 달라짐
손목 PPG 값은 움직임에 취약함
피부 접촉 온도는 심부체온이 아님
조도값은 복사열이 아님
MPU6050만으로 낙상을 100% 확정할 수 없음
```

---

## 34. 관련 문서

| 경로                                     | 설명               |
| -------------------------------------- | ---------------- |
| `../../README.md`                      | 프로젝트 전체 README   |
| `../README.md`                         | SW 전체 README     |
| `../HNU_PPE_Manager/README.md`         | 관리자 앱 README     |
| `../../HW/README.md`                   | HW 전체 README     |
| `../../HW/SmartShield_ESP32/README.md` | ESP32 펌웨어 README |

---

## 35. 최종 요약

`HNU_PPE_Control`은 Smart Shield의 작업자용 Android 앱이다.

이 앱은 ESP32와 BLE로 연결하여 센서 payload를 수신하고, 위험도를 계산한 뒤 작업자 UI에 표시한다. 또한 계산된 위험도 명령을 ESP32로 전송하여 RGB LED, 진동모터, 부저를 제어하고, Firebase에 작업자 현재 상태와 위험 로그를 업로드한다.

위험도 계산은 의료 진단이 아니라 산업안전 보조 판단이며, 단일 센서값이 아니라 환경 데이터, 생체 신호 변화, 피부 접촉 온도 변화, 자세 상태, BLE 연결 상태를 종합하여 수행한다.
