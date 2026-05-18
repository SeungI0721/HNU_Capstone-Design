
# Smart Shield Test Plan

## 1. 문서 목적

이 문서는 Smart Shield 프로젝트의 최종 검증 계획을 정리한다.

Smart Shield는 ESP32 기반 웨어러블 장치, Android 작업자 앱, Firebase Realtime Database, Android 관리자 앱으로 구성된다. 따라서 최종 검증은 단순히 코드가 실행되는지만 확인하는 것이 아니라, 센서 데이터 수집, BLE 통신, 위험도 계산, Firebase 업로드, 관리자 앱 표시까지 전체 흐름을 확인해야 한다.

---

## 2. 검증 범위

최종 검증 범위는 다음과 같다.

```text
1. ESP32 하드웨어 및 센서 동작 검증
2. BLE 통신 검증
3. Android 작업자 앱 검증
4. 위험도 알고리즘 검증
5. Firebase 데이터 업로드 검증
6. Android 관리자 앱 검증
7. 위치별 BLE 안정성 검증
8. 최종 착용 상태 통합 검증
````

---

## 3. 전체 검증 순서

권장 검증 순서는 다음과 같다.

```text
1. 하드웨어 단독 테스트
2. 센서별 단독 테스트
3. ESP32 통합 펌웨어 테스트
4. BLE Advertise 확인
5. Android 작업자 앱 BLE 연결 테스트
6. BLE Notify payload 수신 테스트
7. Payload 파싱 테스트
8. 위험도 계산 테스트
9. BLE Write 명령 전송 테스트
10. ESP32 출력 장치 제어 테스트
11. Firebase currentStatus 업로드 테스트
12. Firebase riskLogs 생성 테스트
13. 관리자 앱 Firebase 읽기 테스트
14. 관리자 앱 UI 표시 테스트
15. BLE 위치별 안정성 테스트
16. 최종 착용 상태 통합 테스트
```

---

## 4. 테스트 환경

테스트 환경은 다음 항목을 기록한다.

| 항목            | 기록 내용                  |
| ------------- | ---------------------- |
| 테스트 일자        | YYYY-MM-DD             |
| 테스트 장소        | 실내 / 실외 / 실험실 / 복도 등   |
| 테스트 담당자       | 이름 또는 역할               |
| ESP32 보드      | ESP32 DevKit 등         |
| Android 기기    | 모델명                    |
| Android 버전    | 예: Android 13          |
| 앱 버전          | 작업자 앱 / 관리자 앱          |
| Firebase 프로젝트 | 프로젝트명                  |
| 착용 위치         | 손목 / 팔 / 의류 부착 등       |
| 휴대폰 위치        | 손 / 앞주머니 / 뒷주머니 / 가방 등 |

---

## 5. 하드웨어 기본 검증

### 5-1. 전원 및 배선

| 항목          | 기대 결과                 | 결과          |
| ----------- | --------------------- | ----------- |
| ESP32 전원 인가 | 정상 부팅                 | PASS / FAIL |
| USB 연결      | PC에서 포트 인식            | PASS / FAIL |
| 센서 전원       | 각 센서 전원 정상            | PASS / FAIL |
| GND 공통      | 모든 모듈 GND 공통 연결       | PASS / FAIL |
| I2C SDA/SCL | GPIO21 / GPIO22 기준 연결 | PASS / FAIL |
| 출력 장치 배선    | RGB LED, 진동모터, 부저 연결  | PASS / FAIL |

### 5-2. 주의사항

```text
진동모터는 ESP32 GPIO에 직접 연결하지 않는다.
MOSFET 또는 트랜지스터를 사용한다.
모터 사용 시 플라이백 다이오드를 권장한다.
I2C 센서는 3.3V 기준으로 연결한다.
긴 점퍼선은 I2C 통신 불안정 원인이 될 수 있다.
```

---

## 6. I2C 센서 주소 검증

I2C Scanner로 센서 주소를 확인한다.

| 센서                     | 예상 주소                     | 확인 결과       |
| ---------------------- | ------------------------- | ----------- |
| BME280                 | `0x76` 또는 `0x77`          | PASS / FAIL |
| BH1750 / GY-302        | `0x23` 또는 `0x5C`          | PASS / FAIL |
| MPU6050 / GY-521       | `0x68` 또는 `0x69`          | PASS / FAIL |
| MAX30102 / SEN0344     | `0x57`                    | PASS / FAIL |
| MAX30205 / Fever Click | `0x48` 또는 `0x49` 계열 확인 필요 | PASS / FAIL |

> 실제 주소는 모듈 배선과 보드 설정에 따라 달라질 수 있다.
> 코드의 주소값과 I2C Scanner 결과가 일치해야 한다.

---

## 7. 센서별 단독 테스트

### 7-1. BME280

| 테스트 항목   | 기대 결과               | 결과          |
| -------- | ------------------- | ----------- |
| 주변 온도 출력 | 실내 온도와 유사한 값 출력     | PASS / FAIL |
| 습도 출력    | 실내 습도와 유사한 값 출력     | PASS / FAIL |
| 기압 출력    | 비정상적인 0 또는 999 값 없음 | PASS / FAIL |

주의:

```text
BME280은 피부 온도나 체온을 측정하지 않는다.
ESP32 발열과 가까우면 주변 온도값이 높게 나올 수 있다.
```

---

### 7-2. BH1750 / GY-302

| 테스트 항목 | 기대 결과     | 결과          |
| ------ | --------- | ----------- |
| 어두운 곳  | 낮은 LUX 출력 | PASS / FAIL |
| 밝은 곳   | 높은 LUX 출력 | PASS / FAIL |
| 손으로 가림 | LUX 감소    | PASS / FAIL |

주의:

```text
BH1750은 복사열 센서가 아니다.
조도값은 직사광선 노출 가능성의 보조 지표이다.
```

---

### 7-3. MPU6050 / GY-521

| 테스트 항목   | 기대 결과           | 결과          |
| -------- | --------------- | ----------- |
| 정지 상태    | 가속도값 안정         | PASS / FAIL |
| 기울임      | AX / AY / AZ 변화 | PASS / FAIL |
| 흔들림      | 활동량 증가          | PASS / FAIL |
| 낙상 후보 동작 | POSTURE 값 변화    | PASS / FAIL |

주의:

```text
MPU6050만으로 낙상을 100% 확정하지 않는다.
낙상 후보, 움직임 없음, 자세 변화 등을 함께 판단한다.
```

---

### 7-4. MAX30102 / SEN0344

| 테스트 항목 | 기대 결과               | 결과          |
| ------ | ------------------- | ----------- |
| 피부 접촉  | HR 값 출력             | PASS / FAIL |
| 접촉 해제  | 값 불안정 또는 무효 처리      | PASS / FAIL |
| 움직임 발생 | HR / SpO2 신뢰도 저하 처리 | PASS / FAIL |
| 외부광 차단 | 값 안정성 개선            | PASS / FAIL |

주의:

```text
손목 PPG 값은 움직임, 압박, 외부광에 민감하다.
HR은 절대값보다 baseline 대비 변화량을 중심으로 해석한다.
SpO2는 열스트레스 핵심 지표가 아니라 응급 보조 플래그로만 사용한다.
```

---

### 7-5. MAX30205 / Fever Click

| 테스트 항목         | 기대 결과         | 결과          |
| -------------- | ------------- | ----------- |
| 피부 접촉          | TEMP 값 안정     | PASS / FAIL |
| 접촉 해제          | 값 변화 또는 무효 처리 | PASS / FAIL |
| 외부 열원 접근       | TEMP 상승       | PASS / FAIL |
| ESP32 발열 영향 확인 | 센서값 과상승 없음    | PASS / FAIL |

주의:

```text
MAX30205 / Fever Click은 피부 접촉 온도 센서이다.
심부체온을 직접 측정하지 않는다.
센서 밀착 상태와 외부 열원 영향을 받는다.
```

---

## 8. 출력 장치 테스트

### 8-1. RGB LED

| 위험도       | 기대 출력 | 결과          |
| --------- | ----- | ----------- |
| SAFE      | 초록    | PASS / FAIL |
| CAUTION   | 노랑    | PASS / FAIL |
| DANGER    | 빨강    | PASS / FAIL |
| EMERGENCY | 빨강 점멸 | PASS / FAIL |

---

### 8-2. 진동모터

| 위험도       | 기대 출력    | 결과          |
| --------- | -------- | ----------- |
| SAFE      | OFF      | PASS / FAIL |
| CAUTION   | 짧은 진동 1회 | PASS / FAIL |
| DANGER    | 반복 진동    | PASS / FAIL |
| EMERGENCY | 강한 반복 진동 | PASS / FAIL |

---

### 8-3. 부저

| 위험도       | 기대 출력    | 결과          |
| --------- | -------- | ----------- |
| SAFE      | OFF      | PASS / FAIL |
| CAUTION   | 짧은 부저 1회 | PASS / FAIL |
| DANGER    | 느린 반복 부저 | PASS / FAIL |
| EMERGENCY | 빠른 반복 부저 | PASS / FAIL |

---

## 9. ESP32 통합 펌웨어 테스트

| 테스트 항목            | 기대 결과                    | 결과          |
| ----------------- | ------------------------ | ----------- |
| 펌웨어 업로드           | 업로드 성공                   | PASS / FAIL |
| 시리얼 출력            | 115200 baud에서 로그 출력      | PASS / FAIL |
| 센서 초기화            | 연결된 센서 초기화 성공            | PASS / FAIL |
| 센서 fallback       | 미연결 센서에서 앱이 죽지 않도록 기본 처리 | PASS / FAIL |
| BLE Advertise     | `SS_0001` 이름으로 광고        | PASS / FAIL |
| Notify payload 생성 | 정해진 형식으로 문자열 생성          | PASS / FAIL |
| Write 명령 수신       | `RISK:*` 명령 처리           | PASS / FAIL |

---

## 10. BLE 통신 테스트

### 10-1. BLE 연결

| 테스트 항목                   | 기대 결과                 | 결과          |
| ------------------------ | --------------------- | ----------- |
| BLE 장치 검색                | `SS_0001` 검색됨         | PASS / FAIL |
| GATT 연결                  | 연결 성공                 | PASS / FAIL |
| Service 탐색               | Service UUID 발견       | PASS / FAIL |
| Notify Characteristic 탐색 | Sensor Notify UUID 발견 | PASS / FAIL |
| Write Characteristic 탐색  | Control Write UUID 발견 | PASS / FAIL |
| CCCD 설정                  | Notify 활성화            | PASS / FAIL |

---

### 10-2. BLE Notify

| 테스트 항목        | 기대 결과          | 결과          |
| ------------- | -------------- | ----------- |
| Notify 수신     | payload 문자열 수신 | PASS / FAIL |
| 수신 주기         | 설정 주기와 유사      | PASS / FAIL |
| payload 깨짐 없음 | 구분자와 필드 유지     | PASS / FAIL |
| 장시간 수신        | 앱 종료 없이 수신 유지  | PASS / FAIL |

---

### 10-3. BLE Write

| 테스트 항목              | 기대 결과              | 결과          |
| ------------------- | ------------------ | ----------- |
| `RISK:SAFE` 전송      | ESP32 SAFE 출력      | PASS / FAIL |
| `RISK:CAUTION` 전송   | ESP32 CAUTION 출력   | PASS / FAIL |
| `RISK:DANGER` 전송    | ESP32 DANGER 출력    | PASS / FAIL |
| `RISK:EMERGENCY` 전송 | ESP32 EMERGENCY 출력 | PASS / FAIL |
| 알 수 없는 명령           | 오류 처리 또는 무시        | PASS / FAIL |

---

## 11. Payload 파싱 테스트

테스트 payload 예시:

```text
ID:0001,TEMP:36.5,HR:102,SPO2:97,ENV:33.1,HUM:71,LUX:45000,AX:0.12,AY:-0.08,AZ:9.78,POSTURE:NORMAL
```

| 테스트 항목          | 기대 결과      | 결과          |
| --------------- | ---------- | ----------- |
| ID 파싱           | `0001`     | PASS / FAIL |
| TEMP 파싱         | `36.5`     | PASS / FAIL |
| HR 파싱           | `102`      | PASS / FAIL |
| SPO2 파싱         | `97`       | PASS / FAIL |
| ENV 파싱          | `33.1`     | PASS / FAIL |
| HUM 파싱          | `71`       | PASS / FAIL |
| LUX 파싱          | `45000`    | PASS / FAIL |
| AX / AY / AZ 파싱 | 각각 Float 값 | PASS / FAIL |
| POSTURE 파싱      | `NORMAL`   | PASS / FAIL |

---

## 12. Payload 예외 테스트

| 입력 상황        | 기대 처리                  | 결과          |
| ------------ | ---------------------- | ----------- |
| 일부 필드 누락     | 앱 종료 없이 기본값 또는 null 처리 | PASS / FAIL |
| 숫자 변환 실패     | 해당 필드 무효 처리            | PASS / FAIL |
| 알 수 없는 필드 추가 | 무시 또는 로그 처리            | PASS / FAIL |
| 마지막 쉼표 포함    | 가능하면 안전 처리             | PASS / FAIL |
| 빈 payload    | 무효 처리                  | PASS / FAIL |
| HR:0         | 무효 또는 신뢰도 낮음 처리        | PASS / FAIL |
| SPO2:0       | 무효 또는 신뢰도 낮음 처리        | PASS / FAIL |
| TEMP:-127    | 무효 처리                  | PASS / FAIL |

---

## 13. 위험도 알고리즘 테스트

### 13-1. 정상 상태

| 조건                           | 기대 위험도 | 결과          |
| ---------------------------- | ------ | ----------- |
| 정상 환경, 정상 HR, 정상 TEMP, 정상 자세 | SAFE   | PASS / FAIL |

---

### 13-2. 주의 상태

| 조건                 | 기대 위험도     | 결과          |
| ------------------ | ---------- | ----------- |
| 고온 또는 고습 단독        | CAUTION 후보 | PASS / FAIL |
| 피부 접촉 온도 소폭 상승     | CAUTION 후보 | PASS / FAIL |
| HR이 baseline 대비 상승 | CAUTION 후보 | PASS / FAIL |

---

### 13-3. 위험 상태

| 조건              | 기대 위험도    | 결과          |
| --------------- | --------- | ----------- |
| 고온·고습 + TEMP 상승 | DANGER 후보 | PASS / FAIL |
| 고온·고습 + HR 상승   | DANGER 후보 | PASS / FAIL |
| 활동량 낮음 + HR 과상승 | DANGER 후보 | PASS / FAIL |
| 불안정 자세 지속       | DANGER 후보 | PASS / FAIL |

---

### 13-4. 응급 상태

| 조건                       | 기대 위험도       | 결과          |
| ------------------------ | ------------ | ----------- |
| POSTURE:EMERGENCY        | EMERGENCY 후보 | PASS / FAIL |
| POSTURE:FALL + 움직임 없음    | EMERGENCY 후보 | PASS / FAIL |
| SpO2 저하 + 움직임 없음 + HR 이상 | EMERGENCY 후보 | PASS / FAIL |
| 위험 상태 장시간 지속             | EMERGENCY 후보 | PASS / FAIL |

---

## 14. 위험도 유지 정책 테스트

| 테스트 항목     | 기대 결과            | 결과          |
| ---------- | ---------------- | ----------- |
| 순간 튐 값     | 즉시 위험도 상승하지 않음   | PASS / FAIL |
| CAUTION 지속 | 일정 시간 유지         | PASS / FAIL |
| DANGER 지속  | SAFE로 즉시 복귀하지 않음 | PASS / FAIL |
| 정상값 지속     | 일정 시간 후 SAFE 복귀  | PASS / FAIL |

---

## 15. Firebase 업로드 테스트

### 15-1. currentStatus

| 테스트 항목              | 기대 결과                            | 결과          |
| ------------------- | -------------------------------- | ----------- |
| workerId 저장         | `0001` 저장                        | PASS / FAIL |
| deviceName 저장       | `SS_0001` 저장                     | PASS / FAIL |
| workLocationCode 저장 | 선택한 위치 코드 저장                     | PASS / FAIL |
| workLocationName 저장 | 선택한 위치 이름 저장                     | PASS / FAIL |
| 센서값 저장              | temp/hr/spo2/env/hum/lux 저장      | PASS / FAIL |
| posture 저장          | 현재 자세 저장                         | PASS / FAIL |
| riskLevel 저장        | 현재 위험도 저장                        | PASS / FAIL |
| riskCommand 저장      | ESP32 명령 저장                      | PASS / FAIL |
| bleConnected 저장     | BLE 연결 상태 저장                     | PASS / FAIL |
| bleSignalLevel 저장   | GOOD/NORMAL/WEAK/DISCONNECTED 저장 | PASS / FAIL |
| updatedAt 저장        | 현재 시간 저장                         | PASS / FAIL |

---

### 15-2. riskLogs

| 테스트 항목           | 기대 결과                    | 결과          |
| ---------------- | ------------------------ | ----------- |
| CAUTION 이상 로그 생성 | 설정 기준에 따라 생성             | PASS / FAIL |
| DANGER 로그 생성     | 위험 발생 시 생성               | PASS / FAIL |
| EMERGENCY 로그 생성  | 응급 발생 시 생성               | PASS / FAIL |
| createdAt 저장     | 로그 생성 시간 저장              | PASS / FAIL |
| 작업 위치 포함         | workLocationCode/name 포함 | PASS / FAIL |
| 센서값 포함           | 당시 센서값 포함                | PASS / FAIL |

---

## 16. 관리자 앱 테스트

### 16-1. 메인 화면

| 테스트 항목      | 기대 결과                  | 결과          |
| ----------- | ---------------------- | ----------- |
| 작업자 목록 표시   | Firebase workers 목록 표시 | PASS / FAIL |
| 위험도 표시      | riskLevel 표시           | PASS / FAIL |
| 위험도별 정렬     | EMERGENCY 우선 표시        | PASS / FAIL |
| 작업 위치 표시    | workLocationName 표시    | PASS / FAIL |
| BLE 상태 표시   | 연결/끊김 표시               | PASS / FAIL |
| 마지막 업데이트 표시 | updatedAt 변환 표시        | PASS / FAIL |

---

### 16-2. 상세 화면

| 테스트 항목      | 기대 결과                       | 결과          |
| ----------- | --------------------------- | ----------- |
| 작업자 ID 표시   | workerId 표시                 | PASS / FAIL |
| 장치 이름 표시    | deviceName 표시               | PASS / FAIL |
| 센서값 표시      | temp/hr/spo2/env/hum/lux 표시 | PASS / FAIL |
| 자세 표시       | posture 표시                  | PASS / FAIL |
| 위험 로그 표시    | riskLogs 표시                 | PASS / FAIL |
| BLE RSSI 표시 | bleRssi 표시                  | PASS / FAIL |

---

## 17. BLE 위치별 안정성 테스트

BLE 성능은 휴대폰 위치와 인체 차폐에 영향을 받는다.

각 조건에서 최소 1분 이상 측정한다.

| 조건           | RSSI 평균 | 누락 패킷 수 | 끊김 횟수 | 재연결 시간 | 결과          |
| ------------ | ------- | ------- | ----- | ------ | ----------- |
| 휴대폰 손에 든 상태  |         |         |       |        | PASS / FAIL |
| 휴대폰 앞주머니     |         |         |       |        | PASS / FAIL |
| 휴대폰 뒷주머니     |         |         |       |        | PASS / FAIL |
| 휴대폰 가방 안     |         |         |       |        | PASS / FAIL |
| 휴대폰 책상 위     |         |         |       |        | PASS / FAIL |
| ESP32와 같은 몸쪽 |         |         |       |        | PASS / FAIL |
| ESP32와 반대 몸쪽 |         |         |       |        | PASS / FAIL |
| 걷기           |         |         |       |        | PASS / FAIL |
| 팔 움직임        |         |         |       |        | PASS / FAIL |

---

## 18. 재연결 테스트

| 테스트 항목         | 기대 결과              | 결과          |
| -------------- | ------------------ | ----------- |
| ESP32 전원 OFF   | 앱이 연결 끊김 감지        | PASS / FAIL |
| ESP32 전원 ON    | 앱이 재연결 시도          | PASS / FAIL |
| 10분 이내 재연결     | 재연결 성공 또는 실패 상태 표시 | PASS / FAIL |
| Firebase 상태 반영 | bleConnected 값 변경  | PASS / FAIL |
| Notify 재활성화    | 재연결 후 데이터 수신 재개    | PASS / FAIL |

---

## 19. Foreground Service 테스트

| 테스트 항목     | 기대 결과                 | 결과          |
| ---------- | --------------------- | ----------- |
| 작업 시작      | Foreground Service 시작 | PASS / FAIL |
| 알림 표시      | 작업 중 알림 표시            | PASS / FAIL |
| 앱 백그라운드 이동 | 작업 상태 유지              | PASS / FAIL |
| 작업 종료      | Foreground Service 종료 | PASS / FAIL |
| 앱 종료 후 처리  | 정책에 맞게 세션 종료 또는 유지    | PASS / FAIL |

---

## 20. 최종 착용 상태 통합 테스트

최종 시연 전 실제 착용 상태에서 전체 흐름을 확인한다.

| 테스트 항목       | 기대 결과             | 결과          |
| ------------ | ----------------- | ----------- |
| 장치 착용        | 센서값 안정            | PASS / FAIL |
| 작업자 앱 연결     | BLE 연결 성공         | PASS / FAIL |
| 작업 위치 선택     | Firebase에 위치 저장   | PASS / FAIL |
| 센서 데이터 수신    | payload 정상 수신     | PASS / FAIL |
| 위험도 계산       | UI에 위험도 표시        | PASS / FAIL |
| ESP32 출력 제어  | 위험도별 LED/진동/부저 동작 | PASS / FAIL |
| Firebase 업로드 | currentStatus 갱신  | PASS / FAIL |
| 관리자 앱 표시     | 작업자 상태 표시         | PASS / FAIL |
| 연결 끊김 처리     | 상태 표시 및 재연결 시도    | PASS / FAIL |

---

## 21. 테스트 결과 기록 양식

```text
테스트 일자:
테스트 장소:
테스트 담당자:
ESP32 보드:
Android 기기:
Android 버전:
작업자 앱 버전:
관리자 앱 버전:
펌웨어 버전:
Firebase 프로젝트:

