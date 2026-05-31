
# Smart Shield Final Checklist

## 1. 발표 전 핵심 확인

| 항목 | 상태 확인 |
|---|---|
| ESP32 전원 인가 | 부팅 로그 확인 |
| I2C 센서 초기화 | BME280, BH1750, MPU6050, MAX30102, MAX30205 상태 확인 |
| BLE 광고 | `SS_0001` 광고 확인 |
| 작업자 앱 연결 | BLE 연결 성공 확인 |
| Notify 수신 | 작업자 앱에서 payload 수신 확인 |
| Write 전송 | 위험도 명령이 ESP32로 전송되는지 확인 |
| 출력장치 | RED LED, 진동모터, 부저 동작 확인 |
| Firebase 업로드 | `currentStatus`, `riskLogs` 갱신 확인 |
| 관리자 앱 | Firebase 기반 작업자 상태 표시 확인 |

---

## 2. 최종 시스템 흐름 확인

최종 시연 흐름은 다음 순서로 확인한다.

```text
ESP32 센서값 수집
→ BLE Notify
→ Android 작업자 앱 수신
→ payload 파싱
→ 위험도 계산
→ BLE Write 명령 전송
→ ESP32 RED LED / 진동모터 / 부저 제어
→ Firebase currentStatus / riskLogs 업로드
→ 관리자 앱에서 작업자 상태 확인
````

---

## 3. BLE 설정 확인

| 항목                           | 최종 값                                   |
| ---------------------------- | -------------------------------------- |
| BLE Device Name              | `SS_0001`                              |
| Service UUID                 | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Sensor Notify Characteristic | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Control Write Characteristic | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| CCCD                         | `00002902-0000-1000-8000-00805F9B34FB` |

확인할 것:

* Android 앱과 ESP32의 UUID가 동일한지 확인한다.
* Notify 구독이 정상적으로 활성화되는지 확인한다.
* 위험도 명령이 Write characteristic으로 전송되는지 확인한다.
* BLE 연결이 끊겼을 때 재연결 또는 끊김 상태 표시가 동작하는지 확인한다.

---

## 4. BLE Payload 확인

최종 payload 예시는 다음과 같다.

```text
ID:0001,TEMP:34.8,TEMP_VALID:1,TEMP_SOURCE:MEASURED,HR:82,SPO2:98,ENV:28.4,HUM:55,LUX:1200,AX:0.01,AY:0.02,AZ:9.80,POSTURE:NORMAL
```

| 필드               | 확인 여부           |
| ---------------- | --------------- |
| `ID`             | 작업자 ID 파싱 확인    |
| `TEMP`           | 피부 접촉 온도 파싱 확인  |
| `TEMP_VALID`     | 체온 유효 플래그 파싱 확인 |
| `TEMP_SOURCE`    | 체온 데이터 출처 파싱 확인 |
| `HR`             | 심박수 파싱 확인       |
| `SPO2`           | 산소포화도 파싱 확인     |
| `ENV`            | 주변 온도 파싱 확인     |
| `HUM`            | 습도 파싱 확인        |
| `LUX`            | 조도 파싱 확인        |
| `AX`, `AY`, `AZ` | 3축 가속도 파싱 확인    |
| `POSTURE`        | 자세 상태 파싱 확인     |

BME280의 기압값 `PRESS`는 센서 확장 가능 항목이지만, 최종 BLE payload와 Firebase 핵심 스키마에서는 필수 항목으로 사용하지 않는다.

---

## 5. 센서 상태 확인

| 센서                     | 최종 확인 내용                               |
| ---------------------- | -------------------------------------- |
| BME280                 | `ENV`, `HUM` 값 출력                      |
| BH1750                 | `LUX` 값 출력                             |
| MPU6050                | `AX`, `AY`, `AZ`, `POSTURE` 출력         |
| MAX30102 / SEN0344     | `HR`, `SPO2` 출력                        |
| MAX30205 / Fever Click | `TEMP`, `TEMP_VALID`, `TEMP_SOURCE` 출력 |

MAX30205 / Fever Click은 최종 통합 펌웨어 기준으로 `0x48` 주소를 사용한다.
I2C Scanner에서 다른 주소가 확인되면 펌웨어 상수 수정이 필요하다.

---

## 6. 위험도 명령 확인

| 앱 위험도 | ESP32 명령         |
| ----- | ---------------- |
| 정상    | `RISK:SAFE`      |
| 주의    | `RISK:CAUTION`   |
| 위험    | `RISK:DANGER`    |
| 응급    | `RISK:EMERGENCY` |

확인할 것:

* 앱에서 위험도 단계가 정상적으로 계산되는지 확인한다.
* 동일한 위험도 명령이 불필요하게 반복 전송되지 않는지 확인한다.
* ESP32가 수신한 명령에 따라 출력장치를 제어하는지 확인한다.

---

## 7. 출력장치 확인

최종 시연 버전의 출력장치는 다음과 같다.

| 출력장치    | 확인 내용            |
| ------- | ---------------- |
| RED LED | 위험도별 점등 또는 점멸 확인 |
| 진동모터    | 위험도별 진동 패턴 확인    |
| 부저      | 위험도별 경고음 확인      |

최종 코드 기준 출력장치는 RGB LED가 아니라 단일 RED LED이다.
기존 문서의 RGB LED 색상 표시 기준은 사용하지 않는다.

---

## 8. Firebase 확인

Firebase Realtime Database는 다음 경로를 사용한다.

```text
workers/{workerId}/currentStatus
workers/{workerId}/riskLogs/{logId}
```

| 항목                 | 확인 내용                               |
| ------------------ | ----------------------------------- |
| `currentStatus`    | 최신 상태가 덮어쓰기 방식으로 갱신되는지 확인           |
| `riskLogs`         | 위험 이벤트가 누적 저장되는지 확인                 |
| `workerId`         | `SS_0001` → `0001`로 일치하는지 확인        |
| `riskLevel`        | `정상`, `주의`, `위험`, `응급` 한글 값 확인      |
| `riskCommand`      | `RISK:SAFE` 등 ESP32 명령 값 확인         |
| `bleSignalLevel`   | `좋음`, `보통`, `약함`, `끊김`, `연결 전` 값 확인 |
| `workLocationCode` | `LOC_ROOF` 등 최종 코드값 확인              |
| `workLocationName` | 최종 표시명 확인                           |

기존 문서의 `ZONE_A`, `A구역`, `CAUTION`, `GOOD` 같은 예시는 최종 코드 기준과 다르므로 사용하지 않는다.

---

## 9. 작업 위치 코드 확인

| 코드              | 표시명         |
| --------------- | ----------- |
| `LOC_ROOF`      | 옥상 방수 작업 구역 |
| `LOC_WAREHOUSE` | 실내 자재 창고    |
| `LOC_OUTDOOR_A` | 외부 철근 조립 구역 |
| `LOC_BASEMENT`  | 지하 설비 점검 구역 |
| `LOC_SCAFFOLD`  | 외부 비계 작업 구역 |

작업자 앱에서 선택한 작업 위치가 Firebase에 동일하게 저장되는지 확인한다.

---

## 10. 관리자 앱 확인

관리자 앱은 Firebase 데이터를 읽어 작업자 상태를 표시한다.

확인할 것:

* Firebase 연결이 정상인지 확인한다.
* 작업자 목록이 표시되는지 확인한다.
* 위험도 높은 작업자가 구분되는지 확인한다.
* 개별 작업자 상세 정보가 표시되는지 확인한다.
* 주기 갱신 및 Firebase 리스너를 통해 최신 상태가 반영되는지 확인한다.

관리자 앱은 다음 기능을 수행하지 않는다.

```text
BLE 센서 직접 연결
센서 데이터 직접 수신
위험도 직접 계산
ESP32 직접 제어
Firebase 데이터 임의 수정
```

---

## 11. 발표 표현 체크

사용 가능한 표현:

```text
ESP32 기반 PPE 웨어러블 안전 보조 시스템
BLE Notify 기반 센서 데이터 전송
BLE Write 기반 위험도 명령 전송
Firebase Realtime Database 기반 작업자 상태 모니터링
피부 접촉 온도 변화 추적
심박수 변화 기반 생리적 부담 추정
IMU 기반 자세 이상 및 낙상 가능성 추정
다중 센서 융합 기반 위험도 판단
```

피해야 할 표현:

```text
의료기기
정확한 체온 측정
의료급 산소포화도 측정
온열질환 진단
낙상 100% 감지
RGB LED 색상 경고
관리자 앱이 BLE 센서와 직접 연결
관리자 앱이 모든 작업자 상태를 완전 실시간으로 직접 감시
```

---

## 12. 최종 통과 기준

발표 전 최소 통과 기준은 다음과 같다.

| 영역       | 최소 통과 기준                 |
| -------- | ------------------------ |
| ESP32    | 센서값이 Serial Monitor에 출력됨 |
| BLE      | 작업자 앱에서 `SS_0001` 연결 가능  |
| Payload  | 작업자 앱에서 payload 파싱 가능    |
| 위험도      | 정상/주의/위험/응급 중 하나로 표시 가능  |
| 출력       | RED LED, 진동모터, 부저 출력 확인  |
| Firebase | `currentStatus` 갱신 확인    |
| 관리자 앱    | Firebase 기반 작업자 상태 확인 가능 |

위 항목 중 실패한 기능이 있으면 발표에서는 “구현 완료”가 아니라 “추가 검증 필요” 또는 “시연 환경에서 제한적으로 확인”으로 표현한다.
