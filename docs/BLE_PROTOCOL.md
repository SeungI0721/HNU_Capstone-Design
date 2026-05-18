
# Smart Shield BLE Protocol

## 1. 문서 목적

이 문서는 Smart Shield 프로젝트에서 사용하는 BLE 통신 규격을 정리한다.

Smart Shield는 ESP32 웨어러블 장치와 Android 작업자 앱 사이에서 BLE Notify / Write 구조를 사용한다.

- ESP32 → Android 작업자 앱: 센서 데이터 전송
- Android 작업자 앱 → ESP32: 위험도 명령 전송

관리자 앱은 BLE 통신을 수행하지 않는다.

---

## 2. BLE 역할

| 장치 | BLE 역할 |
| --- | --- |
| ESP32 | Peripheral / GATT Server |
| Android 작업자 앱 | Central / GATT Client |
| Android 관리자 앱 | BLE 사용 안 함 |

---

## 3. BLE 통신 흐름

```text
ESP32 센서값 수집
→ BLE Notify
→ Android 작업자 앱 수신
→ Payload 파싱
→ 위험도 계산
→ BLE Write
→ ESP32 출력 장치 제어
````

---

## 4. BLE 장치 이름 규칙

ESP32 BLE 장치 이름은 다음 형식을 사용한다.

```text
SS_0001
```

### 이름 규칙

```text
SS_{workerId}
```

예시:

```text
SS_0001
SS_0002
SS_0003
```

`workerId`는 4자리 숫자 문자열을 사용한다.

---

## 5. 작업자 ID 매핑 규칙

작업자 ID는 BLE 이름, payload ID, Firebase 경로에서 동일하게 유지한다.

```text
BLE Device Name: SS_0001
Payload ID: 0001
Firebase workerId: 0001
```

이 규칙을 유지해야 ESP32, 작업자 앱, Firebase, 관리자 앱이 동일한 작업자를 기준으로 동작한다.

---

## 6. BLE UUID

| 항목                           | UUID                                   |
| ---------------------------- | -------------------------------------- |
| Service UUID                 | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Sensor Notify Characteristic | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Control Write Characteristic | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| CCCD                         | `00002902-0000-1000-8000-00805F9B34FB` |

---

## 7. Characteristic 역할

| Characteristic               | 방향            | 역할            |
| ---------------------------- | ------------- | ------------- |
| Sensor Notify Characteristic | ESP32 → 작업자 앱 | 센서 payload 전송 |
| Control Write Characteristic | 작업자 앱 → ESP32 | 위험도 명령 전송     |
| CCCD                         | 작업자 앱 설정      | Notify 활성화    |

---

## 8. Notify Payload 형식

ESP32는 센서 데이터를 문자열 payload로 구성하여 Notify로 전송한다.

### 기본 형식

```text
KEY:VALUE,KEY:VALUE,KEY:VALUE
```

### 예시

```text
ID:0001,TEMP:36.5,HR:102,SPO2:97,ENV:33.1,HUM:71,LUX:45000,AX:0.12,AY:-0.08,AZ:9.78,POSTURE:NORMAL
```

---

## 9. Payload 필드 정의

| 필드        | 타입           | 의미        | 예시       |
| --------- | ------------ | --------- | -------- |
| `ID`      | String       | 작업자 ID    | `0001`   |
| `TEMP`    | Float        | 피부 접촉 온도  | `36.5`   |
| `HR`      | Int          | 심박수 추정값   | `102`    |
| `SPO2`    | Int          | 산소포화도 추정값 | `97`     |
| `ENV`     | Float        | 주변 온도     | `33.1`   |
| `HUM`     | Float        | 주변 습도     | `71`     |
| `LUX`     | Float 또는 Int | 조도        | `45000`  |
| `AX`      | Float        | X축 가속도    | `0.12`   |
| `AY`      | Float        | Y축 가속도    | `-0.08`  |
| `AZ`      | Float        | Z축 가속도    | `9.78`   |
| `POSTURE` | String       | 자세 상태     | `NORMAL` |

---

## 10. POSTURE 값

`POSTURE` 필드는 MPU6050 기반 자세 상태를 나타낸다.

```text
NORMAL
WARNING
UNSTABLE
FALL
EMERGENCY
```

| 값           | 의미                |
| ----------- | ----------------- |
| `NORMAL`    | 정상 자세             |
| `WARNING`   | 주의가 필요한 자세 또는 움직임 |
| `UNSTABLE`  | 불안정한 자세 또는 움직임    |
| `FALL`      | 낙상 가능성            |
| `EMERGENCY` | 즉시 확인이 필요한 자세 상태  |

---

## 11. Payload 작성 규칙

### 권장 규칙

```text
1. 필드는 쉼표로 구분한다.
2. 키와 값은 콜론으로 구분한다.
3. 키 이름은 대문자로 작성한다.
4. 숫자 값은 단위를 붙이지 않는다.
5. 알 수 없는 값은 빈 문자열보다 기본값 또는 이전 안정값을 사용한다.
6. payload 마지막에 불필요한 쉼표를 붙이지 않는다.
```

### 좋은 예시

```text
ID:0001,TEMP:36.5,HR:102,SPO2:97,ENV:33.1,HUM:71,LUX:45000,AX:0.12,AY:-0.08,AZ:9.78,POSTURE:NORMAL
```

### 나쁜 예시

```text
ID=0001,TEMP=36.5,HR=102
TEMP:36.5C,HR:102bpm
ID:0001,TEMP:36.5,
```

인간도 헷갈리는데 파서는 더 쉽게 삐진다. 형식은 고정한다.

---

## 12. 누락 필드 처리

작업자 앱은 일부 필드가 누락되어도 앱이 종료되지 않도록 처리해야 한다.

권장 처리:

| 누락 필드            | 처리                       |
| ---------------- | ------------------------ |
| `ID`             | BLE 장치 이름에서 workerId 추출  |
| `TEMP`           | 이전 안정값 또는 null 처리        |
| `HR`             | 이전 안정값 또는 null 처리        |
| `SPO2`           | 이전 안정값 또는 null 처리        |
| `ENV`            | 이전 안정값 또는 null 처리        |
| `HUM`            | 이전 안정값 또는 null 처리        |
| `LUX`            | 이전 안정값 또는 null 처리        |
| `AX`, `AY`, `AZ` | 활동량 계산 제외 또는 기본값 처리      |
| `POSTURE`        | `NORMAL` 또는 `UNKNOWN` 처리 |

---

## 13. 비정상 값 처리

작업자 앱은 비정상적인 숫자값을 그대로 신뢰하지 않는다.

예시:

```text
HR:0
HR:255
SPO2:0
TEMP:-127
ENV:999
LUX:-1
```

권장 처리:

```text
1. 물리적으로 불가능한 값은 무효 처리한다.
2. 이전 안정값과 지나치게 차이 나는 값은 튐 값으로 처리한다.
3. 일정 횟수 이상 무효값이 반복되면 센서 오류 상태로 표시한다.
4. 무효값은 위험도 계산에서 제외하거나 가중치를 낮춘다.
```

---

## 14. BLE Write 명령 형식

작업자 앱은 위험도 계산 결과를 ESP32로 문자열 명령으로 전송한다.

공식 명령은 다음 4개이다.

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

---

## 15. BLE Write 명령 정의

| 명령               | 의미 | ESP32 출력                      |
| ---------------- | -- | ----------------------------- |
| `RISK:SAFE`      | 정상 | LED 초록, 진동 OFF, 부저 OFF        |
| `RISK:CAUTION`   | 주의 | LED 노랑, 짧은 진동, 짧은 부저          |
| `RISK:DANGER`    | 위험 | LED 빨강, 반복 진동, 반복 부저          |
| `RISK:EMERGENCY` | 응급 | LED 빨강 점멸, 강한 반복 진동, 빠른 반복 부저 |

---

## 16. `RISK:ERROR` 처리

`RISK:ERROR`는 공식 위험도 명령이 아니다.

`RISK:ERROR`는 다음 경우에만 내부 보조 상태로 사용할 수 있다.

```text
알 수 없는 명령 수신
payload 파싱 실패
센서 상태 오류
디버깅용 예외 처리
```

앱과 ESP32 사이의 공식 위험도 프로토콜은 다음 4개만 기준으로 한다.

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

---

## 17. 전송 주기

BLE Notify 전송 주기는 안정성을 우선하여 설정한다.

권장 기준:

| 구분         | 전송 주기          |
| ---------- | -------------- |
| 안정 구현      | 500ms ~ 1000ms |
| 실시간성 개선    | 100ms ~ 200ms  |
| 고속 스트리밍 실험 | 35ms           |
| 검증 전 비권장   | 25ms 이하        |

최종 캡스톤 구현에서는 1초 주기 전송도 충분히 실용적이다.

---

## 18. BLE 연결 및 재연결 정책

작업자 앱은 ESP32와 BLE GATT 연결을 유지한다.

연결이 끊긴 경우 다음 순서로 처리한다.

```text
1. 연결 끊김 감지
2. UI에 연결 끊김 상태 표시
3. Firebase에 bleConnected=false 업로드
4. 마지막 연결 장치 정보 저장
5. 일정 간격으로 재연결 시도
6. 10분 이내 재연결을 목표로 반복 시도
7. 재연결 성공 시 Notify 재활성화
8. 재연결 실패가 길어지면 관리자 확인 필요 상태로 표시
```

---

## 19. RSSI 및 신호 세기

작업자 앱은 BLE RSSI를 사람이 이해하기 쉬운 단계로 변환하여 표시한다.

예시 기준:

| RSSI       | 표시 | Firebase 값     |
| ---------- | -- | -------------- |
| -60 dBm 이상 | 좋음 | `GOOD`         |
| -75 dBm 이상 | 보통 | `NORMAL`       |
| -90 dBm 이상 | 약함 | `WEAK`         |
| 연결 끊김      | 끊김 | `DISCONNECTED` |

RSSI 기준은 실제 테스트 결과에 따라 조정할 수 있다.

---

## 20. BLE 위치별 검증

BLE 성능은 휴대폰 위치와 인체 차폐에 영향을 받는다.

권장 테스트 조건:

```text
휴대폰 손에 든 상태
휴대폰 앞주머니
휴대폰 뒷주머니
휴대폰 가방 안
휴대폰 책상 위
ESP32와 같은 몸쪽
ESP32와 반대 몸쪽
걷기
팔 움직임
```

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

## 21. Android BLE 권한

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

Android 14 이상에서는 Foreground Service Type 설정도 확인해야 한다.

---

## 22. BLE 디버깅 체크리스트

| 문제          | 확인 사항                                              |
| ----------- | -------------------------------------------------- |
| 장치 검색 안 됨   | Bluetooth ON, 위치 권한, BLE 이름, ESP32 advertise 상태 확인 |
| 연결 실패       | 장치 전원, GATT 연결 상태, 기존 연결 해제 여부 확인                  |
| Service 없음  | Service UUID 일치 여부 확인                              |
| Notify 안 됨  | Characteristic UUID, CCCD 설정, Notify enable 여부 확인  |
| Payload 깨짐  | 문자열 인코딩, payload 길이, 구분자 형식 확인                     |
| Write 반응 없음 | Write Characteristic UUID, 명령 문자열, 개행 문자 포함 여부 확인  |
| 자주 끊김       | 휴대폰 위치, 인체 차폐, 전송 주기, RSSI 확인                      |
| 재연결 실패      | 이전 GATT close 처리, scan 재시작, Android BLE 캐시 문제 확인   |

---

## 23. 최종 요약

Smart Shield BLE 프로토콜은 ESP32와 Android 작업자 앱 사이의 통신 규격이다.

ESP32는 BLE Notify로 센서 payload를 전송하고, 작업자 앱은 payload를 파싱하여 위험도를 계산한다. 이후 작업자 앱은 BLE Write로 `RISK:SAFE`, `RISK:CAUTION`, `RISK:DANGER`, `RISK:EMERGENCY` 명령을 ESP32에 전송한다.

관리자 앱은 BLE를 사용하지 않으며, Firebase에 업로드된 데이터를 읽어 작업자 상태를 모니터링한다.
