# Smart Shield TestCode

Smart Shield 하드웨어를 단계별로 검증하기 위한 Arduino 테스트 코드 폴더입니다. 센서 단독 테스트, 출력 모듈 단독 테스트, 센서 통합 테스트, 경고 출력 통합 테스트가 포함되어 있습니다.

## 폴더 구조

```text
TestCode/
├─ README.md
├─ Sensor/
├─ Module/
├─ Integration_Test_Code/
└─ Warning_Integration_Test_Code/
```

## 하위 폴더 요약

| 폴더 | 내용 |
|---|---|
| `Sensor/` | BME280, BH1750, MPU6050, MAX30102, MAX30205, I2C Scanner 테스트 |
| `Module/` | RGB LED, 진동 모터, PWM 진동 모터, 패시브 부저 테스트 |
| `Integration_Test_Code/` | 여러 센서를 한 번에 초기화하고 시리얼 출력으로 확인하는 통합 테스트 |
| `Warning_Integration_Test_Code/` | 위험 단계별 LED, 진동, 부저 출력 패턴 확인 |

## 권장 테스트 순서

1. `Sensor/I2C_Seaner`로 I2C 주소 확인
2. `Sensor/` 폴더의 센서 단독 테스트 실행
3. `Module/` 폴더의 출력 모듈 단독 테스트 실행
4. `Integration_Test_Code`로 센서 통합 테스트 실행
5. `Warning_Integration_Test_Code`로 경고 출력 통합 테스트 실행
6. `SmartShield_ESP32` 최종 펌웨어 업로드

## 공통 기준

| 항목 | 값 |
|---|---|
| 보드 | ESP32 Dev Module |
| FQBN | `esp32:esp32:esp32` |
| Serial baud | `115200` |
| I2C SDA | GPIO21 |
| I2C SCL | GPIO22 |

## 실행 방법

각 테스트 스케치 폴더에서 Arduino CLI를 실행합니다. 보드 포트는 현재 PC에서 인식된 COM 포트로 바꿉니다.

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 .
arduino-cli upload --fqbn esp32:esp32:esp32 -p COM8 .
arduino-cli monitor -p COM8 -c baudrate=115200
```

## 외부 의존성

| 항목 | 설명 |
|---|---|
| Arduino CLI | 테스트 스케치 빌드와 업로드 |
| ESP32 board package | `esp32:esp32:esp32` |
| 센서 라이브러리 | 각 센서 테스트 스케치의 `#include` 기준 |
| 실제 하드웨어 | 테스트 대상 센서 또는 출력 모듈 |

## 테스트 방법

시리얼 모니터에서 센서 주소, 측정값, 출력 장치 동작 로그를 확인합니다. 측정값이 나오더라도 실제 장착 상태와 최종 펌웨어 동작은 별도로 확인해야 합니다.

## 주의사항

- `I2C_Seaner`는 폴더명이 코드 기준으로 유지되어 있습니다.
- 테스트 코드는 최종 앱 연동을 보장하지 않습니다.
- 출력 모듈 테스트 전에는 모터와 부저 구동 회로를 먼저 확인합니다.

## 관련 문서

| 문서 | 설명 |
|---|---|
| [상위 HW README](../README.md) | 하드웨어 전체 기준 |
| [Sensor README](Sensor/README.md) | 센서 단독 테스트 |
| [Module README](Module/README.md) | 출력 모듈 단독 테스트 |
| [Integration Test README](Integration_Test_Code/README.md) | 센서 통합 테스트 |