테스트 항목:
테스트 조건:
기대 결과:
실제 결과:
PASS / FAIL:
문제 내용:
수정 필요 사항:
재테스트 결과:
```

---

## 22. 최종 제출 전 체크리스트

```text
README 문서가 최신 코드 구조와 일치하는가?
BLE UUID가 앱과 ESP32에서 동일한가?
BLE payload 형식이 문서와 코드에서 동일한가?
위험도 명령이 SAFE / CAUTION / DANGER / EMERGENCY 4단계로 통일되었는가?
Firebase 경로가 workers/{workerId}/currentStatus, workers/{workerId}/riskLogs/{logId}로 통일되었는가?
작업자 ID가 BLE 이름, payload, Firebase에서 동일한가?
관리자 앱이 BLE 기능을 수행하지 않도록 역할이 분리되었는가?
의료기기처럼 보이는 표현이 README와 발표 자료에서 제거되었는가?
센서 fallback 값이 실제 측정값처럼 설명되지 않았는가?
최종 착용 테스트를 1회 이상 수행했는가?
```

---

## 23. 최종 요약

Smart Shield 최종 검증은 하드웨어, BLE, 작업자 앱, Firebase, 관리자 앱을 순서대로 확인해야 한다.

핵심 검증 포인트는 다음과 같다.

```text
센서값이 정상적으로 수집되는가?
BLE Notify payload가 앱으로 안정적으로 전달되는가?
작업자 앱이 payload를 안전하게 파싱하는가?
위험도 계산 결과가 ESP32 명령과 Firebase 데이터로 일관되게 반영되는가?
관리자 앱이 Firebase 데이터를 정확히 읽어 표시하는가?
휴대폰 위치와 착용 조건 변화에도 BLE 연결이 유지되는가?
```