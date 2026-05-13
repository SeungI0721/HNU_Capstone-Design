
# Smart Shield 작업자 앱

Smart Shield 작업자 앱은 ESP32 기반 웨어러블 안전 장치와 BLE로 연결되어 작업자의 센서 데이터를 수신하고, 앱 내부에서 위험도를 계산한 뒤 UI 표시, Firebase 업로드, ESP32 위험 명령 전송, 작업자 알림을 수행하는 Android 앱입니다.

본 앱은 관리자 앱이 아니라 작업자 앱입니다.  

---

## 1. 개발 환경

- Language: Kotlin
- UI: XML 기반 Android View
- Android Gradle Plugin: 8.13.2
- Kotlin Plugin: 2.0.21
- Google Services Plugin: 4.4.4
- compileSdk: 36
- targetSdk: 36
- minSdk: 26
- Java Version: 17
- Kotlin JVM Target: 17

---

## 2. 주요 의존성

```kotlin
implementation("androidx.core:core-ktx:1.15.0")
implementation("androidx.appcompat:appcompat:1.7.0")
implementation("com.google.android.material:material:1.12.0")
implementation("androidx.constraintlayout:constraintlayout:2.2.0")
implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
implementation("com.google.firebase:firebase-database")
````

---

## 3. 앱 목적

Smart Shield 작업자 앱은 건설현장 작업자의 생체 데이터, 환경 데이터, 자세 상태를 수신하여 온열질환 위험 가능성과 이상 상태를 조기에 감지하기 위한 산업안전 보조 앱입니다.

이 앱은 의료 진단 앱이 아니며, 센서값을 의료적 진단값으로 사용하지 않습니다.

---

## 4. 전체 동작 흐름

```text
ESP32 센서값 수집
→ BLE Notify
→ Android 작업자 앱 수신
→ 센서 데이터 파싱
→ 위험도 계산
→ UI 표시
→ Firebase 업로드
→ BLE Write로 ESP32에 위험 명령 전송
→ 작업자 팝업 및 스마트폰 진동 알림
```

---

## 5. BLE 통신 구조

ESP32는 BLE Peripheral 및 GATT Server로 동작하고, Android 작업자 앱은 BLE Central 및 GATT Client로 동작합니다.

### BLE 장치 이름 규칙

```text
SS_0001
```

### BLE UUID

| 항목                           | UUID                                   |
| ---------------------------- | -------------------------------------- |
| Service UUID                 | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Sensor Notify Characteristic | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Control Write Characteristic | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| CCCD                         | `00002902-0000-1000-8000-00805F9B34FB` |

---

## 6. 센서 데이터 Payload 형식

ESP32에서 앱으로 전달하는 BLE Notify payload 예시는 다음과 같습니다.

```text
ID:0001,TEMP:36.5,HR:102,SPO2:97,ENV:33.1,HUM:71,LUX:45000,POSTURE:NORMAL
```

### 필드 설명

| 필드      | 의미        |
| ------- | --------- |
| ID      | 작업자 ID    |
| TEMP    | 피부 접촉 온도  |
| HR      | 심박수       |
| SPO2    | 산소포화도 추정값 |
| ENV     | 주변 온도     |
| HUM     | 습도        |
| LUX     | 조도        |
| POSTURE | 자세 상태     |

### POSTURE 값

```text
NORMAL
WARNING
UNSTABLE
EMERGENCY
FALL
```

---

## 7. 위험도 단계

앱은 센서 데이터를 기반으로 위험도를 4단계로 구분합니다.

| 단계        | 한글 표시 | ESP32 명령         |
| --------- | ----- | ---------------- |
| SAFE      | 정상    | `RISK:SAFE`      |
| CAUTION   | 주의    | `RISK:CAUTION`   |
| DANGER    | 위험    | `RISK:DANGER`    |
| EMERGENCY | 응급    | `RISK:EMERGENCY` |

---

## 8. 위험도 계산 방식

현재 앱은 초기 구현 단계로 rule-based 방식의 위험도 계산을 사용합니다.

계산에 사용하는 주요 값은 다음과 같습니다.

* 피부 접촉 온도
* 심박수
* 산소포화도 추정값
* 주변 온도
* 습도
* 조도
* 자세 상태

---

## 9. Firebase 데이터 구조

작업자 앱은 Firebase Realtime Database에 현재 상태와 위험 로그를 업로드합니다.

### 현재 상태

```text
workers/{workerId}/currentStatus
```

`currentStatus`는 관리자 앱이 실시간으로 읽는 덮어쓰기 경로입니다.

저장 항목 예시:

```text
workerId
deviceName
temp
hr
spo2
env
hum
lux
directSunlight
posture
riskLevel
riskCommand
bleConnected
appSessionActive
updatedAt
```

### 위험 로그

```text
workers/{workerId}/riskLogs/{logId}
```

`riskLogs`는 위험 또는 응급 이벤트를 누적 저장하는 경로입니다.

위험 로그는 `DANGER` 또는 `EMERGENCY` 상태에서만 저장되며, 같은 위험 단계가 반복될 경우 중복 저장을 방지합니다.

---

## 10. 사용자 알림

위험 단계에 따라 앱은 작업자에게 팝업과 스마트폰 진동으로 알림을 제공합니다.

| 단계        | 알림            |
| --------- | ------------- |
| SAFE      | 알림 없음         |
| CAUTION   | 주의 팝업 + 짧은 진동 |
| DANGER    | 위험 팝업 + 중간 진동 |
| EMERGENCY | 응급 팝업 + 긴 진동  |

같은 위험 단계에서는 팝업과 진동이 반복되지 않도록 중복 알림을 방지합니다.

---

## 11. 백그라운드 동작

앱은 Foreground Service를 사용하여 백그라운드에서도 BLE 센서 수신과 Firebase 업로드 상태를 유지합니다.

Foreground Service 알림:

```text
Smart Shield 실행 중
BLE 센서 수신과 Firebase 업로드를 유지합니다.
```

---

## 12. 재연결 정책

BLE 연결이 비정상적으로 끊어진 경우 앱은 자동 재연결을 시도합니다.

* 재연결 주기: 3초
* 최대 재연결 시간: 10분
* 10분 초과 시 세션 종료
* 수동 연결 해제 시 재연결하지 않음

---

## 13. 데이터 수신 상태 감시

앱은 마지막 데이터 수신 시각을 기준으로 BLE 데이터 상태를 판단합니다.

| 조건            | 상태         |
| ------------- | ---------- |
| 정상 수신 중       | 데이터 수신 중   |
| 10초 이상 데이터 없음 | 데이터 수신 불안정 |
| 30초 이상 데이터 없음 | 데이터 오프라인   |

---

## 14. 주요 파일 구조

```text
MainActivity.kt
├─ 앱 전체 흐름 제어
├─ BLE 콜백 처리
├─ 센서 데이터 수신 처리
├─ 위험도 계산 호출
├─ Firebase 업로드 호출
└─ 알림 처리 호출

