# Integration Test Code

센서 통합 테스트 폴더입니다. BME280, BH1750, MPU6050, MAX30102, MAX30205를 한 스케치에서 초기화하고 시리얼 모니터로 측정값을 확인합니다.

## 파일 구성

| 파일 | 내용 |
|---|---|
| `Integration_Test_Code.ino` | 센서 통합 테스트 메인 스케치 |
| `compile.ps1` | Arduino CLI 컴파일 스크립트 |
| `upload.ps1` | ESP32 업로드 스크립트 |
| `monitor.ps1` | 시리얼 모니터 실행 스크립트 |

## 테스트 대상

| 센서 | 주소 | 확인 내용 |
|---|---|---|
| BME280 | `0x76`, `0x77` | 온도, 습도, 기압 |
| BH1750 | `0x23`, `0x5C` | 조도 |
| MPU6050 | `0x68`, `0x69` | 가속도, 자이로 |
| MAX30102 | `0x57` | RED, IR 원시값 |
| MAX30205 | `0x48` | 피부 접촉 온도 |

## 주요 흐름

```text
setup
  Serial 시작
  Wire 시작
  I2C 주소 스캔
  센서별 초기화
  센서 인식 결과 출력

loop
  BME280 측정값 출력
  BH1750 측정값 출력
  MPU6050 측정값 출력
  MAX30102 측정값 출력
  MAX30205 측정값 출력
```

## 스크립트 실행

PowerShell에서 이 폴더로 이동한 뒤 실행합니다.

```powershell
.\compile.ps1
.\upload.ps1 -Port COM8
.\monitor.ps1 -Port COM8
```

직접 Arduino CLI를 사용할 수도 있습니다.

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 .
arduino-cli upload --fqbn esp32:esp32:esp32 -p COM8 .
arduino-cli monitor -p COM8 -c baudrate=115200
```

`COM8`은 예시입니다. 현재 ESP32가 연결된 포트로 바꿔 실행합니다.

## 외부 의존성

| 항목 | 설명 |
|---|---|
| Arduino CLI | 컴파일, 업로드, 시리얼 모니터 실행 |
| ESP32 board package | `esp32:esp32:esp32` |
| 센서 라이브러리 | BME280, BH1750, MPU6050, MAX30105 계열 라이브러리 |
| 실제 센서 | 테스트 대상 센서가 I2C 버스에 연결되어 있어야 함 |

## 검증 기준

- I2C 스캔 결과가 예상 주소와 일치하는지 확인합니다.
- 각 센서 초기화 로그가 성공으로 표시되는지 확인합니다.
- 시리얼 모니터에서 값이 반복 출력되는지 확인합니다.
- 값이 고정되거나 비정상 범위이면 배선, 전원, 센서 주소를 먼저 점검합니다.

## 주의사항

- 이 스케치는 BLE와 Firebase를 검증하지 않습니다.
- MAX30102 값은 원시값 확인 중심이며 의료용 HR/SpO2 검증이 아닙니다.
- 최종 펌웨어 동작은 `HW/SmartShield_ESP32`에서 다시 확인합니다.

## 관련 문서

| 문서 | 설명 |
|---|---|
| [TestCode README](../README.md) | 하드웨어 테스트 순서 |
| [Sensor README](../Sensor/README.md) | 센서 단독 테스트 기준 |
| [최종 펌웨어 README](../../SmartShield_ESP32/README.md) | 최종 ESP32 동작 |
