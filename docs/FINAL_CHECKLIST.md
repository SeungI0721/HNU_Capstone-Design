
# Smart Shield Final Checklist

## 1. 문서 목적

이 문서는 Smart Shield 프로젝트를 최종 제출하거나 발표하기 전 확인해야 할 항목을 정리한다.

확인 범위는 다음과 같다.

```text
README 문서
폴더 구조
ESP32 펌웨어
Android 작업자 앱
Android 관리자 앱
BLE 통신
Firebase 구조
센서 데이터
위험도 알고리즘
최종 테스트
Git / 제출 파일 정리
````

---

## 2. 문서 확인

| 항목                                                            | 확인          |
| ------------------------------------------------------------- | ----------- |
| 최상위 `README.md`가 프로젝트 전체 구조를 설명하는가?                           | PASS / FAIL |
| `HW/README.md`가 하드웨어 구성과 테스트 순서를 설명하는가?                       | PASS / FAIL |
| `HW/SmartShield_ESP32/README.md`가 펌웨어, BLE, 센서, 명령 구조를 설명하는가? | PASS / FAIL |
| `SW/README.md`가 작업자 앱과 관리자 앱 역할을 구분하는가?                       | PASS / FAIL |
| `SW/HNU_PPE_Control/README.md`가 작업자 앱 기능을 설명하는가?              | PASS / FAIL |
| `SW/HNU_PPE_Manager/README.md`가 관리자 앱 기능을 설명하는가?              | PASS / FAIL |
| `docs/BLE_PROTOCOL.md`가 BLE 규격을 설명하는가?                        | PASS / FAIL |
| `docs/FIREBASE_SCHEMA.md`가 Firebase 구조를 설명하는가?                | PASS / FAIL |
| `docs/RISK_ALGORITHM.md`가 위험도 판단 기준을 설명하는가?                   | PASS / FAIL |
| `docs/TEST_PLAN.md`가 검증 계획을 설명하는가?                            | PASS / FAIL |

---

## 3. 금지 표현 확인

README, 발표 자료, 보고서에서 다음 표현을 사용하지 않는다.

```text
정확한 체온 측정
의료급 산소포화도 측정
온열질환 진단
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

## 4. 폴더 구조 확인

권장 최종 구조:

```text
HNU_Capstone-Design/
├─ README.md
├─ docs/
│  ├─ SYSTEM_ARCHITECTURE.md
│  ├─ BLE_PROTOCOL.md
│  ├─ FIREBASE_SCHEMA.md
│  ├─ RISK_ALGORITHM.md
│  ├─ TEST_PLAN.md
│  └─ FINAL_CHECKLIST.md
├─ HW/
│  ├─ README.md
│  ├─ SmartShield_ESP32/
│  │  ├─ README.md
│  │  └─ SmartShield_ESP32.ino
│  └─ TestCode/
│     ├─ README.md
│     ├─ Sensor/
│     ├─ Module/
│     ├─ Integration_Test_Code/
│     └─ Warning_Integration_Test_Code/
└─ SW/
   ├─ README.md
   ├─ HNU_PPE_Control/
   │  ├─ README.md
   │  └─ app/
   └─ HNU_PPE_Manager/
      ├─ README.md
      └─ app/
```

확인 항목:

| 항목                 | 확인          |
| ------------------ | ----------- |
| 최상위 README 존재      | PASS / FAIL |
| `docs/` 폴더 존재      | PASS / FAIL |
| `HW/` 폴더 존재        | PASS / FAIL |
| `SW/` 폴더 존재        | PASS / FAIL |
| 최종 ESP32 펌웨어 위치 명확 | PASS / FAIL |
| 작업자 앱 위치 명확        | PASS / FAIL |
| 관리자 앱 위치 명확        | PASS / FAIL |

---

## 5. ESP32 펌웨어 확인

| 항목                                   | 확인          |
| ------------------------------------ | ----------- |
| ESP32 보드 업로드 성공                      | PASS / FAIL |
| 시리얼 모니터 출력 확인                        | PASS / FAIL |
| BLE 장치 이름이 `SS_0001` 형식인가?           | PASS / FAIL |
| Service UUID가 앱과 동일한가?               | PASS / FAIL |
| Notify Characteristic UUID가 앱과 동일한가? | PASS / FAIL |
| Write Characteristic UUID가 앱과 동일한가?  | PASS / FAIL |
| 센서 payload 형식이 문서와 일치하는가?            | PASS / FAIL |
| `RISK:SAFE` 명령 처리 가능                 | PASS / FAIL |
| `RISK:CAUTION` 명령 처리 가능              | PASS / FAIL |
| `RISK:DANGER` 명령 처리 가능               | PASS / FAIL |
| `RISK:EMERGENCY` 명령 처리 가능            | PASS / FAIL |
| `RISK:ERROR`를 공식 위험도로 사용하지 않는가?      | PASS / FAIL |

---

## 6. 센서 확인