ble/
├─ BleManager.kt
├─ BleConstants.kt
└─ BlePermissionHelper.kt

data/
├─ SensorData.kt
└─ RiskLevel.kt

parser/
└─ SensorDataParser.kt

risk/
├─ HeatstrokeAnalyzer.kt
└─ RiskCommandMapper.kt

firebase/
├─ FirebaseStatusUploader.kt
└─ RiskLogPolicy.kt

alert/
└─ AlertManager.kt

service/
├─ ForegroundServiceController.kt
└─ SmartShieldForegroundService.kt

test/
└─ FakeSensorDataProvider.kt

ui/
└─ MainUiController.kt
```

---

## 15. 주요 파일 설명

### MainActivity.kt

앱의 중심 Activity입니다.
BLE 스캔, 연결, 데이터 수신, 위험도 계산, Firebase 업로드, 알림 호출 등 전체 흐름을 제어합니다.

### BleManager.kt

BLE 스캔, 연결, GATT 서비스 탐색, Notify 활성화, 센서 데이터 수신, 위험 명령 Write, 자동 재연결을 담당합니다.

### BleConstants.kt

ESP32와 Android 앱이 공유하는 BLE UUID와 요청 코드를 관리합니다.

### BlePermissionHelper.kt

Android 버전별 BLE 권한 확인과 권한 요청을 담당합니다.

### SensorDataParser.kt

ESP32에서 받은 문자열 payload를 `SensorData` 객체로 변환하고, 필수 필드와 값 범위를 검증합니다.

### HeatstrokeAnalyzer.kt

센서 데이터와 자세 상태를 기반으로 위험 단계를 계산합니다.

### RiskCommandMapper.kt

위험 단계를 ESP32로 보낼 명령 문자열로 변환합니다.

### FirebaseStatusUploader.kt

Firebase Realtime Database에 현재 상태와 위험 로그를 업로드합니다.

### RiskLogPolicy.kt

위험 로그의 중복 저장을 방지합니다.

### AlertManager.kt

위험 단계별 팝업과 스마트폰 진동 알림을 담당합니다.

### SmartShieldForegroundService.kt

앱이 백그라운드에서도 실행 중임을 알림으로 유지합니다.

### MainUiController.kt

작업자 앱 화면 표시와 버튼 이벤트 연결을 담당합니다.

### FakeSensorDataProvider.kt

검증용 Fake 센서 payload를 생성합니다.

---

## 16. Android 권한

앱에서 사용하는 주요 권한은 다음과 같습니다.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

Android 11 이하 호환을 위해 다음 권한도 사용합니다.

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
```
