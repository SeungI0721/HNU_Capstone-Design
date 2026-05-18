# Integration Test Code

센서 통합 테스트 폴더입니다. BME280, BH1750, MPU6050, MAX30102, MAX30205를 한 스케치에서 초기화하고 시리얼 모니터로 측정값을 확인합니다.

## 파일 구성

| 파일 | 내용 |
| --- | --- |
| `Integration_Test_Code.ino` | 센서 통합 테스트 메인 스케치입니다. I2C 스캔, 센서 초기화, 측정값 출력을 수행합니다. |
| `compile.ps1` | Arduino CLI로 통합 테스트 스케치를 컴파일합니다. |
| `upload.ps1` | ESP32 보드에 통합 테스트 스케치를 업로드합니다. |
| `monitor.ps1` | 시리얼 모니터를 실행합니다. |
| `README.md` | 본 폴더 설명 문서입니다. |

## 테스트 대상

| 센서 | 주소 | 확인 내용 |
| --- | --- | --- |
| BME280 | `0x76`, `0x77` | 온도, 습도, 기압 |
| BH1750 | `0x23`, `0x5C` | 조도 |
| MPU6050 | `0x68`, `0x69` | 가속도, 자이로 |
| MAX30102 | `0x57` | RED, IR 원시값 |
| MAX30205 | `0x48`, `0x49` | 피부 온도 |

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
.\upload.ps1
.\monitor.ps1
```

직접 Arduino CLI를 사용할 때는 아래 형식을 사용합니다.

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 .
arduino-cli upload --fqbn esp32:esp32:esp32 -p COM포트 .
arduino-cli monitor -p COM포트 -c baudrate=115200
```

`COM포트`는 현재 ESP32가 연결된 포트로 바꿉니다.
