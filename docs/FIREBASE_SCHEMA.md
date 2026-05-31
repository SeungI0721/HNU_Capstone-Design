
# Firebase Realtime Database Schema

## 1. 기본 경로

Smart Shield는 Firebase Realtime Database를 사용하여 작업자의 현재 상태와 위험 로그를 저장한다.

```text
workers/{workerId}/currentStatus
workers/{workerId}/riskLogs/{logId}
````

| 경로                                    | 역할                                  |
| ------------------------------------- | ----------------------------------- |
| `workers/{workerId}/currentStatus`    | 작업자의 최신 센서값, BLE 상태, 위험도, 작업 위치를 저장 |
| `workers/{workerId}/riskLogs/{logId}` | 위험 상태 발생 또는 위험도 변화 시점의 이력을 누적 저장    |

`workerId`는 BLE 장치명, BLE payload, Firebase 경로에서 동일하게 사용한다.

```text
BLE Device Name: SS_0001
Payload ID: 0001
Firebase Path: workers/0001
```

---

## 2. currentStatus 예시

```json
{
  "workers": {
    "0001": {
      "currentStatus": {
        "workerId": "0001",
        "deviceName": "SS_0001",

        "workLocationCode": "LOC_ROOF",
        "workLocationName": "옥상 방수 작업 구역",

        "temp": 34.8,
        "tempValid": true,
        "tempSource": "MEASURED",

        "hr": 82,
        "spo2": 98,

        "env": 28.4,
        "hum": 55,
        "lux": 1200,

        "ax": 0.01,
        "ay": 0.02,
        "az": 9.8,
        "posture": "NORMAL",

        "riskLevel": "정상",
        "riskCommand": "RISK:SAFE",

        "bleConnected": true,
        "bleSignalLevel": "좋음",
        "bleRssi": -58,

        "appSessionActive": true,
        "updatedAt": 1710000000000
      }
    }
  }
}
```

---

## 3. currentStatus 필드 설명

| 필드                 | 타입      | 예시              | 설명                    |
| ------------------ | ------- | --------------- | --------------------- |
| `workerId`         | String  | `"0001"`        | 작업자 또는 장치 식별 ID       |
| `deviceName`       | String  | `"SS_0001"`     | BLE 장치명               |
| `workLocationCode` | String  | `"LOC_ROOF"`    | 작업 위치 코드              |
| `workLocationName` | String  | `"옥상 방수 작업 구역"` | 작업 위치 표시명             |
| `temp`             | Number  | `34.8`          | MAX30205 기반 피부 접촉 온도값 |
| `tempValid`        | Boolean | `true`          | 체온 센서값 유효 여부          |
| `tempSource`       | String  | `"MEASURED"`    | 체온 데이터 출처             |
| `hr`               | Number  | `82`            | 심박수                   |
| `spo2`             | Number  | `98`            | 산소포화도 추정값             |
| `env`              | Number  | `28.4`          | 주변 온도                 |
| `hum`              | Number  | `55`            | 주변 습도                 |
| `lux`              | Number  | `1200`          | 조도                    |
| `ax`               | Number  | `0.01`          | X축 가속도                |
| `ay`               | Number  | `0.02`          | Y축 가속도                |
| `az`               | Number  | `9.8`           | Z축 가속도                |
| `posture`          | String  | `"NORMAL"`      | 자세 상태                 |
| `riskLevel`        | String  | `"정상"`          | 앱 표시용 위험 단계           |
| `riskCommand`      | String  | `"RISK:SAFE"`   | ESP32로 전송되는 위험 명령     |
| `bleConnected`     | Boolean | `true`          | BLE 연결 여부             |
| `bleSignalLevel`   | String  | `"좋음"`          | BLE 신호 상태 표시          |
| `bleRssi`          | Number  | `-58`           | BLE RSSI 값            |
| `appSessionActive` | Boolean | `true`          | 작업 세션 활성 여부           |
| `updatedAt`        | Number  | `1710000000000` | 마지막 갱신 시각             |

---

## 4. 작업 위치 코드

현재 작업자 앱에서 사용하는 작업 위치 코드는 다음 기준을 따른다.

| 코드              | 표시명         |
| --------------- | ----------- |
| `LOC_ROOF`      | 옥상 방수 작업 구역 |
| `LOC_WAREHOUSE` | 실내 자재 창고    |
| `LOC_OUTDOOR_A` | 외부 철근 조립 구역 |
| `LOC_BASEMENT`  | 지하 설비 점검 구역 |
| `LOC_SCAFFOLD`  | 외부 비계 작업 구역 |

기존 문서의 `ZONE_A`, `A구역` 형식은 최종 코드 기준과 다르므로 사용하지 않는다.

---

## 5. 위험도 값

앱 UI와 Firebase에는 한글 위험도 라벨을 저장한다.

| 앱 표시값 | ESP32 전송 명령      |
| ----- | ---------------- |
| `정상`  | `RISK:SAFE`      |
| `주의`  | `RISK:CAUTION`   |
| `위험`  | `RISK:DANGER`    |
| `응급`  | `RISK:EMERGENCY` |

보고서나 발표자료에서 `SAFE`, `CAUTION`, `DANGER`, `EMERGENCY`는 ESP32 제어 명령 또는 내부 프로토콜 설명에만 사용한다.

---

## 6. BLE 신호 상태 값

Firebase의 `bleSignalLevel`은 앱 표시용 한글 값을 사용한다.

| 값      | 의미                    |
| ------ | --------------------- |
| `좋음`   | 안정적인 BLE 연결 상태        |
| `보통`   | 사용 가능하지만 신호가 다소 약한 상태 |
| `약함`   | 끊김 가능성이 있는 약한 연결 상태   |
| `끊김`   | BLE 연결 끊김             |
| `연결 전` | 아직 BLE 연결이 시작되지 않은 상태 |

기존 문서의 `GOOD`, `NORMAL`, `WEAK` 같은 영문 enum 예시는 최종 코드 기준과 다르므로 사용하지 않는다.

---

## 7. riskLogs 예시

```json
{
  "workers": {
    "0001": {
      "riskLogs": {
        "-NxExampleLogId": {
          "workerId": "0001",
          "deviceName": "SS_0001",

          "workLocationCode": "LOC_ROOF",
          "workLocationName": "옥상 방수 작업 구역",

          "riskLevel": "위험",
          "riskCommand": "RISK:DANGER",

          "temp": 37.2,
          "tempValid": true,
          "tempSource": "MEASURED",

          "hr": 118,
          "spo2": 96,

          "env": 34.1,
          "hum": 71,
          "lux": 45000,

          "ax": 1.2,
          "ay": -0.4,
          "az": 8.9,
          "posture": "WARNING",

          "bleConnected": true,
          "bleSignalLevel": "보통",
          "bleRssi": -72,

          "createdAt": 1710000000000
        }
      }
    }
  }
}
```

---

## 8. 주의 사항

* Firebase에는 현재 상태와 위험 로그를 저장한다.
* `currentStatus`는 최신 상태를 덮어쓰는 경로이다.
* `riskLogs`는 위험 이벤트 이력을 누적 저장하는 경로이다.
* 관리자 앱은 Firebase 데이터를 읽어 작업자 상태를 표시한다.
* 관리자 앱은 BLE 연결, 센서 데이터 수신, 위험도 계산, ESP32 제어를 수행하지 않는다.
* Firebase Security Rules는 실제 배포 규칙 파일 또는 콘솔 설정이 확인되지 않으면 “검증 완료”라고 표현하지 않는다.