| 센서                     | 확인 항목                       | 확인          |
| ---------------------- | --------------------------- | ----------- |
| BME280                 | ENV / HUM / PRESS 출력        | PASS / FAIL |
| BH1750                 | LUX 출력                      | PASS / FAIL |
| MPU6050                | AX / AY / AZ / POSTURE 출력   | PASS / FAIL |
| MAX30102 / SEN0344     | HR / SPO2 출력 또는 fallback 처리 | PASS / FAIL |
| MAX30205 / Fever Click | TEMP 출력 또는 fallback 처리      | PASS / FAIL |

주의:

```text
fallback 값은 실제 측정값이 아니다.
센서 미연결 상태에서 출력되는 테스트값을 실제 센서값처럼 설명하지 않는다.
```

---

## 7. I2C 주소 확인

| 센서                     | 예상 주소                     | 확인          |
| ---------------------- | ------------------------- | ----------- |
| BME280                 | `0x76` 또는 `0x77`          | PASS / FAIL |
| BH1750                 | `0x23` 또는 `0x5C`          | PASS / FAIL |
| MPU6050                | `0x68` 또는 `0x69`          | PASS / FAIL |
| MAX30102 / SEN0344     | `0x57`                    | PASS / FAIL |
| MAX30205 / Fever Click | `0x48` 또는 `0x49` 계열 확인 필요 | PASS / FAIL |

---

## 8. 작업자 앱 확인

| 항목                            | 확인          |
| ----------------------------- | ----------- |
| 앱 빌드 성공                       | PASS / FAIL |
| 앱 실행 성공                       | PASS / FAIL |
| Bluetooth 권한 요청 정상            | PASS / FAIL |
| 위치 권한 요청 정상                   | PASS / FAIL |
| Foreground Service 권한 및 알림 정상 | PASS / FAIL |
| ESP32 BLE 장치 검색 가능            | PASS / FAIL |
| ESP32 BLE 연결 가능               | PASS / FAIL |
| BLE Notify 수신 가능              | PASS / FAIL |
| payload 파싱 정상                 | PASS / FAIL |
| 위험도 계산 정상                     | PASS / FAIL |
| UI 값 업데이트 정상                  | PASS / FAIL |
| 위험 단계별 팝업 정상                  | PASS / FAIL |
| 스마트폰 진동 정상                    | PASS / FAIL |
| BLE Write 명령 전송 정상            | PASS / FAIL |
| Firebase currentStatus 업로드 정상 | PASS / FAIL |
| Firebase riskLogs 생성 정상       | PASS / FAIL |
| BLE 끊김 시 UI 반영                | PASS / FAIL |
| BLE 재연결 시도 정상                 | PASS / FAIL |

---

## 9. 관리자 앱 확인

| 항목                           | 확인          |
| ---------------------------- | ----------- |
| 앱 빌드 성공                      | PASS / FAIL |
| 앱 실행 성공                      | PASS / FAIL |
| Firebase 연결 정상               | PASS / FAIL |
| 작업자 목록 표시                    | PASS / FAIL |
| 위험도 표시                       | PASS / FAIL |
| 응급 작업자 우선 표시                 | PASS / FAIL |
| 작업 위치 표시                     | PASS / FAIL |
| BLE 연결 상태 표시                 | PASS / FAIL |
| 센서값 표시                       | PASS / FAIL |
| 작업자 상세 화면 표시                 | PASS / FAIL |
| 위험 로그 표시                     | PASS / FAIL |
| 관리자 앱이 BLE 연결을 수행하지 않음       | PASS / FAIL |
| 관리자 앱이 위험도 계산을 수행하지 않음       | PASS / FAIL |
| 관리자 앱이 Firebase 데이터를 수정하지 않음 | PASS / FAIL |

---

## 10. BLE 프로토콜 확인

| 항목                    | 기준                                     | 확인          |
| --------------------- | -------------------------------------- | ----------- |
| Device Name           | `SS_0001`                              | PASS / FAIL |
| Service UUID          | `089fca17-755f-4578-b8af-ee5e32526b0f` | PASS / FAIL |
| Notify Characteristic | `0000FFF1-0000-1000-8000-00805F9B34FB` | PASS / FAIL |
| Write Characteristic  | `0000FFF2-0000-1000-8000-00805F9B34FB` | PASS / FAIL |
| CCCD                  | `00002902-0000-1000-8000-00805F9B34FB` | PASS / FAIL |
| Notify payload        | `KEY:VALUE` 형식                         | PASS / FAIL |
| Write 명령              | `RISK:*` 형식                            | PASS / FAIL |

---

## 11. Firebase 구조 확인

| 항목                       | 기준                                    | 확인          |
| ------------------------ | ------------------------------------- | ----------- |
| 현재 상태 경로                 | `workers/{workerId}/currentStatus`    | PASS / FAIL |
| 위험 로그 경로                 | `workers/{workerId}/riskLogs/{logId}` | PASS / FAIL |
| workerId                 | 4자리 문자열                               | PASS / FAIL |
| BLE 이름과 workerId 일치      | `SS_0001` ↔ `0001`                    | PASS / FAIL |
| payload ID와 workerId 일치  | `ID:0001` ↔ `0001`                    | PASS / FAIL |
| 관리자 앱에서 currentStatus 읽기 | 가능                                    | PASS / FAIL |
| 관리자 앱에서 riskLogs 읽기      | 가능                                    | PASS / FAIL |

