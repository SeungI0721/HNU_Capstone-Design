# Smart Shield HW

이 폴더는 ESP32 펌웨어와 하드웨어 검증용 테스트 코드를 포함합니다.

## 이 폴더의 역할

ESP32 최종 펌웨어와 하드웨어 검증용 Arduino 테스트 코드를 관리합니다. 실제 장치 연결, 센서 주소 확인, 출력 장치 동작 확인은 이 폴더의 코드를 기준으로 진행합니다.

## 폴더 구조

```text
HW/
├─ README.md
├─ SmartShield_ESP32/
└─ TestCode/
```

| 경로 | 설명 |
|---|---|
| `SmartShield_ESP32/` | 최종 통합 ESP32 펌웨어 |
| `TestCode/` | 센서, 출력 모듈, 통합 테스트용 Arduino 스케치 |

현재 최종 빌드 기준은 `HW/SmartShield_ESP32/SmartShield_ESP32.ino`입니다.

## 코드 흐름

```text
TestCode에서 센서와 출력 모듈 단독 확인
→ Integration_Test_Code에서 센서 통합 확인
→ SmartShield_ESP32 최종 펌웨어 빌드
→ BLE 연결, Firebase 업로드, 출력 장치 동작을 앱과 함께 검증
```

## 외부 의존성

| 항목 | 현재 코드 기준 |
|---|---|
| Arduino CLI | ESP32 스케치 컴파일과 업로드에 사용 |
| ESP32 board package | `esp32:esp32:esp32` |
| 주요 Arduino 라이브러리 | `Adafruit_BME280`, `Adafruit_MPU6050`, `BH1750`, `MAX30105`, ESP32 BLE 라이브러리 |
| 실제 장치 | ESP32 Dev Module, I2C 센서, RED LED, 진동 모터, 부저 |

## 최종 하드웨어 기준

| 구분 | 장치 | 통신/제어 | 코드 기준 |
|---|---|---|---|
| 메인 보드 | ESP32 Dev Module | Arduino / BLE | `esp32:esp32:esp32` |
| 환경 센서 | BME280 | I2C | `0x76` 또는 `0x77` |
| 조도 센서 | BH1750 / GY-302 | I2C | `0x23` 또는 `0x5C` |
| 자세 센서 | MPU6050 / GY-521 | I2C | `0x68` 또는 `0x69` |
| 심박/SpO2 보조 | MAX30102 | I2C | `0x57` |
| 피부 온도 | MAX30205 / Fever Click | I2C | `0x48` |
| 시각 경고 | 단일 RED LED | GPIO | GPIO27 |
| 촉각 경고 | 진동 모터 | GPIO | GPIO23 |
| 청각 경고 | 부저 | PWM | GPIO18 |

## 공통 배선

| 기능 | ESP32 핀 |
|---|---|
| I2C SDA | GPIO21 |
| I2C SCL | GPIO22 |
| RED LED | GPIO27 |
| 진동 모터 | GPIO23 |
| 부저 | GPIO18 |

진동 모터는 ESP32 GPIO에 직접 연결하지 말고 MOSFET 또는 트랜지스터와 플라이백 다이오드를 사용해 구동하는 것을 권장합니다.

## 주의 사항

- 최종 펌웨어는 RGB LED가 아니라 단일 RED LED만 사용합니다.
- MAX30205 주소는 현재 코드에서 `0x48`만 확인합니다. 다른 보드나 배선에서는 I2C Scanner로 실제 주소를 먼저 확인합니다.
- MAX30102 HR/SpO2 값은 간이 추정값입니다. 착용 상태, 손가락 접촉, 움직임에 크게 영향을 받으므로 실측 안정성을 별도로 확인해야 합니다.
- 센서가 없거나 읽기 실패한 경우 일부 값은 fallback으로 채워질 수 있습니다. 피부 온도는 `TEMP_VALID:0`, `TEMP_SOURCE:INVALID`로 구분합니다.
- 이 시스템은 산업안전 보조 장치이며 의료 진단 장치가 아닙니다.

## 빌드

저장소 루트에서 최종 펌웨어를 컴파일합니다.

```powershell
arduino-cli compile --fqbn esp32:esp32:esp32 HW\SmartShield_ESP32
```

업로드는 실제 ESP32 보드가 연결되어 있고 COM 포트가 인식된 뒤 실행합니다.

```powershell
arduino-cli upload -p COM8 --fqbn esp32:esp32:esp32 HW\SmartShield_ESP32
```

## 실측 검증

하드웨어 실측 결과는 [MEASUREMENT_VERIFICATION.md](../docs/MEASUREMENT_VERIFICATION.md) 기준으로 기록하고, 결과 요약은 [DEMO_VERIFICATION_RESULT.md](../docs/DEMO_VERIFICATION_RESULT.md)에 남깁니다. 빌드 성공은 실제 센서값 안정성, BLE 연결, 출력 장치 동작을 보장하지 않습니다.

## 관련 문서

| 문서 | 설명 |
|---|---|
| [최상단 README](../README.md) | 전체 시스템 구조 |
| [SmartShield_ESP32 README](SmartShield_ESP32/README.md) | 최종 펌웨어 세부 설명 |
| [TestCode README](TestCode/README.md) | 하드웨어 테스트 코드 흐름 |
| [BLE_PROTOCOL.md](../docs/BLE_PROTOCOL.md) | ESP32와 작업자 앱 BLE 규격 |
| [DEMO_VERIFICATION_RESULT.md](../docs/DEMO_VERIFICATION_RESULT.md) | 빌드·시연·부분 검증·미검증 상태 |
