# Smart Shield Docs

이 폴더는 Smart Shield 프로젝트의 통신 규격, 데이터 구조, 위험도 기준, 검증 절차를 정리한 문서를 포함합니다.

## 이 폴더의 역할

코드만으로 확인하기 어려운 시스템 규격과 시연·검증 기준을 문서로 관리합니다. 최상단 README에는 전체 요약만 두고, 세부 규격은 이 폴더의 문서에서 확인합니다.

## 주요 파일

| 파일 | 역할 |
|---|---|
| `SYSTEM_ARCHITECTURE.md` | 전체 시스템 구성과 데이터 흐름 |
| `BLE_PROTOCOL.md` | ESP32와 작업자 앱 사이 BLE Notify/Write 규격 |
| `FIREBASE_SCHEMA.md` | Firebase Realtime Database 저장 구조 |
| `RISK_ALGORITHM.md` | 위험도 판단 기준과 표현 주의사항 |
| `MEASUREMENT_VERIFICATION.md` | 센서, BLE, Firebase, 출력 장치 실측 검증 체크리스트 템플릿 |
| `DEMO_VERIFICATION_RESULT.md` | 빌드·시연·부분 검증·미검증 상태 요약 |
| `TEST_PLAN.md` | 테스트 시나리오와 확인 항목 |
| `FINAL_CHECKLIST.md` | 최종 점검 체크리스트 |
| `OwnedParts.md` | 보유 부품과 하드웨어 구성 참고 자료 |
| `최종 코드.txt` | 발표 또는 제출용으로 정리된 코드 참고 자료 |

## 읽는 순서

```text
SYSTEM_ARCHITECTURE.md
→ BLE_PROTOCOL.md
→ FIREBASE_SCHEMA.md
→ RISK_ALGORITHM.md
→ MEASUREMENT_VERIFICATION.md
→ DEMO_VERIFICATION_RESULT.md
→ TEST_PLAN.md
→ FINAL_CHECKLIST.md
```

## 사용 방법

구현 중에는 코드와 문서가 맞는지 함께 확인합니다. BLE UUID, Firebase 필드명, 위험도 기준이 바뀌면 관련 문서를 먼저 수정하고 최상단 README에는 필요한 요약만 반영합니다.

## 외부 의존성

| 항목 | 설명 |
|---|---|
| ESP32 펌웨어 | BLE payload와 출력 제어 기준 |
| 작업자 Android 앱 | 위험도 계산과 Firebase 업로드 기준 |
| 관리자 Android 앱 | Firebase 조회와 화면 표시 기준 |
| 실제 하드웨어 | 실측 검증과 최종 체크리스트 기준 |

## 주요 설정값

자주 확인하는 값은 각 세부 문서에 분리되어 있습니다.

| 설정 | 확인 문서 |
|---|---|
| BLE UUID와 payload | `BLE_PROTOCOL.md` |
| Firebase 경로와 필드 | `FIREBASE_SCHEMA.md` |
| 위험도 기준 | `RISK_ALGORITHM.md` |
| 실측 확인 항목 | `MEASUREMENT_VERIFICATION.md` |
| 검증 결과 요약 | `DEMO_VERIFICATION_RESULT.md` |

## 테스트 방법

문서 자체는 실행 대상이 아닙니다. 실제 검증은 Android 앱 빌드, ESP32 펌웨어 컴파일, 실제 장치 시연 결과를 기준으로 확인합니다. 검증 체크리스트는 `MEASUREMENT_VERIFICATION.md`, 결과 요약은 `DEMO_VERIFICATION_RESULT.md`, 테스트 기준은 `TEST_PLAN.md`와 `FINAL_CHECKLIST.md`의 항목과 대조합니다.

## 주의사항

- 문서 내용은 현재 코드 기준과 맞아야 합니다.
- 검증하지 않은 기능은 완료된 것처럼 작성하지 않습니다.
- 발표용 표현은 의료 진단처럼 보이지 않도록 `RISK_ALGORITHM.md`의 표현 기준을 따릅니다.
- `최종 코드.txt`는 참고용 자료이며 실제 빌드 기준은 `HW/`와 `SW/`의 소스 코드입니다.

## 관련 상위 문서

| 문서 | 설명 |
|---|---|
| [최상단 README](../README.md) | 프로젝트 전체 요약 |
| [HW README](../HW/README.md) | 하드웨어 펌웨어와 테스트 코드 |
| [SW README](../SW/README.md) | Android 앱 구성 |
