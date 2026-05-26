# Smart Shield SW

이 폴더는 Smart Shield Android 앱 두 개를 포함합니다.

## 앱 구성

| 경로 | 역할 |
|---|---|
| `HNU_PPE_Control/` | 작업자 앱 |
| `HNU_PPE_Manager/` | 관리자 앱 |

## 작업자 앱 역할

작업자 앱은 ESP32와 BLE로 직접 연결되는 앱입니다.

- BLE 장치 검색 및 연결
- BLE Notify payload 수신
- 센서 데이터 파싱
- 온열 위험 가능성 및 자세 이상 위험도 계산
- 작업자 화면 표시
- 위험 단계별 팝업/진동 처리
- ESP32로 `RISK:*` 명령 전송
- Firebase `currentStatus`, `riskLogs` 업로드

## 관리자 앱 역할

관리자 앱은 Firebase 데이터를 읽어 표시하는 앱입니다.

- 작업자 현재 상태 조회
- 위험 작업자 우선 표시
- 구역별 작업자 필터링
- 작업자 상세 상태 확인
- 최초 응급 발생 로그 요약 표시

관리자 앱은 BLE 연결, 센서 payload 파싱, 위험도 계산, ESP32 제어 명령 전송을 수행하지 않습니다.

## 빌드

```powershell
cd d:\HNU\SW\HNU_PPE_Control
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug

cd d:\HNU\SW\HNU_PPE_Manager
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

## 실측 검증

앱 빌드 성공 이후 BLE 연결, Firebase 업로드, 관리자 앱 표시까지 실제 장치와 휴대폰으로 검증해야 합니다. 측정 항목은 `docs/MEASUREMENT_VERIFICATION.md`를 기준으로 기록합니다.
