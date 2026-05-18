
# Smart Shield System Architecture

## 1. 문서 목적

이 문서는 Smart Shield 프로젝트의 전체 시스템 구조를 설명한다.

Smart Shield는 ESP32 기반 웨어러블 장치, 작업자용 Android 앱, 관리자용 Android 앱, Firebase Realtime Database로 구성된 산업안전 보조 시스템이다.

본 문서는 각 구성 요소의 역할, 데이터 흐름, BLE 통신 구조, Firebase 연동 구조, 위험도 계산 위치를 정리한다.

---

## 2. 전체 시스템 개요

Smart Shield는 작업자의 환경 데이터, 생체 신호, 자세 및 움직임 데이터를 수집하여 온열질환 위험 가능성과 이상 상태를 조기에 감지하는 캡스톤 프로젝트이다.

전체 구조는 다음과 같다.

```text
ESP32 웨어러블 장치
→ Android 작업자 앱
→ Firebase Realtime Database
→ Android 관리자 앱
````

ESP32는 센서 데이터를 수집하고, 작업자 앱은 데이터를 분석하여 위험도를 계산한다. 관리자 앱은 Firebase에 저장된 작업자 상태를 읽어 모니터링한다.

---

## 3. 구성 요소

| 구성 요소                      | 역할                                                      |
| -------------------------- | ------------------------------------------------------- |
| ESP32 웨어러블 장치              | 센서 데이터 수집, BLE Notify 송신, BLE Write 명령 수신, 출력 장치 제어     |
| Android 작업자 앱              | BLE 연결, 센서 데이터 수신, 위험도 계산, Firebase 업로드, ESP32 제어 명령 전송 |
| Firebase Realtime Database | 작업자 현재 상태와 위험 로그 저장                                     |
| Android 관리자 앱              | Firebase 데이터 읽기, 작업자 상태 모니터링                            |

---

## 4. 시스템 흐름

```text
1. 작업자가 웨어러블 장치를 착용한다.
2. ESP32가 센서값을 수집한다.
3. 작업자 앱이 ESP32와 BLE로 연결된다.
4. ESP32가 BLE Notify로 센서 payload를 전송한다.
5. 작업자 앱이 payload를 파싱한다.
6. 작업자 앱이 위험도를 계산한다.
7. 작업자 앱이 BLE Write로 ESP32에 위험도 명령을 전송한다.
8. ESP32가 위험도에 따라 RGB LED, 진동모터, 부저를 제어한다.
9. 작업자 앱이 Firebase에 현재 상태와 위험 로그를 업로드한다.
10. 관리자 앱이 Firebase 데이터를 읽어 작업자 상태를 표시한다.
```

---

## 5. 전체 데이터 흐름도

```text
[센서]
  │
  ▼
[ESP32]
  │ BLE Notify
  ▼
[작업자 앱]
  ├─ payload 파싱
  ├─ 위험도 계산
  ├─ UI 표시
  ├─ 스마트폰 진동 / 팝업
  ├─ BLE Write → ESP32 출력 제어
  └─ Firebase 업로드
          │
          ▼
[Firebase Realtime Database]
          │
          ▼
[관리자 앱]
  ├─ 작업자 목록 표시
  ├─ 위험도 표시
  ├─ BLE 상태 표시
  └─ 작업자 상세 상태 표시
