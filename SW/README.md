# Smart Shield SW

이 폴더는 Smart Shield Android 앱 두 개를 포함합니다.

## 이 폴더의 역할

작업자 앱과 관리자 앱의 Android 프로젝트를 분리해 관리합니다. BLE 수신과 위험도 계산은 작업자 앱에서 수행하고, Firebase 조회 기반 모니터링은 관리자 앱에서 수행합니다.

## 앱 구성

| 경로 | 역할 |
|---|---|
| `HNU_PPE_Control/` | 작업자 앱 |
| `HNU_PPE_Manager/` | 관리자 앱 |

## 작업자 앱 역할

`HNU_PPE_Control`은 ESP32와 BLE로 직접 연결되는 앱입니다.

- BLE 권한 요청
- `SS_0001` 형식의 ESP32 장치 스캔 및 연결
- GATT 연결, MTU 요청, Notify 구독
- ESP32 Notify payload 파싱
- `TEMP_VALID`, `TEMP_SOURCE` 기반 피부 온도 유효성 처리
- 작업 시작 기준 온도 대비 변화 추적
- 온열 위험도와 자세 위험도 계산
- 작업자 UI 표시
- 위험 단계별 앱 알림 및 진동 처리
- ESP32로 `RISK:*` 명령 전송
- Firebase `currentStatus`, `riskLogs` 업로드
- Foreground Service 기반 작업 세션 유지

## 관리자 앱 역할

`HNU_PPE_Manager`는 Firebase 데이터를 읽어 표시하는 모니터링 앱입니다.

- Firebase `workers` 노드 조회
- 작업자 현재 상태 목록 표시
- 위험/응급 작업자 우선 표시
- 작업 구역별 필터링
- 작업자 상세 상태 확인
- 최초 응급 발생 로그 요약 표시

관리자 앱은 BLE 연결, 센서 payload 파싱, 위험도 계산, ESP32 제어 명령 전송을 수행하지 않습니다. 이 작업은 작업자 앱이 담당합니다.

## 빌드

각 앱 폴더에서 Gradle wrapper를 실행합니다.

```powershell
cd SW\HNU_PPE_Control
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest

cd ..\HNU_PPE_Manager
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

## 외부 의존성

| 항목 | 현재 코드 기준 |
|---|---|
| Android SDK | 앱 빌드와 실행에 필요 |
| Kotlin / Android Gradle Plugin | 각 앱의 Gradle 설정 기준 |
| Firebase Realtime Database | 작업자 상태 업로드와 관리자 조회 |
| Android 기기 | BLE 테스트는 실제 기기에서 확인 필요 |

## 주요 설정값

| 항목 | 값 |
|---|---|
| 작업자 앱 | `HNU_PPE_Control` |
| 관리자 앱 | `HNU_PPE_Manager` |
| Firebase 기준 경로 | `workers/{workerId}` |
| BLE 대상 장치명 | `SS_0001` 형식 |

## 검증

앱 빌드 성공 뒤 실제 Android 기기에서 다음 흐름을 확인합니다.

1. 작업자 앱이 ESP32를 스캔하고 연결하는지 확인
2. Notify payload가 UI에 반영되는지 확인
3. 위험도 변화가 앱 알림, Firebase, ESP32 출력에 반영되는지 확인
4. 관리자 앱에서 Firebase 상태가 목록과 상세 화면에 표시되는지 확인

측정 및 시연 체크리스트는 `docs/MEASUREMENT_VERIFICATION.md`를 기준으로 기록합니다.

## 주의사항

- Windows에서 프로젝트 경로에 한글이 포함되면 Android Gradle Plugin 경로 검사로 빌드가 중단될 수 있습니다.
- BLE 스캔과 연결 검증은 에뮬레이터가 아니라 실제 Android 기기 기준으로 확인합니다.
- Firebase 보안 규칙과 인증 정책은 배포 전에 별도 검토가 필요합니다.

## 관련 문서

| 문서 | 설명 |
|---|---|
| [최상단 README](../README.md) | 전체 시스템 구조 |
| [작업자 앱 README](HNU_PPE_Control/README.md) | BLE 수신과 위험도 계산 |
| [관리자 앱 README](HNU_PPE_Manager/README.md) | Firebase 조회와 모니터링 |
| [Firebase 스키마](../docs/FIREBASE_SCHEMA.md) | DB 구조 |
