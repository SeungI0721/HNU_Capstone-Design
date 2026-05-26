# HNU_PPE_Manager

Smart Shield 관리자 Android 앱입니다. 작업자 앱이 Firebase Realtime Database에 업로드한 상태를 읽어 작업자 목록, 위험 작업자, 구역별 상태를 표시합니다.

## 앱 역할

- Firebase `workers` 노드 읽기
- 모니터링 시작 이후 새로 갱신된 작업자 상태 표시
- 위험/응급 작업자 우선 표시
- 구역별 작업자 필터링
- 작업자 상세 상태 확인
- 최초 응급 발생 로그 요약 표시

관리자 앱은 BLE 연결, 센서 payload 파싱, 위험도 계산, ESP32 제어 명령 전송을 수행하지 않습니다.

## 핵심 파일

| 파일 | 역할 |
|---|---|
| `AdminMainActivity.kt` | 모니터링 시작/종료, Firebase 조회, 작업자 목록 표시 |
| `AdminWorkerStatus.kt` | Firebase snapshot을 관리자 화면 모델로 변환 |
| `AdminWorkerAdapter.kt` | 위험 작업자/작업자 목록 RecyclerView 표시 |
| `AdminWorkerDetailActivity.kt` | 작업자 상세 상태 표시 |
| `AdminFirebaseConfig.kt` | Firebase 수동 초기화 |

## 빌드

```powershell
cd d:\HNU\SW\HNU_PPE_Manager
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

## 시연 전 확인

- 작업자 앱에서 `currentStatus`가 갱신되는지 확인
- 관리자 앱에서 모니터링 시작 후 새 데이터만 표시되는지 확인
- 위험/응급 상태가 위험 작업자 영역에 표시되는지 확인
- 최초 응급 로그 시간이 올바르게 표시되는지 확인