```

---

## 6. ESP32 역할

ESP32는 웨어러블 장치의 중심 보드이다.

### 주요 역할

```text
센서 데이터 수집
BLE Peripheral / GATT Server 동작
BLE Notify로 센서 payload 전송
BLE Write 명령 수신
RGB LED 제어
진동모터 제어
부저 제어
시리얼 로그 출력
```

### ESP32가 직접 수행하지 않는 것

```text
최종 위험도 계산
Firebase 업로드
관리자 모니터링
의료 진단 판단
```

위험도 계산은 Android 작업자 앱에서 수행한다.

---

## 7. 작업자 앱 역할

작업자 앱은 Smart Shield 시스템의 중심 소프트웨어이다.

### 주요 역할

```text
ESP32 BLE 장치 검색
ESP32 BLE 연결
BLE Notify 수신
센서 payload 파싱
위험도 계산
작업자 UI 업데이트
위험 단계별 팝업 표시
스마트폰 진동 경고
BLE Write로 ESP32 제어 명령 전송
Firebase 현재 상태 업로드
Firebase 위험 로그 저장
Foreground Service 기반 작업 세션 유지
BLE 끊김 감지 및 재연결 처리
```

### 작업자 앱이 계산하는 데이터

```text
위험도 단계
ESP32 제어 명령
BLE 신호 단계
작업 세션 상태
Firebase 업로드 데이터
```

---

## 8. 관리자 앱 역할

관리자 앱은 Firebase 데이터를 읽어 작업자 상태를 표시한다.

### 주요 역할

```text
작업자 목록 조회
작업자 위험도 표시
작업 위치 표시
BLE 연결 상태 표시
응급 작업자 우선 표시
작업자 상세 정보 표시
위험 로그 확인
```

### 관리자 앱이 수행하지 않는 것

```text
BLE 스캔
BLE 연결
BLE Notify 수신
센서 payload 파싱
위험도 계산
ESP32 제어 명령 전송
Firebase 데이터 수정
```

관리자 앱은 읽기 전용 모니터링 앱이다.

---

## 9. Firebase 역할

Firebase Realtime Database는 작업자 상태 공유를 위한 중앙 데이터 저장소이다.

### 저장 데이터

```text
작업자 현재 상태
위험 로그
작업 위치
BLE 연결 상태
마지막 업데이트 시각
위험도 단계
센서값
```

### 주요 경로

```text
workers/{workerId}/currentStatus
workers/{workerId}/riskLogs/{logId}
```

---

## 10. BLE 통신 구조

Smart Shield는 BLE Notify / Write 구조를 사용한다.

```text
ESP32 → 작업자 앱: BLE Notify
작업자 앱 → ESP32: BLE Write
```

### BLE 역할

| 장치    | 역할                           |
| ----- | ---------------------------- |
| ESP32 | BLE Peripheral / GATT Server |
| 작업자 앱 | BLE Central / GATT Client    |
| 관리자 앱 | BLE 사용 안 함                   |

---

## 11. BLE UUID

| 항목                           | UUID                                   |
| ---------------------------- | -------------------------------------- |
| Service UUID                 | `089fca17-755f-4578-b8af-ee5e32526b0f` |
| Sensor Notify Characteristic | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Control Write Characteristic | `0000FFF2-0000-1000-8000-00805F9B34FB` |
| CCCD                         | `00002902-0000-1000-8000-00805F9B34FB` |

---

## 12. 작업자 ID 규칙

작업자 ID는 전체 시스템에서 동일하게 유지한다.

```text
BLE Device Name: SS_0001
Payload ID: 0001
Firebase workerId: 0001
```

이 규칙을 유지해야 ESP32, 작업자 앱, Firebase, 관리자 앱이 같은 작업자를 기준으로 연결된다.

---

## 13. 센서 데이터 흐름

ESP32는 다음 센서 데이터를 수집한다.

| 데이터              | 센서                     | 의미        |
| ---------------- | ---------------------- | --------- |
| `TEMP`           | MAX30205 / Fever Click | 피부 접촉 온도  |
| `HR`             | MAX30102 / SEN0344     | 심박수 추정값   |
| `SPO2`           | MAX30102 / SEN0344     | 산소포화도 추정값 |
| `ENV`            | BME280                 | 주변 온도     |
| `HUM`            | BME280                 | 주변 습도     |
| `LUX`            | BH1750                 | 조도        |
| `AX`, `AY`, `AZ` | MPU6050                | 가속도       |
| `POSTURE`        | MPU6050 기반 판단          | 자세 상태     |

---

## 14. 위험도 계산 위치

위험도 계산은 Android 작업자 앱에서 수행한다.

```text
ESP32: 센서 수집 및 출력 장치 제어
작업자 앱: 위험도 계산
관리자 앱: 위험도 표시
Firebase: 상태 저장
```

ESP32는 최종 위험도를 자체 판단하지 않고, 작업자 앱이 전송한 명령에 따라 출력 장치를 제어한다.

---

## 15. 위험도 단계

| 단계        | 명령               | 의미           |
| --------- | ---------------- | ------------ |
| SAFE      | `RISK:SAFE`      | 위험 징후 없음     |
| CAUTION   | `RISK:CAUTION`   | 위험 가능성 증가    |
| DANGER    | `RISK:DANGER`    | 위험 상태 가능성 높음 |
| EMERGENCY | `RISK:EMERGENCY` | 즉시 확인 필요     |

---

## 16. 위험도 판단 원칙

위험도는 단일 센서값이 아니라 여러 데이터를 종합하여 판단한다.

사용 데이터:

```text
주변 온도
주변 습도
조도
피부 접촉 온도
심박수
산소포화도
자세 상태
활동량
BLE 연결 상태
```

판단 원칙:

```text
고온·고습 환경일수록 위험 가능성 증가
피부 접촉 온도 상승 추세가 지속되면 위험 가능성 증가
활동량 대비 심박수 상승이 크면 위험 가능성 증가
낙상 또는 장시간 움직임 없음은 응급 후보
SpO2는 열스트레스 핵심 지표가 아니라 응급 보조 플래그
BLE 장기 끊김은 상태 확인 필요 플래그
```

---

## 17. 의료기기 아님

Smart Shield는 의료기기가 아니다.

금지 표현:

```text
온열질환 진단
정확한 체온 측정
의료급 산소포화도 측정
낙상 100% 감지
SpO2 기반 열스트레스 판단
HR > 120이면 무조건 위험
TEMP > 37.5이면 무조건 위험
```

권장 표현:

```text
산업안전 보조 시스템
온열질환 위험 가능성 조기 감지
피부 접촉 온도 변화 추적
심박수 변화 기반 생리적 부담 추정
IMU 기반 자세 이상 및 낙상 가능성 추정
다중 센서 융합 기반 위험도 판단
```

---

## 18. BLE 안정성 고려사항

BLE 통신 품질은 다음 요소에 영향을 받는다.

```text
휴대폰 위치
ESP32 착용 위치
인체 차폐
장애물
휴대폰 기종
OS 버전
BLE 칩셋
전송 주기
payload 크기
```

따라서 실제 테스트에서는 휴대폰 위치별 안정성 검증이 필요하다.

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

---

## 19. 시스템 제한사항

Smart Shield는 캡스톤 구현을 위한 산업안전 보조 시스템이다.

제한사항:

```text
의료 진단 불가
센서값 절대 정확도 보장 불가
BLE 통신 안정성은 기기와 환경에 따라 달라짐
손목 PPG 값은 움직임에 취약함
피부 접촉 온도는 심부체온이 아님
조도값은 복사열이 아님
MPU6050만으로 낙상을 100% 확정할 수 없음
Firebase 네트워크 지연 가능성 존재
```

---

## 20. 최종 요약

Smart Shield는 ESP32 웨어러블 장치, 작업자 Android 앱, Firebase Realtime Database, 관리자 Android 앱으로 구성된다.

ESP32는 센서 데이터를 수집하고 BLE Notify로 작업자 앱에 전송한다. 작업자 앱은 데이터를 파싱하고 위험도를 계산한 뒤 ESP32에 BLE Write 명령을 전송하며 Firebase에 상태를 업로드한다. 관리자 앱은 Firebase 데이터를 읽어 작업자 상태를 모니터링한다.

위험도 계산은 작업자 앱에서 수행되며, 관리자 앱은 읽기 전용 모니터링 앱으로 동작한다.