---

## 12. 위험도 알고리즘 확인

| 항목                         | 확인          |
| -------------------------- | ----------- |
| SAFE 단계 계산 가능              | PASS / FAIL |
| CAUTION 단계 계산 가능           | PASS / FAIL |
| DANGER 단계 계산 가능            | PASS / FAIL |
| EMERGENCY 단계 계산 가능         | PASS / FAIL |
| 낙상 후보 처리 가능                | PASS / FAIL |
| 움직임 없음 처리 가능               | PASS / FAIL |
| HR 튐 값 예외 처리 가능            | PASS / FAIL |
| SpO2 단독으로 열스트레스 위험 판단하지 않음 | PASS / FAIL |
| TEMP를 심부체온으로 표현하지 않음       | PASS / FAIL |
| LUX 단독으로 위험도 올리지 않음        | PASS / FAIL |
| 센서값 누락 시 앱이 종료되지 않음        | PASS / FAIL |

---

## 13. BLE 위치별 검증

| 조건           | 확인          |
| ------------ | ----------- |
| 휴대폰 손에 든 상태  | PASS / FAIL |
| 휴대폰 앞주머니     | PASS / FAIL |
| 휴대폰 뒷주머니     | PASS / FAIL |
| 휴대폰 가방 안     | PASS / FAIL |
| 휴대폰 책상 위     | PASS / FAIL |
| ESP32와 같은 몸쪽 | PASS / FAIL |
| ESP32와 반대 몸쪽 | PASS / FAIL |
| 걷기 상태        | PASS / FAIL |
| 팔 움직임 상태     | PASS / FAIL |

기록 항목:

```text
RSSI
Notify 수신 성공률
누락 패킷 수
연결 끊김 횟수
재연결 성공 시간
마지막 데이터 수신 시각
```

---

## 14. 최종 시연 확인

| 항목            | 확인          |
| ------------- | ----------- |
| ESP32 전원 ON   | PASS / FAIL |
| 작업자 앱 실행      | PASS / FAIL |
| BLE 연결 성공     | PASS / FAIL |
| 작업 위치 선택      | PASS / FAIL |
| 작업 시작         | PASS / FAIL |
| 센서값 UI 표시     | PASS / FAIL |
| 위험도 UI 표시     | PASS / FAIL |
| ESP32 LED 출력  | PASS / FAIL |
| ESP32 진동모터 출력 | PASS / FAIL |
| ESP32 부저 출력   | PASS / FAIL |
| Firebase 업로드  | PASS / FAIL |
| 관리자 앱 표시      | PASS / FAIL |
| 작업 종료         | PASS / FAIL |

---

## 15. Git / 제출 파일 정리

### Git에 포함 권장

```text
README.md
docs/
HW/SmartShield_ESP32/
HW/TestCode/
SW/HNU_PPE_Control/app/src/
SW/HNU_PPE_Control/build.gradle.kts
SW/HNU_PPE_Control/settings.gradle.kts
SW/HNU_PPE_Manager/app/src/
SW/HNU_PPE_Manager/build.gradle.kts
SW/HNU_PPE_Manager/settings.gradle.kts
```

### Git에서 제외 권장

```text
.gradle/
build/
**/build/
local.properties
*.iml
captures/
.externalNativeBuild/
.cxx/
.DS_Store
```

### ZIP 제출 시 제외 권장

```text
.git/
.gradle/
**/build/
local.properties
```

---

## 16. Firebase 보안 확인

| 항목                              | 확인          |
| ------------------------------- | ----------- |
| `google-services.json` 공개 여부 확인 | PASS / FAIL |
| Firebase Database Rules 확인      | PASS / FAIL |
| 테스트용 전체 공개 규칙 제거 여부 확인          | PASS / FAIL |
| 쓰기 권한 제한 여부 확인                  | PASS / FAIL |
| 읽기 권한 제한 여부 확인                  | PASS / FAIL |

공개 저장소에 올릴 경우 Firebase 보안 규칙을 반드시 확인한다.

---

## 17. 최종 제출 전 한 줄 결론

아래 문장을 README 또는 발표 자료에 포함할 수 있다.

```text
Smart Shield는 ESP32 기반 웨어러블 장치와 Android 앱을 BLE로 연결하여 작업자의 환경 데이터, 생체 신호, 자세 및 움직임 데이터를 수집하고, 온열질환 위험 가능성과 이상 상태를 조기에 감지하기 위한 산업안전 보조 시스템이다.
```

---

## 18. 최종 체크 결과

```text
최종 점검 일자:
점검자:
전체 상태: PASS / FAIL

남은 문제:
1.
2.
3.

수정 필요 사항:
1.
2.
3.

최종 제출 가능 여부:
가능 / 보류
```