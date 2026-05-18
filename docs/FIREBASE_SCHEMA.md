
# Smart Shield Firebase Schema

## 1. 문서 목적

이 문서는 Smart Shield 프로젝트에서 사용하는 Firebase Realtime Database 구조를 정의한다.

Firebase는 작업자 앱과 관리자 앱 사이의 데이터 공유 저장소로 사용된다.

- 작업자 앱: Firebase에 작업자 현재 상태와 위험 로그 업로드
- 관리자 앱: Firebase 데이터를 읽어 작업자 상태 모니터링

관리자 앱은 Firebase 데이터를 읽기 전용으로 사용한다.

---

## 2. Database Type

Smart Shield는 Firebase Realtime Database를 사용한다.

```text
Firebase Realtime Database
````

---

## 3. 기본 경로

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

## 4. 작업자 ID 규칙

작업자 ID는 BLE 이름, payload ID, Firebase 경로에서 동일하게 유지한다.

```text
BLE Device Name: SS_0001
Payload ID: 0001
Firebase workerId: 0001
```

### 규칙

```text
workerId는 4자리 숫자 문자열을 사용한다.
BLE 이름은 SS_{workerId} 형식을 사용한다.
payload의 ID 값은 workerId와 동일해야 한다.
Firebase 경로의 workerId도 동일해야 한다.
```

이 규칙이 깨지면 작업자 앱과 관리자 앱이 같은 작업자를 서로 다른 작업자로 인식할 수 있다. 컴퓨터는 눈치가 없다. 시킨 대로만 망한다.

---

## 5. 전체 데이터 구조

```json
{
  "workers": {
    "0001": {
      "currentStatus": {
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
      },
      "riskLogs": {
        "logId_001": {
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
      }
    }
  }
}
```

---

## 6. currentStatus

`currentStatus`는 작업자의 최신 상태를 저장한다.

경로:

```text
workers/{workerId}/currentStatus
```

예시:

```text
workers/0001/currentStatus
```

---

## 7. currentStatus 필드 정의

| 필드                 | 타입      | 설명                        | 예시              |
| ------------------ | ------- | ------------------------- | --------------- |
| `workerId`         | String  | 작업자 ID                    | `0001`          |
| `deviceName`       | String  | BLE 장치 이름                 | `SS_0001`       |
| `workLocationCode` | String  | 작업 위치 코드                  | `ZONE_A`        |
| `workLocationName` | String  | 작업 위치 이름                  | `A구역`           |
| `temp`             | Number  | 피부 접촉 온도                  | `36.5`          |
| `hr`               | Number  | 심박수 추정값                   | `102`           |
| `spo2`             | Number  | 산소포화도 추정값                 | `97`            |
| `env`              | Number  | 주변 온도                     | `33.1`          |
| `hum`              | Number  | 주변 습도                     | `71`            |
| `lux`              | Number  | 조도                        | `45000`         |
| `posture`          | String  | 자세 상태                     | `NORMAL`        |
| `riskLevel`        | String  | 위험도 단계                    | `CAUTION`       |
| `riskCommand`      | String  | ESP32 제어 명령               | `RISK:CAUTION`  |
| `bleConnected`     | Boolean | BLE 연결 여부                 | `true`          |
| `bleSignalLevel`   | String  | BLE 신호 단계                 | `GOOD`          |
| `bleRssi`          | Number  | BLE RSSI                  | `-58`           |
| `appSessionActive` | Boolean | 작업 세션 활성 여부               | `true`          |
| `updatedAt`        | Number  | 마지막 업데이트 시각, epoch millis | `1710000000000` |

---

## 8. riskLogs

`riskLogs`는 위험 상태가 발생했을 때의 로그를 저장한다.

경로:

```text
workers/{workerId}/riskLogs/{logId}
```

예시:

```text
workers/0001/riskLogs/logId_001
```

---

## 9. riskLogs 필드 정의

| 필드                 | 타입     | 설명                     | 예시                              |
| ------------------ | ------ | ---------------------- | ------------------------------- |
| `workerId`         | String | 작업자 ID                 | `0001`                          |
| `riskLevel`        | String | 위험도 단계                 | `DANGER`                        |
| `riskCommand`      | String | ESP32 제어 명령            | `RISK:DANGER`                   |
| `message`          | String | 위험 상태 설명               | `고온·고습 환경과 심박수 상승이 함께 감지되었습니다.` |
| `temp`             | Number | 피부 접촉 온도               | `37.2`                          |
| `hr`               | Number | 심박수 추정값                | `118`                           |
| `spo2`             | Number | 산소포화도 추정값              | `96`                            |
| `env`              | Number | 주변 온도                  | `35.1`                          |
| `hum`              | Number | 주변 습도                  | `78`                            |
| `lux`              | Number | 조도                     | `52000`                         |
| `posture`          | String | 자세 상태                  | `NORMAL`                        |
| `workLocationCode` | String | 작업 위치 코드               | `ZONE_A`                        |
| `workLocationName` | String | 작업 위치 이름               | `A구역`                           |
| `createdAt`        | Number | 로그 생성 시각, epoch millis | `1710000000000`                 |

---

## 10. 위험도 값

`riskLevel`은 다음 값만 사용한다.

```text
SAFE
CAUTION
DANGER
EMERGENCY
```

| 값           | 의미 |
| ----------- | -- |
| `SAFE`      | 정상 |
| `CAUTION`   | 주의 |
| `DANGER`    | 위험 |
| `EMERGENCY` | 응급 |

---

## 11. 위험도 명령 값

`riskCommand`는 ESP32로 전송하는 BLE Write 명령과 동일한 값을 저장한다.

```text
RISK:SAFE
RISK:CAUTION
RISK:DANGER
RISK:EMERGENCY
```

| 값                | 의미 |
| ---------------- | -- |
| `RISK:SAFE`      | 정상 |
| `RISK:CAUTION`   | 주의 |
| `RISK:DANGER`    | 위험 |
| `RISK:EMERGENCY` | 응급 |

`RISK:ERROR`는 공식 위험도 명령이 아니다.
필요하다면 내부 오류 또는 디버깅용 보조 상태로만 사용한다.

---

## 12. 자세 값

`posture`는 다음 값을 사용한다.

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

## 13. BLE 연결 값

### bleConnected

| 값       | 의미        |
| ------- | --------- |
| `true`  | BLE 연결 중  |
| `false` | BLE 연결 끊김 |

### bleSignalLevel

```text
GOOD
NORMAL
WEAK
DISCONNECTED
```

| 값              | 의미    |
| -------------- | ----- |
| `GOOD`         | 신호 좋음 |
| `NORMAL`       | 신호 보통 |
| `WEAK`         | 신호 약함 |
| `DISCONNECTED` | 연결 끊김 |

---

## 14. 작업 위치 값

작업 위치는 코드와 이름을 함께 저장한다.

예시:

```json
{
  "workLocationCode": "ZONE_A",
  "workLocationName": "A구역"
}
```

권장 예시:

| workLocationCode | workLocationName |
| ---------------- | ---------------- |
| `ZONE_A`         | `A구역`            |
| `ZONE_B`         | `B구역`            |
| `ZONE_C`         | `C구역`            |
| `INDOOR`         | `실내`             |
| `OUTDOOR`        | `실외`             |

---

## 15. 시간 값

시간 값은 epoch milliseconds를 사용한다.

```text
1710000000000
```

| 필드          | 의미                        |
| ----------- | ------------------------- |
| `updatedAt` | currentStatus 마지막 업데이트 시각 |
| `createdAt` | riskLogs 생성 시각            |

앱에서는 이 값을 사람이 읽기 쉬운 날짜/시간 형식으로 변환하여 표시한다.

---

## 16. 작업자 앱 Write 정책

작업자 앱은 다음 데이터를 Firebase에 쓴다.

```text
currentStatus
riskLogs
```

작업자 앱의 책임:

```text
BLE 데이터 수신
payload 파싱
위험도 계산
currentStatus 업데이트
위험 발생 시 riskLogs 생성
BLE 연결 상태 업로드
작업 위치 업로드
작업 세션 상태 업로드
```

---

## 17. 관리자 앱 Read 정책

관리자 앱은 Firebase 데이터를 읽기 전용으로 사용한다.

관리자 앱의 책임:

```text
workers 목록 읽기
currentStatus 읽기
riskLogs 읽기
위험도 표시
작업자 상세 정보 표시
구역 필터링
응급 작업자 우선 표시
```

관리자 앱은 다음 작업을 수행하지 않는다.

```text
currentStatus 수정
riskLogs 수정
위험도 계산
ESP32 제어 명령 전송
BLE 연결
```

---

## 18. 데이터 갱신 정책

권장 갱신 정책은 다음과 같다.

| 데이터             | 갱신 방식                     |
| --------------- | ------------------------- |
| `currentStatus` | 작업자 앱에서 주기적 업데이트          |
| `riskLogs`      | 위험 이벤트 발생 시 생성            |
| 관리자 작업자 목록      | 15초 주기 갱신 또는 Firebase 리스너 |
| 관리자 응급 상태       | 실시간 리스너 권장                |

---

## 19. 데이터 누락 처리

관리자 앱은 일부 필드가 누락되어도 앱이 종료되지 않도록 처리해야 한다.

권장 기본 표시:

| 누락 필드              | 표시                    |
| ------------------ | --------------------- |
| `workerId`         | `알 수 없음`              |
| `workLocationName` | `위치 미지정`              |
| `riskLevel`        | `UNKNOWN` 또는 `데이터 없음` |
| `bleConnected`     | `false`               |
| `bleSignalLevel`   | `DISCONNECTED`        |
| 센서값                | `-`                   |
| `updatedAt`        | `업데이트 없음`             |

---

## 20. Firebase 보안 주의사항

`google-services.json`은 Android 앱 빌드에 필요하다.

공개 저장소에 업로드할 경우 다음 사항을 확인한다.

```text
Firebase Realtime Database Rules 설정
API Key 제한 설정
쓰기 권한 제한
읽기 권한 제한
테스트용 공개 규칙 제거
```

테스트 중에는 편의를 위해 규칙을 넓게 열 수 있지만, 공개 저장소나 실제 배포에서는 보안 규칙을 반드시 제한해야 한다.

---

## 21. 권장 Database Rules 예시

아래 규칙은 예시이며, 실제 배포용으로 그대로 사용하면 안 된다.

```json
{
  "rules": {
    "workers": {
      "$workerId": {
        ".read": true,
        ".write": true
      }
    }
  }
}
```

실제 배포에서는 사용자 인증, 작업자 ID 검증, 관리자 권한 분리 등을 적용해야 한다.

---

## 22. 최종 요약

Smart Shield는 Firebase Realtime Database를 사용하여 작업자 앱과 관리자 앱 사이의 데이터를 공유한다.

작업자 앱은 `workers/{workerId}/currentStatus`에 최신 상태를 업로드하고, 위험 이벤트 발생 시 `workers/{workerId}/riskLogs/{logId}`에 로그를 저장한다.

관리자 앱은 Firebase 데이터를 읽기 전용으로 조회하여 작업자 상태를 모니터링한다.

작업자 ID는 BLE 장치 이름, payload ID, Firebase 경로에서 동일하게 유지해야 한다.
