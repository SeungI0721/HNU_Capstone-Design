
# Smart Shield System Architecture

## 1. 시스템 개요

Smart Shield는 ESP32 기반 PPE 웨어러블 안전 보조 시스템이다.

작업자의 생체 데이터, 환경 데이터, 자세 및 움직임 데이터를 수집하고, Android 작업자 앱에서 위험도를 계산한 뒤, 위험 상황에서 ESP32 출력장치와 Firebase 기반 관리자 앱을 통해 경고 및 모니터링을 수행한다.

전체 흐름은 다음과 같다.

```text
I2C 센서
→ ESP32
→ BLE Notify
→ Android 작업자 앱
→ 위험도 계산
→ BLE Write
→ ESP32 출력 제어
→ Firebase 업로드
→ Android 관리자 앱 모니터링
````

---

## 2. 주요 구성요소

| 구성요소                       | 역할                                                              |
| -------------------------- | --------------------------------------------------------------- |
| ESP32                      | 센서 수집, BLE Peripheral/GATT Server, 출력장치 제어                      |
| I2C 센서                     | 생체·환경·자세 데이터 측정                                                 |
| 작업자 앱                      | BLE Central/GATT Client, 데이터 수신·파싱, 위험도 계산, UI 표시, Firebase 업로드 |
| Firebase Realtime Database | 작업자 현재 상태와 위험 로그 저장                                             |
| 관리자 앱                      | Firebase 데이터를 읽어 작업자 상태 모니터링                                    |

---

## 3. 센서 구성

| 센서                     | 데이터                                 | 역할                    |
| ---------------------- | ----------------------------------- | --------------------- |
| BME280                 | `ENV`, `HUM`                        | 주변 온도, 습도 측정          |
| BH1750                 | `LUX`                               | 조도 측정, 직사광선 노출 가능성 보조 |
| MPU6050                | `AX`, `AY`, `AZ`, `POSTURE`         | 자세, 움직임, 낙상 후보 판단     |
| MAX30102 / SEN0344     | `HR`, `SPO2`                        | 심박수 및 산소포화도 추정        |
| MAX30205 / Fever Click | `TEMP`, `TEMP_VALID`, `TEMP_SOURCE` | 피부 접촉 온도 변화 추적        |

BME280의 기압값은 센서 확장 가능 항목이지만, 최종 BLE payload와 Firebase 핵심 스키마에서는 `ENV`, `HUM` 중심으로 사용한다.

---

## 4. ESP32 출력장치

최종 시연 버전의 출력장치는 다음과 같다.

| 출력장치    | 역할          |
| ------- | ----------- |
| RED LED | 위험 단계 시각 표시 |
| 진동모터    | 작업자 촉각 경고   |
| 부저      | 작업자 청각 경고   |

기존 설계 문서에 남아 있던 RGB LED 표현은 최종 코드 기준과 다르다.
최종 구현에서는 단일 RED LED를 사용하며, 위험도에 따라 점등 또는 점멸 패턴을 적용한다.

---

## 5. BLE 통신 구조

ESP32는 BLE Peripheral / GATT Server로 동작한다.
Android 작업자 앱은 BLE Central / GATT Client로 동작한다.

| 항목                           | 값                                      |
| ---------------------------- | -------------------------------------- |
| BLE Device Name              | `SS_0001`                              |
| Service UUID                 | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Sensor Notify Characteristic | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Control Write Characteristic | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| CCCD                         | `00002902-0000-1000-8000-00805F9B34FB` |

센서 데이터는 ESP32에서 앱으로 BLE Notify 방식으로 전송된다.
위험도 제어 명령은 앱에서 ESP32로 BLE Write 방식으로 전송된다.

---

## 6. BLE Payload 예시

```text
ID:0001,TEMP:34.8,TEMP_VALID:1,TEMP_SOURCE:MEASURED,HR:82,SPO2:98,ENV:28.4,HUM:55,LUX:1200,AX:0.01,AY:0.02,AZ:9.80,POSTURE:NORMAL
```

| 필드               | 의미           |
| ---------------- | ------------ |
| `ID`             | 작업자 ID       |
| `TEMP`           | 피부 접촉 온도     |
| `TEMP_VALID`     | 체온 센서값 유효 여부 |
| `TEMP_SOURCE`    | 체온 데이터 출처    |
| `HR`             | 심박수          |
| `SPO2`           | 산소포화도 추정값    |
| `ENV`            | 주변 온도        |
| `HUM`            | 주변 습도        |
| `LUX`            | 조도           |
| `AX`, `AY`, `AZ` | 3축 가속도       |
| `POSTURE`        | 자세 상태        |

---

## 7. 위험도 명령

작업자 앱은 계산된 위험도에 따라 ESP32로 다음 명령을 전송한다.

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

ESP32는 수신한 명령에 따라 RED LED, 진동모터, 부저를 제어한다.

---

## 8. Firebase 구조

Firebase Realtime Database는 다음 구조를 사용한다.

```text
workers/{workerId}/currentStatus
workers/{workerId}/riskLogs/{logId}
```

| 경로              | 역할           |
| --------------- | ------------ |
| `currentStatus` | 최신 상태 저장     |
| `riskLogs`      | 위험 이벤트 이력 저장 |

작업자 앱은 Firebase에 현재 상태와 위험 로그를 업로드한다.
관리자 앱은 Firebase 데이터를 읽어 작업자 상태를 표시한다.

관리자 앱은 BLE 연결, 센서 데이터 수신, 위험도 계산, ESP32 제어를 수행하지 않는다.

---

## 9. 관리자 앱 동작

관리자 앱은 Firebase에 저장된 작업자 상태를 조회하여 작업자 목록과 위험 작업자를 표시한다.

최종 표현은 다음과 같이 작성한다.

```text
관리자 앱은 Firebase 기반으로 작업자 상태를 모니터링하며, 주기 갱신과 Firebase 리스너를 통해 최신 상태를 반영한다.
```

다음 표현은 피한다.

```text
관리자 앱이 모든 작업자 상태를 완전 실시간으로 직접 감시한다.
관리자 앱이 BLE 센서와 직접 연결한다.
관리자 앱이 위험도 계산을 수행한다.
```
