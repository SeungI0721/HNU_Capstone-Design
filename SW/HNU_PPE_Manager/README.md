# HNU_PPE_Manager

Smart Shield 관리자 Android 앱입니다. Firebase Realtime Database에 저장된 작업자 상태를 읽어 작업자 목록, 위험 작업자, 상세 상태를 표시합니다.

관리자 앱은 ESP32와 직접 BLE로 연결하지 않습니다. 센서 수신, payload 파싱, 위험도 계산, ESP32 출력 제어는 작업자 앱이 담당합니다.

## 이 폴더의 역할

```text
Firebase workers 조회
→ 작업자 목록 표시
→ 위험 작업자 우선 표시
→ 작업 위치 필터링
→ 작업자 상세 상태 표시
```

## 주요 파일

| 파일 | 역할 |
|---|---|
| `app/src/main/java/com/example/hnu_ppe_manager/AdminMainActivity.kt` | 관리자 메인 화면, 모니터링 시작·중지, 목록 갱신 |
| `app/src/main/java/com/example/hnu_ppe_manager/AdminWorkerDetailActivity.kt` | 개별 작업자 상세 상태 화면 |
| `app/src/main/java/com/example/hnu_ppe_manager/AdminWorkerStatus.kt` | Firebase 스냅샷을 화면 표시용 모델로 변환 |
| `app/src/main/java/com/example/hnu_ppe_manager/AdminWorkerAdapter.kt` | 작업자 목록 RecyclerView 표시 |
| `app/src/main/java/com/example/hnu_ppe_manager/AdminFirebaseConfig.kt` | Firebase 수동 초기화 설정 |
| `app/src/main/res/layout/activity_admin_main.xml` | 관리자 메인 화면 레이아웃 |
| `app/src/main/res/layout/activity_admin_worker_detail.xml` | 작업자 상세 화면 레이아웃 |
| `app/build.gradle.kts` | 앱 모듈 빌드 설정 |

## 코드 흐름

```text
AdminMainActivity.onCreate()
  Firebase 초기화
  화면과 RecyclerView 바인딩
  위치 필터와 빈 목록 상태 표시

모니터링 시작
  workers 노드 1회 조회
  workers 노드 실시간 리스너 등록
  15초 주기 보조 갱신 시작

Firebase 데이터 수신
  currentStatus가 있는 작업자만 읽음
  AdminWorkerStatus로 변환
  위험도와 갱신 시각 기준 정렬
  위험 작업자 영역과 전체 목록 갱신

작업자 선택
  AdminWorkerDetailActivity 실행
  workers/{workerId}/currentStatus 실시간 표시
```

## Firebase 의존성

관리자 앱은 다음 경로를 읽습니다.

```text
workers/{workerId}/currentStatus
workers/{workerId}/riskLogs/{logId}
```

`currentStatus`는 작업자 목록과 상세 화면의 기준 데이터입니다. `riskLogs`는 모니터링 시작 이후 최초 응급 로그를 위험 작업자 목록에 표시할 때 사용합니다.

## 주요 설정값

관리자 앱은 공개 저장소에 Firebase API Key를 직접 저장하지 않습니다. 빌드 또는 실행 환경에서 다음 Gradle 속성이나 환경 변수를 설정해야 Firebase 수동 초기화가 동작합니다.

| 항목 | 설정 이름 |
|---|---|
| Firebase Database URL | `HNU_PPE_FIREBASE_DATABASE_URL` |
| Firebase API Key | `HNU_PPE_FIREBASE_API_KEY` |
| Firebase Application ID | `HNU_PPE_FIREBASE_APPLICATION_ID` |
| Firebase Project ID | `HNU_PPE_FIREBASE_PROJECT_ID` |
| 모니터링 보조 갱신 주기 | `15_000 ms` |
| 전체 위치 필터 표시명 | `전체` |
| 상세 화면 기준 경로 | `workers/{workerId}/currentStatus` |

로컬 개발에서는 Git에 올리지 않는 환경 변수 또는 개인 Gradle 속성으로 위 값을 주입합니다.

## 실행 및 빌드

관리자 앱 폴더에서 Gradle 명령을 실행합니다.

```powershell
cd SW\HNU_PPE_Manager
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Android 기기 또는 에뮬레이터에서 실행하려면 Firebase Realtime Database 접근이 가능한 네트워크 환경이 필요합니다.

## 테스트 방법

1. 작업자 앱에서 ESP32 연결 또는 가짜 데이터 흐름으로 `currentStatus`를 업로드합니다.
2. 관리자 앱을 실행합니다.
3. 모니터링 시작 버튼을 누릅니다.
4. 작업자 목록에 `workers/{workerId}/currentStatus` 데이터가 표시되는지 확인합니다.
5. 위험도가 `위험` 또는 `응급`인 작업자가 위험 작업자 영역에 표시되는지 확인합니다.
6. 작업자 항목을 눌러 상세 화면 값이 Firebase와 일치하는지 확인합니다.

## 주의사항

- 관리자 앱은 위험도를 새로 계산하지 않고 작업자 앱이 업로드한 값을 표시합니다.
- BLE 연결 상태와 센서값은 작업자 앱의 업로드 주기와 Firebase 연결 상태에 따라 지연될 수 있습니다.
- Firebase 보안 규칙과 인증 정책은 실제 배포 환경에서 별도 검증이 필요합니다.
- Firebase 설정값이 비어 있으면 앱은 빌드되지만 Firebase 초기화와 데이터 조회는 동작하지 않습니다.
- 현재 앱은 `workers` 노드를 읽는 구조이므로 작업자 수가 늘어나면 조회 범위와 인덱스 전략을 재검토해야 합니다.

## 관련 문서

| 문서 | 설명 |
|---|---|
| [상위 SW README](../README.md) | Android 앱 전체 구성 |
| [작업자 앱 README](../HNU_PPE_Control/README.md) | Firebase 업로드와 위험도 계산 주체 |
| [Firebase 스키마](../../docs/FIREBASE_SCHEMA.md) | `workers` 데이터 구조 |
| [최상단 README](../../README.md) | 전체 시스템 구조 |
